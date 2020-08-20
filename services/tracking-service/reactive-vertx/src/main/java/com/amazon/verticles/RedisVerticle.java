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
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Response;

import java.util.logging.Logger;

import static com.amazon.util.Constants.*;
import static io.vertx.redis.client.Command.*;
import static io.vertx.redis.client.Request.cmd;

public class RedisVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(RedisVerticle.class.getName());
    private Redis redis;
    private Redis pubsub;

    void registerToEventBusForAdding(final EventBus eb) {
        eb.consumer(Constants.REDIS_STORE_EVENTBUS_ADDRESS, message -> {
            TrackingMessage trackingMessage = Json.decodeValue(((JsonObject) message.body()).encode(), TrackingMessage.class);
            redis.send(cmd(HMSET).arg(trackingMessage.getProgramId()).arg(JsonObject.mapFrom(trackingMessage)), res -> {
                if (!res.succeeded()) {
                    LOGGER.info(res.cause().getMessage());
                }
            });
        });
    }

    void registerToEventBusForPurging(final EventBus eb) {
        eb.consumer(Constants.REDIS_PURGE_EVENTBUS_ADDRESS, message -> redis.send(cmd(FLUSHALL), res -> {
            if (!res.succeeded()) {
                LOGGER.info(res.cause().getMessage());
            }
        }));
    }

    void registerToEventBusForCacheVerticle(final EventBus eb) {
        eb.consumer(Constants.REDIS_EVENTBUS_ADDRESS, message -> {
            // Getting data from Redis and storing it in cache verticle

            TrackingMessage trackingMessage = Json.decodeValue(((JsonObject) message.body()).encode(), TrackingMessage.class);
            LOGGER.info(RedisVerticle.class.getSimpleName() + ": I have received a message: " + message.body());

            LOGGER.info("Looking for programId " + trackingMessage.getProgramId() + " in Redis");

            redis.send(cmd(HGETALL).arg(trackingMessage.getProgramId()), res -> {
                if (res.succeeded()) {
                    Response result = res.result();
                    if (null == result || result.size() == 0) {
                        LOGGER.info("No object found");
                        message.reply(new JsonObject());
                    } else {
                        TrackingMessage msg = new TrackingMessage();
                        msg.setUserAgent(result.get("userAgent").toString());
                        msg.setProgramId(result.get("programId").toString());
                        msg.setProgramName(result.get("programName").toString());
                        msg.setChecksum(result.get("checksum").toString());
                        msg.setCustomerId(result.get("customerId").toInteger());
                        msg.setCustomerName(result.get("customerName").toString());
                        msg.setMessageId(trackingMessage.getMessageId());
                        msg.setValid(result.get("valid").toBoolean());

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

    void registerToEventBusForPubSub(final EventBus eb) {

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

        redis.send(cmd(SUBSCRIBE).arg(Constants.REDIS_PUBSUB_CHANNEL), res -> {
            if (res.succeeded()) {
                LOGGER.info("Subscribed to " + Constants.REDIS_PUBSUB_CHANNEL);
            } else {
                LOGGER.info(res.cause().getMessage());
            }
        });
    }

    @Override
    public void start() {

        String envRedisHost = System.getenv(REDIS_HOST);
        String envRedisPort = System.getenv(REDIS_PORT);

        String redisHost = envRedisHost == null ? "localhost" : envRedisHost;
        int redisPort = envRedisPort == null ? 6379 : Integer.parseInt(envRedisPort);

        String redisURI = String.format("redis://%s:%d", redisHost, redisPort);

        LOGGER.info("--> Using Redis Connection URI " + redisURI);

        redis = Redis.createClient(vertx, redisURI);
        pubsub = Redis.createClient(vertx, redisURI);

        EventBus eb = vertx.eventBus();
        this.registerToEventBusForAdding(eb);
        this.registerToEventBusForCacheVerticle(eb);
        this.registerToEventBusForPubSub(eb);
        this.registerToEventBusForPurging(eb);
    }

    @Override
    public void stop() {
        if (redis != null) {
            redis.close();
        }
        if (pubsub != null) {
            pubsub.close();
        }
    }
}
