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

import com.amazon.exceptions.KinesisException;
import com.amazon.proto.TrackingEventProtos;

import com.amazon.vo.TrackingMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.amazon.util.Constants.KINESIS_EVENTBUS_ADDRESS;
import static com.amazon.util.Constants.STREAM_NAME;


public class KinesisVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisVerticle.class);
    private KinesisAsyncClient kinesisAsyncClient;
    private String eventStream = "EventStream";

    @Override
    public void start() {

        EventBus eb = vertx.eventBus();

        kinesisAsyncClient = createClient();
        eventStream = System.getenv(STREAM_NAME) == null ? "EventStream" : System.getenv(STREAM_NAME);

        eb.consumer(KINESIS_EVENTBUS_ADDRESS, message -> {
            try {
                TrackingMessage trackingMessage = Json.decodeValue((String)message.body(), TrackingMessage.class);
                String partitionKey = trackingMessage.getMessageId();

                byte [] byteMessage = createMessage(trackingMessage);

                sendMessageToKinesis(byteMessage, partitionKey);

                // Now send back reply
                message.reply("OK");
            }
            catch (KinesisException exc) {
                LOGGER.error(exc);
            }
        });
    }

    @Override
    public void stop() {
        if (kinesisAsyncClient != null) {
            kinesisAsyncClient.close();
        }
    }

    protected void sendMessageToKinesis(byte [] byteMessage, String partitionKey) throws KinesisException {
        if (null == kinesisAsyncClient) {
            throw new KinesisException("AmazonKinesisAsync is not initialized");
        }

        SdkBytes payload = SdkBytes.fromByteArray(byteMessage);
        PutRecordRequest putRecordRequest = PutRecordRequest.builder()
                .partitionKey(partitionKey)
                .streamName(eventStream)
                .data(payload)
                .build();

        LOGGER.debug("Writing to streamName " + eventStream + " using partitionkey " + partitionKey);

        CompletableFuture<PutRecordResponse> response = kinesisAsyncClient.putRecord(putRecordRequest);

        try
        {
            PutRecordResponse recordResult = response.get();
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

    private KinesisAsyncClient createClient() {

        ClientAsyncConfiguration clientConfiguration = ClientAsyncConfiguration.builder().build();

        // Reading credentials from ENV-variables
        AwsCredentialsProvider awsCredentialsProvider = DefaultCredentialsProvider.builder().build();

        // Configuring Kinesis-client with configuration
        String tmp = System.getenv("REGION");

        Region myRegion = null;
        if (tmp == null || tmp.trim().length() == 0) {
            myRegion = Region.US_EAST_1;
            LOGGER.info("Using default region");
        } else {
            myRegion = Region.of(tmp);
        }

        LOGGER.info("Deploying in Region " + myRegion.toString());

        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .maxConcurrency(100)
                .maxPendingConnectionAcquires(10_000)
                .build();

        KinesisAsyncClient kinesisClient = KinesisAsyncClient.builder()
                .httpClient(httpClient)
                .asyncConfiguration(clientConfiguration)
                .credentialsProvider(awsCredentialsProvider)
                .region(myRegion)
                .build();

        httpClient.close();

        return kinesisClient;
    }
}
