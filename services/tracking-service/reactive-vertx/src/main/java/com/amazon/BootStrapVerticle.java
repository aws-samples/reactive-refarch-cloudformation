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

import com.amazon.verticles.CacheVerticle;
import com.amazon.verticles.HttpVerticle;
import com.amazon.verticles.KinesisVerticle;
import com.amazon.verticles.RedisVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class BootStrapVerticle extends AbstractVerticle {

    static {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BootStrapVerticle.class);

    public static void main (String ... args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new BootStrapVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        LOGGER.info("Deploying " + RedisVerticle.class.getCanonicalName());
        vertx.deployVerticle(RedisVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(1));

        LOGGER.info("Deploying " + CacheVerticle.class.getCanonicalName());
        vertx.deployVerticle(CacheVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(1));

        LOGGER.info("Deploying " + HttpVerticle.class.getCanonicalName());
        vertx.deployVerticle(HttpVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(5));

        LOGGER.info("Deploying " + KinesisVerticle.class.getCanonicalName());
        vertx.deployVerticle(KinesisVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(5));
    }
}
