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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.getenv;

public class BootStrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootStrapVerticle.class);

    static {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
        String trustStoreLocation = getenv("javax.net.ssl.trustStore");

        if (null != trustStoreLocation) {
            LOGGER.info("Setting javax.net.ssl.trustStore to " + trustStoreLocation);
            System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
        }
    }

    public static void main (String ... args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new BootStrapVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) {

        List<Future> futures = Stream.generate(Future::<String>future).limit(4)
                .collect(Collectors.toList());

        LOGGER.info("Deploying " + RedisVerticle.class.getCanonicalName());
        this.deployVerticle(RedisVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(1), futures.get(0));

        LOGGER.info("Deploying " + CacheVerticle.class.getCanonicalName());
        this.deployVerticle(CacheVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(1), futures.get(1));

        LOGGER.info("Deploying " + HttpVerticle.class.getCanonicalName());
        this.deployVerticle(HttpVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(5), futures.get(2));

        LOGGER.info("Deploying " + KinesisVerticle.class.getCanonicalName());
        this.deployVerticle(KinesisVerticle.class.getCanonicalName(), new DeploymentOptions().setInstances(5), futures.get(3));

        CompositeFuture.all(futures).setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });
    }

    private void deployVerticle(final String verticleName, final DeploymentOptions deploymentOptions,
                                Future<String> future) {
        vertx.deployVerticle(verticleName, deploymentOptions, deployment ->
        {
            if (!deployment.succeeded()) {
                LOGGER.error(deployment.cause());
                future.fail(deployment.cause());
            } else {
                future.complete();
            }
        });
    }
}
