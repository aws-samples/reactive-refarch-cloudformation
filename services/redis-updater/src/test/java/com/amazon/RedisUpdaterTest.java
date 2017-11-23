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

package com.amazon;

import com.amazon.vo.TrackingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;

import static com.amazon.util.Constants.REDIS_HOST;
import static com.amazon.util.Constants.REDIS_PORT;

public class RedisUpdaterTest {

    private static final Logger LOGGER = LogManager.getLogger(RedisUpdaterTest.class);
    private static Jedis jedis, subscriberJedis;

    @BeforeClass
    public static void before() throws Exception {
        String redisHost = System.getenv(REDIS_HOST) == null ? "localhost" : System.getenv(REDIS_HOST);
        int redisPort = System.getenv(REDIS_PORT) == null ? 6379 : Integer.getInteger(System.getenv(REDIS_PORT));

        jedis = new Jedis(redisHost, redisPort);
        jedis.connect();

        subscriberJedis = new Jedis(redisHost, redisPort);

        new Thread(() -> {
            try {
                subscriberJedis.subscribe(setupSubscriber(), "com.amazon.reactive");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();


        LOGGER.info("Using Redis Host " + redisHost);
        LOGGER.info("Using Redis Port " + redisPort);
    }

    private static JedisPubSub setupSubscriber() {
        final JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                System.out.println("onUnsubscribe");
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                System.out.println("onSubscribe");
            }

            @Override
            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            @Override
            public void onPSubscribe(String pattern, int subscribedChannels) {
            }

            @Override
            public void onPMessage(String pattern, String channel, String message) {
            }

            @Override
            public void onMessage(String channel, String message) {
                System.out.println("Message received");
                System.out.println("Message: " + message);
            }
        };

        return jedisPubSub;
    }

    @AfterClass
    public static void after() throws Exception {
        // Delete data from Redis

        Thread.sleep(1000);
        deleteDataFromRedis(prepareData());

        if (null != jedis)
            jedis.close();
    }

    private static TrackingMessage prepareData() {
        TrackingMessage msg = new TrackingMessage();
        msg.setProgramId("12345");
        msg.setProgramName("program1");
        msg.setCustomerName("myCustomer");
        msg.setCustomerId(1234);
        msg.setChecksum("check123");
        msg.setValid(true);

        return msg;
    }

    private static void deleteDataFromRedis(final TrackingMessage trackingMessage) {
        jedis.del(trackingMessage.getProgramId());
    }

    @Test
    public void writeToRedisTest() {
        try {
            TrackingMessage testMessage = prepareData();

            RedisUpdater redisUpdater = new RedisUpdater();
            redisUpdater.updateRedisData(testMessage, jedis);
            redisUpdater.notifySubscribers(testMessage, jedis);

            Map<String, String> resultMap = jedis.hgetAll(testMessage.getProgramId());

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(resultMap);
            TrackingMessage resultMessage = mapper.readValue(json, TrackingMessage.class);
            Assert.assertNotNull(resultMessage);
            Assert.assertEquals(resultMessage, testMessage);
        }

        catch (Exception exc) {
            exc.printStackTrace();
        }
    }


}
