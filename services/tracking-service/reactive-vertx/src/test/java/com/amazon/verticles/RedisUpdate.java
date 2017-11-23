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

import com.amazon.vo.TrackingMessage;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import io.vertx.core.json.Json;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RedisUpdate {

    private static final String kinesisStream = "redis-update";

    private static Charset charset = Charset.forName("UTF-8");

    public static void main (String ... args) {

        AmazonKinesisAsync kinesisAsync = createClient();
        PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setStreamName(kinesisStream);
        putRecordRequest.setPartitionKey("test");

        TrackingMessage msg = prepareData();

        String jsonString = Json.encode(msg);
        System.out.println("Sending JSON-String: " + jsonString);

        ByteBuffer buffer = charset.encode(jsonString);
        putRecordRequest.setData(buffer);

        Future<PutRecordResult> futureResult = kinesisAsync.putRecordAsync(putRecordRequest);
        try
        {
            PutRecordResult recordResult = futureResult.get();
            System.out.println("Sent message to Kinesis: " + recordResult.toString());
        }

        catch (InterruptedException | ExecutionException iexc) {
            System.out.println(iexc);
        }
    }

    private static AmazonKinesisAsync createClient() {

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
        Region myRegion = Regions.getCurrentRegion();

        if (null == myRegion)
            myRegion = Region.getRegion(Regions.EU_WEST_1);

        System.out.println("Using Region " + myRegion);

        AmazonKinesisAsync kinesisClient = AmazonKinesisAsyncClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withCredentials(awsCredentialsProvider)
                .withRegion(myRegion.getName())
                .build();

        return kinesisClient;
    }

    private static TrackingMessage prepareData() {
        TrackingMessage msg = new TrackingMessage();
        msg.setMessageId("messageId");
        msg.setUserAgent("myUserAgent");
        msg.setProgramId("12345");
        msg.setProgramName("program1");
        msg.setCustomerName("myCustomer");
        msg.setCustomerId(1234);
        msg.setChecksum("check123");
        msg.setValid(true);

        return msg;
    }
}
