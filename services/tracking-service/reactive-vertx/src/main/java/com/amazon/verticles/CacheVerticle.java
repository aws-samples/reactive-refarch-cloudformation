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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CacheVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(CacheVerticle.class.getName());

    private static final int MAX_CACHE_ENTRIES = 100000;
    private static final int ENTRY_EXPIRE_TIME = 10;

    private static final Cache<String, TrackingMessage> CACHE = CacheBuilder.newBuilder().
            maximumSize(MAX_CACHE_ENTRIES).
            expireAfterWrite(ENTRY_EXPIRE_TIME, TimeUnit.MINUTES).
            build();

    @Override
    public void start() {

        EventBus eb = vertx.eventBus();

        this.registerToEventBusToGetData(eb);
        this.registerToEventBusForUpdates(eb);
        this.registerToEventBusToFill(eb);

        eb.consumer(Constants.CACHE_PURGE_EVENTBUS_ADDRESS, message -> CACHE.cleanUp());
    }

    private void writeDataToCache(final Message<JsonObject> message) {
        TrackingMessage trackingMessage = Json.decodeValue(message.body().encode(), TrackingMessage.class);
        CACHE.put(trackingMessage.getProgramId(), trackingMessage);
        LOGGER.fine("Stored the following key/value-pair in cache: " + trackingMessage.getProgramId() + " -> " + message.body());
    }

    private void registerToEventBusToFill(final EventBus eb) {
        // Handler for test data
        eb.consumer(Constants.CACHE_STORE_EVENTBUS_ADDRESS, this::writeDataToCache);
    }

    private void registerToEventBusForUpdates(final EventBus eb) {
        // Writing the data into the cache
        // Called from Redis verticle (Redis pub/sub-update)
        eb
                .<JsonObject>consumer(Constants.CACHE_REDIS_EVENTBUS_ADDRESS)
                .handler(message -> {
                    LOGGER.fine("I have received a message: " + message.body());
                    LOGGER.fine("Message type: " + message.body().getClass().getName());
                    writeDataToCache(message);
                });
    }

    private void registerToEventBusToGetData(final EventBus eb) {
        eb
                .<JsonObject>consumer(Constants.CACHE_EVENTBUS_ADDRESS)
                .handler(message -> {
                    // Is data stored in cache?

                    TrackingMessage trackingMessage = Json.decodeValue(message.body().encode(), TrackingMessage.class);
                    LOGGER.fine("Wrote message to cache: " + message.body());
                    TrackingMessage value = CACHE.getIfPresent(trackingMessage.getProgramId());

                    if (null == value) {
                        JsonObject msgToSend = JsonObject.mapFrom(trackingMessage);
                        LOGGER.info("Key " + trackingMessage.getProgramId() + " not found in cache --> Redis");
                        eb
                                .<JsonObject>request(Constants.REDIS_EVENTBUS_ADDRESS, msgToSend)
                                .onSuccess(res -> {
                                    JsonObject msg = res.body();

                                    if (msg.isEmpty()) {
                                        message.reply(msg);
                                    } else {
                                        LOGGER.fine("Message from Redis-Verticle: " + msg);
                                        TrackingMessage msgFromRedis = Json.decodeValue(msg.encode(), TrackingMessage.class);
                                        CACHE.put(msgFromRedis.getProgramId(), msgFromRedis);

                                        message.reply(msg);
                                    }
                                })
                                .onFailure(err -> message.reply(new JsonObject()));

                    } else {
                        LOGGER.fine("Message " + Json.encode(value) + " found in cache --> HttpVerticle");
                        value.setMessageId(trackingMessage.getMessageId());
                        message.reply(JsonObject.mapFrom(value));
                    }
                });
    }
}
