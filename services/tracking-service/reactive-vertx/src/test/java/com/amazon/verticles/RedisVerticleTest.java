/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Map;

import static com.amazon.util.Constants.REDIS_HOST;
import static com.amazon.util.Constants.REDIS_PORT;

@RunWith(VertxUnitRunner.class)
public class RedisVerticleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisVerticleTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Jedis jedis;

    static Vertx vertx;
    static EventBus eb;

    @BeforeClass
    public static void before(TestContext context) throws Exception {

        String redisHost = System.getenv(REDIS_HOST) == null ? "localhost" : System.getenv(REDIS_HOST);
        int redisPort = System.getenv(REDIS_PORT) == null ? 6379 : Integer.getInteger(System.getenv(REDIS_PORT));

        jedis = new Jedis(redisHost, redisPort);
        jedis.connect();

        LOGGER.info("Using Redis Host " + redisHost);
        LOGGER.info("Using Redis Port " + redisPort);

        TrackingMessage trackingMessage = prepareData();

        writeDatatoRedis(trackingMessage, redisPort, redisHost);

        vertx = Vertx.vertx();
        eb = vertx.eventBus();

        // For this test, we need RedisVerticle and CacheVerticle
        vertx.deployVerticle(RedisVerticle.class.getCanonicalName(), context.asyncAssertSuccess(deploymentID -> {

        }));

        vertx.deployVerticle(CacheVerticle.class.getCanonicalName(), context.asyncAssertSuccess(deploymentID -> {

        }));
    }

    @AfterClass
    public static void after(TestContext context) throws Exception {
        // Delete data from Redis

        Thread.sleep(1000);
        deleteDataFromRedis(prepareData());

        vertx.close(context.asyncAssertSuccess());

        if (null != jedis)
            jedis.close();
    }

    private static TrackingMessage prepareData() {
        TrackingMessage msg = new TrackingMessage();
        msg.setMessageId("messageId");
        msg.setUserAgent("myUserAgent");
        msg.setProgramId("12345");
        msg.setProgramName("myProgram");
        msg.setCustomerName("myCustomer");
        msg.setCustomerId(1234);
        msg.setChecksum("check123");
        msg.setValid(true);

        return msg;
    }

    private static void deleteDataFromRedis(final TrackingMessage trackingMessage) {
        jedis.del(trackingMessage.getProgramId());
    }

    private static void writeDatatoRedis(final TrackingMessage trackingMessage, final int redisPort, final String redisHost) {
        // First we write data to Redis

        Map<String, String> map = marshal(Json.encode(trackingMessage));
        LOGGER.info("Writing the following data to Redis: " + map);

        jedis.hmset(trackingMessage.getProgramId(), map);
    }

    public static Map<String, String> marshal(final String jsonString) {

        try {
            Map<String, String> tmpMap = MAPPER.readValue(jsonString, new TypeReference<Map<String, String>>() {
            });
            return tmpMap;
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Test
    public void pubSubTest() throws Exception{
        // Now we send a notification using Pub/Sub
        // But first we wait a second ...

        Thread.sleep(1000);
        Long result = jedis.publish(Constants.REDIS_PUBSUB_CHANNEL, Json.encode(prepareData()));

        LOGGER.info("Result: " + result);

        // CacheVerticle should be called -> data should be in the cache

        TrackingMessage testMessage = prepareData();
        JsonObject message = JsonObject.mapFrom(testMessage);
        try {
            eb.send(Constants.CACHE_EVENTBUS_ADDRESS, message, res -> {
                if (res.succeeded()) {
                    JsonObject body = (JsonObject)res.result().body();
                    LOGGER.info("Received result " + body + " -> " + body.getClass().getName());
                    Assert.assertNotNull(body);
                    TrackingMessage resultMessage = Json.decodeValue(body.encode(), TrackingMessage.class);

                    Assert.assertEquals(testMessage.getProgramId(), resultMessage.getProgramId());

                } else {
                    LOGGER.info(res.cause());
                    Assert.fail();
                }
            });
        }
        catch (Exception exc) {
            exc.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void readFromRedisTest() {

        TrackingMessage testMessage = prepareData();
        JsonObject message = JsonObject.mapFrom(testMessage);

        try {

            eb.send(Constants.REDIS_EVENTBUS_ADDRESS, message, res -> {
                if (res.succeeded()) {
                    Object body = res.result().body();
                    LOGGER.info("Received result " + body + " -> " + body.getClass().getName());
                    Assert.assertNotNull(body);
                    JsonObject msg = (JsonObject) res.result().body();
                    TrackingMessage resultMessage = Json.decodeValue(msg.encode(), TrackingMessage.class);

                    Assert.assertEquals(testMessage.getProgramId(), resultMessage.getProgramId());

                } else {
                    LOGGER.info(res.cause());
                    Assert.fail();
                }
            });
        }
        catch (Exception exc) {
            exc.printStackTrace();
            Assert.fail();
        }
    }
}
