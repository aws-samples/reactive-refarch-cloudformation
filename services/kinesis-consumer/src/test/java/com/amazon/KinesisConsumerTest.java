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
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class KinesisConsumerTest {

    private static final Logger LOGGER = LogManager.getLogger(KinesisConsumerTest.class);
    private  AmazonDynamoDB client;

    @BeforeClass
    public static void before() throws Exception {
    }

    @AfterClass
    public static void after() throws Exception {

    }

    private static TrackingMessage prepareData() {
        TrackingMessage msg = new TrackingMessage();
        msg.setMessageId("messageId");
        msg.setUserAgent("myUserAgent");
        msg.setProgramId("12345");
        msg.setProgramName("myProgram");
        msg.setCustomerName("myCustomer");
        msg.setCustomerId(1234);
        msg.setChecksum("check123");
        msg.setValid(true);

        return msg;
    }

    @Test
    public void writeToDynamoDb() {

        client  = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_WEST_1)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
        DynamoDB dynamoDB = new DynamoDB(client);

        // Write data to local DynamoDB
        TrackingMessage trackingMessage = prepareData();
        KinesisConsumer kinesisConsumer = new KinesisConsumer();
        String id = kinesisConsumer.updateDynamoDb(dynamoDB, "test_table", trackingMessage);

        LOGGER.info("id " + id + " stored");
        Table table = dynamoDB.getTable("test_table");

        Item item = table.getItem("id", id);

        LOGGER.info(item);
        Assert.assertNotNull(item);
        Assert.assertEquals(item.getString("program_id"), trackingMessage.getProgramId());
    }
}
