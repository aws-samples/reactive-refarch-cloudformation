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

import com.amazon.exceptions.KinesisException;
import com.amazon.proto.TrackingEventProtos;
import com.amazon.util.Constants;
import com.amazon.vo.TrackingMessage;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.amazon.util.Constants.STREAM_NAME;

public class KinesisVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisVerticle.class);
    private AmazonKinesisAsync kinesisAsyncClient;
    private String eventStream = "EventStream";

    @Override
    public void start() throws Exception {

        EventBus eb = vertx.eventBus();

        kinesisAsyncClient = createClient();
        eventStream = System.getenv(STREAM_NAME) == null ? "EventStream" : System.getenv(STREAM_NAME);

        eb.consumer(Constants.KINESIS_EVENTBUS_ADDRESS, message -> {
            try {
                TrackingMessage trackingMessage = Json.decodeValue((String)message.body(), TrackingMessage.class);
                String partitionKey = trackingMessage.getMessageId();

                byte [] byteMessage = createMessage(trackingMessage);
                ByteBuffer buf = ByteBuffer.wrap(byteMessage);

                sendMessageToKinesis(buf, partitionKey);

                // Now send back reply
                message.reply("OK");
            }
            catch (KinesisException exc) {
                LOGGER.error(exc);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (kinesisAsyncClient != null) {
            kinesisAsyncClient.shutdown();
        }
    }

    protected void sendMessageToKinesis(ByteBuffer payload, String partitionKey) throws KinesisException {
        if (null == kinesisAsyncClient) {
            throw new KinesisException("AmazonKinesisAsync is not initialized");
        }

        PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setStreamName(eventStream);
        putRecordRequest.setPartitionKey(partitionKey);

        LOGGER.debug("Writing to streamName " + eventStream + " using partitionkey " + partitionKey);

        putRecordRequest.setData(payload);

        Future<PutRecordResult> futureResult = kinesisAsyncClient.putRecordAsync(putRecordRequest);
        try
        {
            PutRecordResult recordResult = futureResult.get();
            LOGGER.debug("Sent message to Kinesis: " + recordResult.toString());
        }

        catch (InterruptedException | ExecutionException iexc) {
            LOGGER.error(iexc);
        }
    }

    private byte[] createMessage(TrackingMessage trackingMessage) {

        TrackingEventProtos.TrackingEvent.Builder trackingBuilder = TrackingEventProtos.TrackingEvent.newBuilder();
        trackingBuilder.setChecksum(trackingMessage.getChecksum());
        trackingBuilder.setCustomerId(trackingMessage.getCustomerId());
        trackingBuilder.setProgramid(trackingMessage.getProgramId());
        trackingBuilder.setUserAgent(trackingMessage.getUserAgent());
        trackingBuilder.setCustomerId(trackingMessage.getCustomerId());
        trackingBuilder.setCustomerName(trackingMessage.getCustomerName());
        trackingBuilder.setMessageId(trackingMessage.getMessageId());
        trackingBuilder.setProgramName(trackingMessage.getProgramName());

        TrackingEventProtos.TrackingEvent trackingEvent = trackingBuilder.build();
        return trackingEvent.toByteArray();
    }

    private AmazonKinesisAsync createClient() {

        // Building Kinesis configuration
        int connectionTimeout = ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT;
        int maxConnection = ClientConfiguration.DEFAULT_MAX_CONNECTIONS;

        RetryPolicy retryPolicy = ClientConfiguration.DEFAULT_RETRY_POLICY;
        int socketTimeout = ClientConfiguration.DEFAULT_SOCKET_TIMEOUT;
        boolean useReaper = ClientConfiguration.DEFAULT_USE_REAPER;
        String userAgent = ClientConfiguration.DEFAULT_USER_AGENT;

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setConnectionTimeout(connectionTimeout);
        clientConfiguration.setMaxConnections(maxConnection);
        clientConfiguration.setRetryPolicy(retryPolicy);
        clientConfiguration.setSocketTimeout(socketTimeout);
        clientConfiguration.setUseReaper(useReaper);
        clientConfiguration.setUserAgentPrefix(userAgent);

        // Reading credentials from ENV-variables
        AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        // Configuring Kinesis-client with configuration
        String myRegion = System.getenv("REGION");

        if (null == myRegion || myRegion.trim().length() == 0) {
            myRegion = Regions.US_EAST_1.getName();
            LOGGER.info("Using default region");
        }

        LOGGER.info("Deploying in Region " + myRegion);

        AmazonKinesisAsync kinesisClient = AmazonKinesisAsyncClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withCredentials(awsCredentialsProvider)
                .withRegion(myRegion)
                .build();

        return kinesisClient;
    }
}
