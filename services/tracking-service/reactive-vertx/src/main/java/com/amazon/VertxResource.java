/*
 * Copyright 2010-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.mutiny.core.Vertx;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;

@ApplicationScoped
public class VertxResource {

    private static final Logger LOGGER = Logger.getLogger(VertxResource.class);

    private Vertx vertx;

    public void init(@Observes StartupEvent e, Vertx vertx, Instance<AbstractVerticle> verticles) {
        this.vertx = vertx;
        for (AbstractVerticle verticle : verticles) {
            LOGGER.info("Deploy verticle: " + verticle.getClass().getName());
            vertx.deployVerticle(verticle).await().indefinitely();
        }
    }
}