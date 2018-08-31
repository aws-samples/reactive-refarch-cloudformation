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

	"encoding/json"

	model "github.com/aws-samples/reactive-refarch-cloudformation/services/redis-updater/model"
	"github.com/aws-samples/reactive-refarch-cloudformation/services/redis-updater/services/persistence"

	"github.com/go-redis/redis"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

var region string
var redisHost string
var redisPort string
var redisChannel string
var redisClient *redis.Client

func init() {
	region = os.Getenv("AWS_REGION")
	redisHost = os.Getenv("REDIS_HOST")
	redisPort = os.Getenv("REDIS_PORT")
	redisChannel = os.Getenv("REDIS_CHANNEL")

	fmt.Printf("Using region %s\n", region)
	fmt.Printf("Using Redis host %s\n", redisHost)
	fmt.Printf("Using Redis port %s\n", redisPort)

	redisClient = redis.NewClient(&redis.Options{
		Addr: redisHost + ":" + redisPort,
		DB:   0, // use default DB
	})
}

func handler(ctx context.Context, kinesisEvent events.KinesisEvent) error {
	for _, record := range kinesisEvent.Records {
		kinesisRecord := record.Kinesis
		dataBytes := kinesisRecord.Data
		dataText := string(dataBytes)

		event := model.Message{}

		if err := json.Unmarshal([]byte(dataText), &event); err != nil {
			fmt.Println("Error unmarshalling data:")
			fmt.Println(err.Error())
		}

		err := persistence.PersistData(event, *redisClient)

		if err != nil {
			return err
		}

		err = persistence.NotifySubscribers(event, *redisClient, redisChannel)
	
		if err != nil {
			return err
		}
	}

	return nil
}

func main() {
	lambda.Start(handler)
}
