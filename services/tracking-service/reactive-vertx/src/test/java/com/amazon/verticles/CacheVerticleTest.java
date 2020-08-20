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
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Logger;

@RunWith(VertxUnitRunner.class)
public class CacheVerticleTest {

    private static final Logger LOGGER = Logger.getLogger(CacheVerticleTest.class.getName());

    Vertx vertx;
    EventBus eb;
    String deploymentId;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
        eb = vertx.eventBus();

        vertx.deployVerticle(CacheVerticle.class.getCanonicalName(), context.asyncAssertSuccess(deploymentID -> this.deploymentId = deploymentID));
    }

     private TrackingMessage prepareData() {
        TrackingMessage msg = new TrackingMessage();
        msg.setMessageId("messageId");
        msg.setUserAgent("myUserAgent");
        msg.setProgramId("12345");
        msg.setCustomerName("myCustomer");
        msg.setValid(true);

        return msg;
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void writeFromRedisTest() {
        LOGGER.info(" ---> Testcase: writeFromRedisTest");

        JsonObject message = JsonObject.mapFrom(prepareData());
        eb.request(Constants.CACHE_REDIS_EVENTBUS_ADDRESS, message, res -> {
            if (res.succeeded()) {
                Object body = res.result().body();
                LOGGER.info("Received result " + body);
                Assert.assertNotNull(body);
            } else {
                LOGGER.info(res.cause().getMessage());
                Assert.fail();
            }
        });
    }

    @Test
    public void readFromCacheTest() {
        LOGGER.info(" ---> Testcase: readFromCacheTest");

        TrackingMessage testMessage = prepareData();
        JsonObject message = JsonObject.mapFrom(testMessage);
        eb.request(Constants.CACHE_EVENTBUS_ADDRESS, message, res -> {
            if (res.succeeded()) {
                JsonObject body = (JsonObject)res.result().body();
                LOGGER.info("Received result " + body + " -> " + body.getClass().getName());
                Assert.assertNotNull(body);
                LOGGER.info(body.getClass().getName());
                TrackingMessage resultMessage = Json.decodeValue(body.encode(), TrackingMessage.class);

                Assert.assertEquals(testMessage.getProgramId(), resultMessage.getProgramId());

            } else {
                LOGGER.info(res.cause().getMessage());
                Assert.fail();
            }
        });

    }


}
