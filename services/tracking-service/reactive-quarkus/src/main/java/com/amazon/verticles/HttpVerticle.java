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

package com.amazon.verticles;

import com.amazon.util.Constants;
import com.amazon.vo.TrackingMessage;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class HttpVerticle extends AbstractVerticle {

    private EventBus eb;
    private HttpServer httpServer;
    private static final Logger LOGGER = Logger.getLogger(HttpVerticle.class.getName());

    @Override
    public void start() {
        LOGGER.info("Starting " + this.getClass().getName());
        this.eb = vertx.eventBus().getDelegate();
        if (eb == null) {
            LOGGER.info("EventBus is null");
        }
        this.initHttpServer();
    }

    @Override
    public void stop() {
        if (httpServer != null) {
            httpServer.close();
        }
    }

    private void initHttpServer() {
        Router router = Router.router(vertx.getDelegate());

        router.route().handler(BodyHandler.create());
        router.get("/event/:eventID").handler(this::handleTrackingEvent);
        router.get("/cache/fill").handler(this::fillCacheWithData);
        router.get("/cache/purge").handler(this::purgeCache);
        router.get("/health/check").handler(this::checkHealth);

        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setCompressionSupported(true);

        httpServer = vertx.createHttpServer(httpServerOptions).getDelegate();
        httpServer.requestHandler(router).listen(8080);
    }

    private void checkHealth(final RoutingContext routingContext) {
        HttpServerResponse response = routingContext.request().response();
        response.setStatusCode(200);
        response.putHeader("content-type", "application/json");
        response.end();
    }

    private void purgeCache(final RoutingContext routingContext) {
        eb.send(Constants.REDIS_PURGE_EVENTBUS_ADDRESS, "");
        eb.send(Constants.CACHE_PURGE_EVENTBUS_ADDRESS, "");

        HttpServerResponse response = routingContext.request().response();
        response.setStatusCode(200);
        response.putHeader("content-type", "application/json");
        response.end();
    }

    private void fillCacheWithData(final RoutingContext routingContext) {
        LOGGER.info("Filling caches with data ... ");
        LOGGER.fine("Reading JSON-data");

        FileSystem fs = vertx.fileSystem().getDelegate();
        fs.readFile("data.json")
                .onSuccess(buf -> {
                    JsonArray jsonArray = buf.toJsonArray();
                    for (Object aJsonArray : jsonArray) {
                        JsonObject obj = (JsonObject) aJsonArray;
                        LOGGER.info("Sending message to cache-verticles: " + obj);
                        eb.send(Constants.CACHE_STORE_EVENTBUS_ADDRESS, obj);
                        eb.send(Constants.REDIS_STORE_EVENTBUS_ADDRESS, obj);
                    }
                    routingContext.end();
                })
                .onFailure(err -> {
                    LOGGER.info(err.getMessage());
                    routingContext.fail(err);
                });
    }

    private void handleTrackingEvent(final RoutingContext routingContext) {

        String userAgent = routingContext.request().getHeader("User-Agent");
        String eventID = routingContext.request().getParam("eventID");

        UUID uuid = UUID.randomUUID();
        TrackingMessage trackingMessage = new TrackingMessage();
        trackingMessage.setMessageId(uuid.toString());
        trackingMessage.setProgramId(eventID);

        JsonObject message = JsonObject.mapFrom(trackingMessage);

        if (null == eventID) {
            routingContext.fail(400);
            return;
        }
        eb
                .<JsonObject>request(Constants.CACHE_EVENTBUS_ADDRESS, message)
                .onSuccess(res -> {
                    JsonObject result = res.body();
                    if (result.isEmpty()) {
                        sendResponse(routingContext, 404, Json.encode("ProgramId not found"));
                        return;
                    }

                    TrackingMessage tmpMsg = Json.decodeValue(result.encode(), TrackingMessage.class);
                    tmpMsg.setUserAgent(userAgent);

                    String enrichedData = Json.encode(tmpMsg);

                    eb.send(Constants.KINESIS_EVENTBUS_ADDRESS, enrichedData);
                    sendResponse(routingContext, 200, enrichedData);
                })
                .onFailure(err -> {
                    LOGGER.severe(err.getMessage());
                    routingContext.fail(err);
                });
    }

    private void sendResponse(final RoutingContext routingContext, int statusCode, final String message) {
        HttpServerResponse response = routingContext.request().response();
        response.setStatusCode(statusCode);
        response.putHeader("content-type", "application/json");

        if (message != null)
            response.end(message);
        else
            response.end();
    }
}
