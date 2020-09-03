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
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.UUID;

public class HttpVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpVerticle.class);
    private EventBus eb;

    @Override
    public void start() {

        this.eb = vertx.eventBus();

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.get("/event/:eventID").handler(this::handleTrackingEvent);
        router.get("/event/delete/:eventID").handler(this::handleTrackingEventForDeletion);
        router.get("/cache/fill").handler(this::fillCacheWithData);
        router.get("/cache/purge").handler(this::purgeCache);
        router.get("/health/check").handler(this::checkHealth);

        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setCompressionSupported(true);

        HttpServer httpServer = vertx.createHttpServer(httpServerOptions);
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
        LOGGER.debug("Reading JSON-data");

        FileSystem fs = vertx.fileSystem();
        fs.readFile("META-INF/data.json", res -> {
            if (res.succeeded()) {
                Buffer buf = res.result();
                JsonArray jsonArray = buf.toJsonArray();
                for (Object aJsonArray : jsonArray) {
                    JsonObject obj = (JsonObject) aJsonArray;
                    LOGGER.debug("Sending message to cache-verticles: " + obj);
                    eb.send(Constants.CACHE_STORE_EVENTBUS_ADDRESS, obj);
                    eb.send(Constants.REDIS_STORE_EVENTBUS_ADDRESS, obj);
                }
            } else {
                LOGGER.info(res.cause());
            }

            HttpServerResponse response = routingContext.request().response();
            response.putHeader("content-type", "application/json");
            response.end();
        });
    }

    private JsonObject parseData(final RoutingContext routingContext) {
        String eventID = routingContext.request().getParam("eventID");

        UUID uuid = UUID.randomUUID();
        TrackingMessage trackingMessage = new TrackingMessage();
        trackingMessage.setMessageId(uuid.toString());
        trackingMessage.setProgramId(eventID);

        JsonObject message = JsonObject.mapFrom(trackingMessage);

        return message;
    }

    private void handleTrackingEventForDeletion(final RoutingContext routingContext) {
        String eventID = routingContext.request().getParam("eventID");

        JsonObject message = this.parseData(routingContext);

        if (null == eventID) {
            sendError(400, routingContext);
        } else {
            eb.send(Constants.REDIS_DELETE_EVENTBUS_ADDRESS, message, res -> {
                if (res.succeeded()) {
                    JsonObject result = (JsonObject) res.result().body();
                    sendResponse(routingContext, 200, result.toString());
                } else {
                    LOGGER.error(res.cause());
                    sendResponse(routingContext, 500, res.cause().getMessage());
                }
            });
        }
    }

    private void handleTrackingEvent(final RoutingContext routingContext) {

        String userAgent = routingContext.request().getHeader("User-Agent");
        String eventID = routingContext.request().getParam("eventID");

        JsonObject message = this.parseData(routingContext);

        if (null == eventID) {
            sendError(400, routingContext);
        } else {
            eb.send(Constants.CACHE_EVENTBUS_ADDRESS, message, res -> {
                if (res.succeeded()) {
                    JsonObject result = (JsonObject) res.result().body();
                    if (result.isEmpty()) {
                        sendResponse(routingContext, 404, Json.encode("ProgramId not found"));
                    } else {

                        TrackingMessage tmpMsg = Json.decodeValue(result.encode(), TrackingMessage.class);
                        tmpMsg.setUserAgent(userAgent);

                        String enrichedData = Json.encode(tmpMsg);

                        eb.send(Constants.KINESIS_EVENTBUS_ADDRESS, enrichedData);
                        sendResponse(routingContext, 200, enrichedData);
                    }
                } else {
                    LOGGER.error(res.cause());
                    sendResponse(routingContext, 500, res.cause().getMessage());
                }
            });
        }
    }

    private void sendError(int statusCode, final RoutingContext routingContext) {
        HttpServerResponse response = routingContext.request().response();
        response.setStatusCode(statusCode).end();
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
