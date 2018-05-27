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
	"time"

	"github.com/aws-samples/reactive-refarch-cloudformation/services/kinesis-consumer/model"
	"github.com/aws/aws-sdk-go/aws"

	"github.com/aws/aws-sdk-go/service/dynamodb"
	"github.com/aws/aws-sdk-go/service/dynamodb/dynamodbattribute"
	"github.com/nu7hatch/gouuid"
)

// PersistData is a function to persist data in DynamoDB
func PersistData(ddb dynamodb.DynamoDB, tablename string, msg model.Message) string {
	uuid, err := uuid.NewV4()

	if err != nil {
		fmt.Printf("Failed creating UUID %s", err)
	} else {
		fmt.Printf("Created UUID %s", uuid)
	}

	timestamp := time.Now().Format(time.StampMicro)

	msg.ID = uuid.String()
	msg.UpdatedAt = timestamp

	av, err := dynamodbattribute.MarshalMap(msg)

	if err != nil {
		fmt.Println("Got error marshalling map:")
		fmt.Println(err.Error())
	}

	input := &dynamodb.PutItemInput{
		Item:      av,
		TableName: aws.String(tablename),
	}

	_, err = ddb.PutItem(input)

	if err != nil {
		fmt.Println("Got error calling PutItem:")
		fmt.Println(err.Error())
	}

	return msg.ID
}
