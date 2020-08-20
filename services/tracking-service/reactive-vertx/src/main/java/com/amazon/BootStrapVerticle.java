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

package com.amazon;

import com.amazon.verticles.CacheVerticle;
import com.amazon.verticles.HttpVerticle;
import com.amazon.verticles.KinesisVerticle;
import com.amazon.verticles.RedisVerticle;
import io.vertx.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.System.getenv;

public class BootStrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(BootStrapVerticle.class.getName());

    static {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
    }

    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new BootStrapVerticle());
    }

    @Override
    public void start(Promise<Void> startFuture) {

        String trustStoreLocation = getenv("javax.net.ssl.trustStore");
        String trustAnchorsLocation = getenv("javax.net.ssl.trustAnchors");

        if (null != trustStoreLocation) {
            LOGGER.info("Setting javax.net.ssl.trustStore to " + trustStoreLocation);
            System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
        } else {
            LOGGER.info("Setting javax.net.ssl.trustStore not set");
        }

        if (null != trustAnchorsLocation) {
            LOGGER.info("Setting javax.net.ssl.trustAnchors to " + trustAnchorsLocation);
            System.setProperty("javax.net.ssl.trustAnchors", trustAnchorsLocation);
        } else {
            LOGGER.info("Setting javax.net.ssl.trustAnchors not set");
        }

        final List<Future> futures = new ArrayList<>(4);

        LOGGER.info("Deploying " + RedisVerticle.class.getCanonicalName());
        futures.add(vertx.deployVerticle(RedisVerticle.class, new DeploymentOptions().setInstances(1)));

        LOGGER.info("Deploying " + CacheVerticle.class.getCanonicalName());
        futures.add(vertx.deployVerticle(CacheVerticle.class, new DeploymentOptions().setInstances(1)));

        LOGGER.info("Deploying " + HttpVerticle.class.getCanonicalName());
        futures.add(vertx.deployVerticle(HttpVerticle.class, new DeploymentOptions().setInstances(5)));

        LOGGER.info("Deploying " + KinesisVerticle.class.getCanonicalName());
        futures.add(vertx.deployVerticle(KinesisVerticle.class, new DeploymentOptions().setInstances(5)));

        CompositeFuture.all(futures).onComplete(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                LOGGER.severe(ar.cause().getMessage());
                startFuture.fail(ar.cause());
            }
        });
    }
}
