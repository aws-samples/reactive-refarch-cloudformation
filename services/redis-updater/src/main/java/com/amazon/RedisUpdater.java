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

import com.amazon.util.Constants;
import com.amazon.vo.TrackingMessage;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Map;

public class RedisUpdater {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void updateRedisData(final TrackingMessage trackingMessage, final Jedis jedis, final LambdaLogger logger) {
        try {

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(trackingMessage);

            this.log("Updating Redis with object " + jsonString, logger);

            Map<String, String> map = marshal(jsonString);
            String statusCode = jedis.hmset(trackingMessage.getProgramId(), map);

            this.log("Status: " + statusCode, logger);
        }
        catch (Exception exc) {
            if (null == logger)
                exc.printStackTrace();
            else
                logger.log(exc.getMessage());
        }
    }

    private void log(String message, LambdaLogger logger) {
        if (null == logger)
            System.out.println(message);
        else
            logger.log(message);
    }

    public void updateRedisData(final TrackingMessage trackingMessage, final Jedis jedis) {
        this.updateRedisData(trackingMessage, jedis, null);
    }

    public void notifySubscribers(final TrackingMessage trackingMessage, final Jedis jedis) {
        this.notifySubscribers(trackingMessage, jedis, null);
    }

    public void notifySubscribers(final TrackingMessage trackingMessage, final Jedis jedis, final LambdaLogger logger) {
        try {

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(trackingMessage);

            this.log("Sending data " + jsonString + " to " + Constants.REDIS_PUBSUB_CHANNEL, logger);
            jedis.publish(Constants.REDIS_PUBSUB_CHANNEL, jsonString);
        }

        catch (final IOException e) {
            log(e.getMessage(), logger);
        }
    }

    private Map<String, String> marshal(final String jsonString) {

        try {
            Map<String, String> map = MAPPER.readValue(jsonString, new TypeReference<Map<String, String>>() {
            });
            return map;
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
