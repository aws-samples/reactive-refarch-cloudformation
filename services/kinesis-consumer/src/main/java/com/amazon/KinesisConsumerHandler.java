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

import com.amazon.proto.TrackingEventProtos;
import com.amazon.vo.TrackingMessage;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.ByteBuffer;

public class KinesisConsumerHandler implements RequestHandler<KinesisEvent, Void> {

    private DynamoDB dynamoDB;

    public Void handleRequest(KinesisEvent kinesisEvent, Context context) {

        LambdaLogger logger = context.getLogger();

        if (null == dynamoDB) {
            logger.log("Creating DynamodbClient ... ");
            Region region = Region.getRegion(Regions.fromName(System.getenv("AWS_REGION")));

            logger.log("Current Region: " + region.getName());
            AmazonDynamoDB client = createDynamodbClient(region);
            dynamoDB = new DynamoDB(client);
        } else {
            logger.log("Reusing DynamodbClient");
        }

        KinesisConsumer consumer = new KinesisConsumer();
        String tableName = System.getenv(Constants.TABLE_NAME);
        logger.log("Received " + kinesisEvent.getRecords().size() + " raw Event Records.");

        for (KinesisEvent.KinesisEventRecord eventRecord : kinesisEvent.getRecords()) {
            // Unwrap protobuf
            try {
                ByteBuffer buffer = eventRecord.getKinesis().getData();
                TrackingEventProtos.TrackingEvent.Builder trackingEventBuilder = TrackingEventProtos.TrackingEvent.newBuilder();
                trackingEventBuilder.mergeFrom(buffer.array());
                TrackingEventProtos.TrackingEvent trackingEvent = trackingEventBuilder.build();

                TrackingMessage trackingMessage = new TrackingMessage();

                trackingMessage.setCustomerId(trackingEvent.getCustomerId());
                trackingMessage.setMessageId(trackingEvent.getMessageId());
                trackingMessage.setChecksum(trackingEvent.getChecksum());
                trackingMessage.setValid(trackingEvent.getIsValid());
                trackingMessage.setCustomerName(trackingEvent.getCustomerName());
                trackingMessage.setProgramId(trackingEvent.getProgramid());
                trackingMessage.setProgramName(trackingEvent.getProgramName());
                trackingMessage.setUserAgent(trackingEvent.getUserAgent());

                consumer.updateDynamoDb(dynamoDB, tableName, trackingMessage);
            }

            catch (InvalidProtocolBufferException exc) {
                logger.log(exc.getMessage());
            }
        }

        return null;
    }

    private AmazonDynamoDB createDynamodbClient(final Region region) {
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(region.getName())
                .build();
    }
}
