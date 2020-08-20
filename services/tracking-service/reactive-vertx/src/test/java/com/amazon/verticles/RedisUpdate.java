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

import com.amazon.proto.TrackingEventProtos;
import com.amazon.vo.TrackingMessage;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.concurrent.CompletableFuture;

public class RedisUpdate {

    private static final String kinesisStream = "redis-update";

    public static void main (String ... args) {

        KinesisAsyncClient kinesisAsync = createClient();

        byte[] msg = prepareData();

        SdkBytes payload = SdkBytes.fromByteArray(msg);
        PutRecordRequest putRecordRequest = PutRecordRequest.builder()
                .partitionKey("test")
                .streamName(kinesisStream)
                .data(payload)
                .build();

        CompletableFuture<PutRecordResponse> future = kinesisAsync.putRecord(putRecordRequest);

        future.whenComplete((result, e) -> {
            if (e != null) {
                e.printStackTrace();
            } else {
                String sequenceNumber = result.sequenceNumber();
                System.out.println("Message sequence number: " + sequenceNumber);
            }
        });
    }

    private static KinesisAsyncClient createClient() {

        ClientAsyncConfiguration clientConfiguration = ClientAsyncConfiguration.builder().build();

        // Reading credentials from ENV-variables
        AwsCredentialsProvider awsCredentialsProvider = DefaultCredentialsProvider.builder().build();

        // Configuring Kinesis-client with configuration
        String tmp = System.getenv("REGION");

        Region myRegion;
        if (tmp == null || tmp.trim().length() == 0) {
            myRegion = Region.US_EAST_1;
            System.out.println("Using default region");
        } else {
            myRegion = Region.of(tmp);
        }

        System.out.println("Deploying in Region " + myRegion.toString());

        return KinesisAsyncClient.builder()
                .asyncConfiguration(clientConfiguration)
                .credentialsProvider(awsCredentialsProvider)
                .region(myRegion)
                .build();
    }

    private static byte [] prepareData() {
        TrackingMessage trackingMessage = new TrackingMessage();
        trackingMessage.setMessageId("messageId");
        trackingMessage.setUserAgent("myUserAgent");
        trackingMessage.setProgramId("12345");
        trackingMessage.setProgramName("program1");
        trackingMessage.setCustomerName("myCustomer");
        trackingMessage.setCustomerId(1234);
        trackingMessage.setChecksum("check123");
        trackingMessage.setValid(true);

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
}
