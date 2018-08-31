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
package main

import (
	"context"
	"fmt"
	"os"

	"github.com/aws-samples/reactive-refarch-cloudformation/services/kinesis-consumer/services/persistence"

	model "github.com/aws-samples/reactive-refarch-cloudformation/services/kinesis-consumer/model"
	consumer "github.com/aws-samples/reactive-refarch-cloudformation/services/kinesis-consumer/services/consumer"

	"github.com/golang/protobuf/proto"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/dynamodb"
)

var region string
var svc *dynamodb.DynamoDB
var tableName string

func init() {
	region = os.Getenv("AWS_REGION")
	tableName = os.Getenv("TABLE_NAME")

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
	}

	// Create DynamoDB client
	svc = dynamodb.New(sess)
	fmt.Printf("DynamoDB client created")
}

func handler(ctx context.Context, kinesisEvent events.KinesisEvent) error {
	for _, record := range kinesisEvent.Records {
		kinesisRecord := record.Kinesis
		dataBytes := kinesisRecord.Data

		msg := &consumer.TrackingEvent{}
		if err := proto.Unmarshal(dataBytes, msg); err != nil {
			fmt.Println("Got error unmarshalling event:")
			fmt.Println(err.Error())
		}

		event := &model.Message{
			UserAgent:    msg.UserAgent,
			ProgramID:    msg.Programid,
			Checksum:     msg.Checksum,
			CustomerID:   msg.CustomerId,
			CustomerName: msg.CustomerName,
			MessageID:    msg.MessageId,
			ProgramName:  msg.ProgramName}

		msgID, err := persistence.PersistData(*svc, tableName, *event)

		fmt.Println("Message-ID:" + msgID)
		if err != nil {
			return err
		}
	}

	return nil
}

func main() {
	lambda.Start(handler)
}
