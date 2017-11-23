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
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static com.amazon.util.Constants.REDIS_HOST;
import static com.amazon.util.Constants.REDIS_PORT;

public class RedisUpdaterHandler implements RequestHandler<KinesisEvent, Void> {

    private Jedis jedis;
    private Charset charset = Charset.forName("UTF-8");
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Void handleRequest(KinesisEvent kinesisEvent, Context context) {

        RedisUpdater redisUpdater = new RedisUpdater();
        LambdaLogger logger = context.getLogger();
        logger.log("Received " + kinesisEvent.getRecords().size() + " raw Event Records.");

        for (KinesisEvent.KinesisEventRecord eventRecord : kinesisEvent.getRecords()) {
            // Update Redis
            if (null == jedis || !jedis.isConnected()) {
                logger.log("Connection to Redis is closed, trying to reconnect ... ");

                if (System.getenv(REDIS_HOST) == null) {
                    logger.log("Not Redis host specified");
                    return null;
                }
                String redisHost = System.getenv(REDIS_HOST);
                int redisPort = System.getenv(REDIS_PORT) == null ? 6379 : Integer.parseInt(System.getenv(REDIS_PORT));

                logger.log("Connection to " + redisHost);
                jedis = new Jedis(redisHost, redisPort);
            }

            ByteBuffer kinesisData = eventRecord.getKinesis().getData();
            String textData = charset.decode(kinesisData).toString();

            try {
                TrackingMessage trackingMessage = mapper.readValue(textData, TrackingMessage.class);

                redisUpdater.updateRedisData(trackingMessage, jedis, logger);
                redisUpdater.notifySubscribers(trackingMessage, jedis, logger);
            }

            catch (Exception exc) {
                if (null == logger)
                    exc.printStackTrace();
                else
                    logger.log(exc.getMessage());
            }
        }

        return null;
    }


}
