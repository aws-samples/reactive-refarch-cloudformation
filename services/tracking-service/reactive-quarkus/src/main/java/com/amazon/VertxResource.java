/*
 * Copyright 2010-2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.vertx.core.Vertx;
import org.jboss.logging.Logger;

@QuarkusMain
public class VertxResource implements QuarkusApplication {

    public static void main(String... args) {
        Quarkus.run(VertxResource.class, args);
    }

    private static final Logger LOGGER = Logger.getLogger(VertxResource.class);

    static final String APP_VERSION = "2.905";

    @Override
    public int run(String... args) throws Exception {

        LOGGER.info("Starting application version: " + APP_VERSION);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("com.amazon.verticles.HttpVerticle").onFailure(
                t -> LOGGER.info("Deployment failed")
        );

        vertx.deployVerticle("com.amazon.verticles.CacheVerticle").onFailure(
                t -> LOGGER.info("Deployment failed")
        );

        vertx.deployVerticle("com.amazon.verticles.KinesisVerticle").onFailure(
                t -> LOGGER.info("Deployment failed")
        );

        vertx.deployVerticle("com.amazon.verticles.RedisVerticle").onFailure(
                t -> LOGGER.info("Deployment failed")
        );

        Quarkus.waitForExit();
        return 0;
    }
}