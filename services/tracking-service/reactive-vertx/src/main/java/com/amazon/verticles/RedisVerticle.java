/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package com.amazon.verticles;

import com.amazon.util.Constants;
import com.amazon.vo.TrackingMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.logging.Logger;

import static com.amazon.util.Constants.*;

public class RedisVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(RedisVerticle.class.getName());
    private RedisClient redisClient, redisPubSubClient;

    void registerToEventBusForAdding(final EventBus eb, final RedisClient redis) {
        eb.consumer(Constants.REDIS_STORE_EVENTBUS_ADDRESS, message -> {
            TrackingMessage trackingMessage = Json.decodeValue(((JsonObject)message.body()).encode(), TrackingMessage.class);
            redis.hmset(trackingMessage.getProgramId(), JsonObject.mapFrom(trackingMessage), res -> {
                if (!res.succeeded()) {
                    LOGGER.info(res.cause());
                }
            });
        });
    }

    void registerToEventBusForPurging(final EventBus eb, final RedisClient redis) {
        eb.consumer(Constants.REDIS_PURGE_EVENTBUS_ADDRESS, message -> redis.flushall(res -> {
            if (!res.succeeded()) {
                LOGGER.info(res.cause());
            }
        }));
    }

    void registerToEventBusForCacheVerticle(final EventBus eb, final RedisClient redis) {
        eb.consumer(Constants.REDIS_EVENTBUS_ADDRESS, message -> {
            // Getting data from Redis and storing it in cache verticle

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

        });
    }

    void registerToEventBusForPubSub(final EventBus eb, final RedisClient redis) {

        // register a handler for the incoming message the naming the Redis module will use is base address + '.' + redis channel
        vertx.eventBus().<JsonObject>consumer(REDIS_PUBSUB_CHANNEL_VERTX, received -> {
            // do whatever you need to do with your message
            JsonObject value = received.body().getJsonObject("value");
            LOGGER.info("Received the following message: " + value);
            // the value is a JSON doc with the following properties
            // channel - The channel to which this message was sent
            // pattern - Pattern is present if you use psubscribe command and is the pattern that matched this message channel
            // message - The message payload

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
    }

    @Override
    public void start() {

        String envRedisHost = System.getenv(REDIS_HOST);
        String envRedisPort = System.getenv(REDIS_PORT);

        String redisHost = envRedisHost == null ? "localhost" : envRedisHost;
        int redisPort = envRedisPort == null ? 6379 : Integer.parseInt(envRedisPort);

        LOGGER.info("--> Using Redis Host " + redisHost);
        LOGGER.info("--> Using Redis Port " + redisPort);

        RedisOptions config = new RedisOptions()
                .setHost(redisHost)
                .setPort(redisPort);

        redisClient = RedisClient.create(vertx, config);
        redisPubSubClient = RedisClient.create(vertx, config);

        EventBus eb = vertx.eventBus();
        this.registerToEventBusForAdding(eb, redisClient);
        this.registerToEventBusForCacheVerticle(eb, redisClient);
        this.registerToEventBusForPubSub(eb, redisPubSubClient);
        this.registerToEventBusForPurging(eb, redisClient);
    }

    @Override
    public void stop() throws Exception {
        redisClient.close(event -> {
            if (event.succeeded()) {
                LOGGER.info("--> Redis connection has been closed.");
            } else {
                LOGGER.severe("--> Redis connection could not be closed!");
            }
        });
    }
}
