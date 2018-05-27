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
	"encoding/json"
	"fmt"

	"github.com/aws-samples/reactive-refarch-cloudformation/services/redis-updater/model"
	"github.com/fatih/structs"
	"github.com/go-redis/redis"
)

// PersistData stores data in Redis
func PersistData(msg model.Message, client redis.Client) {

	values := structs.Map(msg)
	status := client.HMSet(msg.ProgramID, values)
	if status.Err() != nil {
		fmt.Println("Error writing data:")
		fmt.Println(status.Err().Error())
	}

	msgBytes, err := json.Marshal(msg)

	if err != nil {
		fmt.Println("Error marshalling data:")
		fmt.Println(err.Error())
	}

	msgString := string(msgBytes)
	fmt.Println("Persisting data: " + msgString)
}

// NotifySubscribers notifies all subscribers of data changes
func NotifySubscribers(msg model.Message, client redis.Client, channel string) {

	msgBytes, err := json.Marshal(msg)

	if err != nil {
		fmt.Println("Error marshalling data:")
		fmt.Println(err.Error())
	}

	msgString := string(msgBytes)
	client.Publish(channel, msgString)
	fmt.Println("Publishing to subscribers: " + msgString)
}
