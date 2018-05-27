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
package persistence

import (
	"fmt"
	"testing"

	"github.com/aws-samples/reactive-refarch-cloudformation/services/kinesis-consumer/model"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/stretchr/testify/assert"

	"github.com/aws/aws-sdk-go/service/dynamodb"
	"github.com/aws/aws-sdk-go/service/dynamodb/dynamodbattribute"
)

func TestPersistData(t *testing.T) {
	event := prepareData()

	region := "us-east-1"
	tableName := "reactive-ProgramTable-us-east-1"

	fmt.Printf("Using region %s\n", region)
	fmt.Printf("Using DynamoDB table %s\n", tableName)

	// Initialize a session in us-west-2 that the SDK will use to load
	// credentials from the shared credentials file ~/.a	ws/credentials.
	sess, err := session.NewSession(&aws.Config{
		Region: aws.String(region)},
	)

	if err != nil {
		fmt.Println("Error creating session:")
		fmt.Println(err.Error())
		t.Fail()
	}

	// Create DynamoDB client
	svc := dynamodb.New(sess)
	fmt.Printf("DynamoDB client created")

	uuid := PersistData(*svc, tableName, event)

	input := &dynamodb.GetItemInput{
		TableName: aws.String(tableName),
		Key: map[string]*dynamodb.AttributeValue{
			"id": {
				S: aws.String(uuid),
			},
			"customer_id": {
				N: aws.String("1234"),
			},
		},
	}

	itemOutput, err := svc.GetItem(input)

	if err != nil {
		fmt.Println("Error getting data")
		fmt.Println(err.Error())
		t.Fail()
	}

	item := model.Message{}
	err = dynamodbattribute.UnmarshalMap(itemOutput.Item, &item)

	if err != nil {
		fmt.Println("Error getting data")
		fmt.Println(err.Error())
		t.Fail()
	}

	assert.Equal(t, item.ProgramID, event.ProgramID, "ProgramId not the same value")
	assert.Equal(t, item.Checksum, event.Checksum, "Checksum not the same value")
	assert.Equal(t, item.CustomerID, event.CustomerID, "CustomerId not the same value")
	assert.Equal(t, item.MessageID, event.MessageID, "MessageId not the same value")
	assert.Equal(t, item.ProgramName, event.ProgramName, "ProgramName not the same value")
	assert.Equal(t, item.UserAgent, event.UserAgent, "UserAgent not the same value")
}

func prepareData() model.Message {
	event := model.Message{
		MessageID:    "messageId",
		UserAgent:    "myUserAgent",
		ProgramID:    "12345",
		Checksum:     "check123",
		CustomerID:   1234,
		CustomerName: "myCustomer",
		ProgramName:  "myProgram",
	}

	return event
}
