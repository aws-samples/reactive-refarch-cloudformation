# Reactive Vertx

## General concept
This application is the main microservice for the reactive architecture. It is based on the framework [Vert.x](http://vertx.io/) which is a toolkit for building reactive applications on the JVM. Vert.x is an event-driven, reactive, nonblocking, and polyglot framework to implement microservices. It runs on the Java virtual machine (JVM) by using the low-level IO library [Netty](https://netty.io/). You can write applications in Java, JavaScript, Groovy, Ruby, and Ceylon. The framework offers a simple and scalable actor-like concurrency model: Vert.x calls handlers by using a thread known as an event loop. To use this model, you have to write code known as verticles. Those verticles share certain similarities with actors in the Actor Model, and to use them, you have to implement the `Verticle` interface. 
This microservices implements a simplified version of a typical tracking frontend application that can be used e.g. for Ad-tracking purposes. Reactive frameworks like [Vert.x](http://vertx.io/), [Akka](https://akka.io/), and [Reactor](https://projectreactor.io/) are a great match for highly concurrent, realtime workloads like Ad-tracking that usually has a realtime part (input validation and redirects) and a non-realtime part (matching).

## Verticles
The application consists of the following set of verticles:

- `HttpVerticle`: The main verticle that exposes a HTTP-endpoint. To reduce the data transfer, all responses are compressed using gzip.
- `CacheVerticle`: This verticle implements the caching strategy for the application. To simplify the whole architecture, Redis is used as the main datastore which keeps all core data. For production usage, this is not a recommended pattern, all core data should be stored in a database like Amazon DynamoDB or Amazon Aurora and Redis should act as a caching layer. The application has L1 cache based on [Google Guava](https://github.com/google/guava) that stores data with a TTL of 10 minutes. If the data is not found in the L1 cache, Redis is queried, and if the data is found, it is used to respond to the HTTP request and put into the L1 cache. If the core data in Redis is updated (using the [redis-updater]()-Lambda function), Redis is also used as a pub/sub queue to publish the changes to the Vert.x-application in order to reduce communication with Redis.
- `RedisVerticle`:  This verticle handles all data in Redis and subscribes to a pub/sub-channel in order to receive core data-updates and read data from Redis if the required entry can't be found in the L1 cache.
- `KinesisVerticle`: This verticle handles asynchronous writes to an Amazon Kinesis Stream. It consumes JSON-data from the Event Bus and wraps the data in Protobuf to reduce message size.
- `BootStrapVerticle`: The base verticle to deploy other verticles and bootstrap the application.

## Communication
Communication between parts of the application is implemented using three different approaches. The verticles communicate with each other over [Vert.x events bus](http://vertx.io/docs/vertx-core/java/#event_bus), the event bus is a "light-weight distributed messaging system which allows different parts of your application, or different applications and services to communicate with each in a loosely coupled way". In the case JSON messages are used to share data between verticles. To update core data in the L1 cache, Redis [pub/sub](https://redis.io/topics/pubsub) is used. The publish/subscribe messaging paradigm is necessary because the number of Vert.x application can change due to auto scaling. Processed and vaidated input data will be transformed into a protobuf schema and send asynchronously to an Amazon Kinesis Stream. 

## Caching infrastructure
Caching is an important concept to reduce chattiness and latency. In this case, caching is implemented using a L1 cache based on Google Guava with a pre-defined size-limit of cache-entries and a TTL of 10 minutes. If a cache-miss occurs in the L1 cache, the application will access the L2 cache which is a Redis 3 cluster with one partition running in multi-az mode for high availability using Amazon ElastiCache. Thankfully, Vert.x 3 has built-in support for Redis which makes asynchronous calls much easier. The following code-snippet shows how to access Redis asynchronously using the [HGETALL-command](https://redis.io/commands/hgetall):

```
TrackingMessage trackingMessage = Json.decodeValue(((JsonObject)message.body()).encode(), TrackingMessage.class);
            LOGGER.info(RedisVerticle.class.getSimpleName() + ": I have received a message: " + message.body());

            LOGGER.info("Looking for programId " + trackingMessage.getProgramId() + " in Redis");

            redis.hgetall(trackingMessage.getProgramId(), res -> {
                if (res.succeeded()) {
                    JsonObject result = res.result();
                    if (null == result || result.isEmpty()) {
                        LOGGER.info("No object found");
                        message.reply(new JsonObject());
                    } else {
                        String strRes = Json.encode(result);
                        TrackingMessage msg = Json.decodeValue(strRes, TrackingMessage.class);
                        msg.setMessageId(trackingMessage.getMessageId());

                        JsonObject msgResult = JsonObject.mapFrom(msg);

                        LOGGER.info("Result: " + msgResult);
                        message.reply(msgResult);
                    }
                } else {
                    LOGGER.info("No object found: " + res.cause());
                    message.reply(new JsonObject());
                }
            });
```

Redis is also used to update the cache entries in the L1 cache using pub/sub. The following code-snippet shows how Vert.x can be used to subscribe to a Redis channel:

```
        vertx.eventBus().<JsonObject>consumer(REDIS_PUBSUB_CHANNEL_VERTX, received -> {
            JsonObject value = received.body().getJsonObject("value");
           
            String message = value.getString("message");

            JsonObject jsonObject = new JsonObject(message);
            eb.send(CACHE_REDIS_EVENTBUS_ADDRESS, jsonObject);
        });

        redis.subscribe(Constants.REDIS_PUBSUB_CHANNEL, res -> {
            if (res.succeeded()) {
                LOGGER.info("Subscribed to " + Constants.REDIS_PUBSUB_CHANNEL);
            } else {
                LOGGER.info(res.cause());
            }
        });
```