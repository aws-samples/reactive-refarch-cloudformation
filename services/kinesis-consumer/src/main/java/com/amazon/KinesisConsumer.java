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
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;

import java.sql.Timestamp;
import java.util.UUID;

public class KinesisConsumer {

    public String updateDynamoDb(final DynamoDB dynamoDB, final String tableName, final TrackingMessage trackingMessage) {
        // Write data in DynamoDB
        String id = UUID.randomUUID().toString();

        String updatedAt = new Timestamp(System.currentTimeMillis()).toString();

        Item item = new Item().withPrimaryKey("id", id)
                .withString("updated_at", updatedAt)
                .withString("checksum", trackingMessage.getChecksum())
                .withString("customer_name", trackingMessage.getCustomerName())
                .withString("message_id", trackingMessage.getMessageId())
                .withString("program_id", trackingMessage.getProgramId())
                .withString("program_name", trackingMessage.getProgramName())
                .withString("user_agent", trackingMessage.getUserAgent())
                .withInt("customer_id", trackingMessage.getCustomerId());

        Table table = dynamoDB.getTable(tableName);

        PutItemSpec spec = new PutItemSpec();
        spec.withItem(item).withReturnValues(ReturnValue.ALL_OLD);

        table.putItem(spec);
        return id;
    }

}
