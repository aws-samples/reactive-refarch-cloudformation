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
	"strconv"
	"testing"

	"github.com/aws-samples/reactive-refarch-cloudformation/services/redis-updater/model"
	"github.com/go-redis/redis"
	"github.com/stretchr/testify/assert"
)

func TestPersistData(t *testing.T) {

	redisHost := "localhost"
	redisPort := "6379"

	redisClient := redis.NewClient(&redis.Options{
		Addr: redisHost + ":" + redisPort,
		DB:   0, // use default DB
	})

	event := prepareData()

	PersistData(event, *redisClient)

	fmt.Println("Using key " + event.ProgramID)
	retVal := redisClient.HGetAll(event.ProgramID).Val()

	custID, err := strconv.ParseInt(retVal["CustomerID"], 10, 32)
	cID := int32(custID)

	assert.Equal(t, retVal["ProgramID"], event.ProgramID, "ProgramId not the same value")
	assert.Equal(t, retVal["Checksum"], event.Checksum, "Checksum not the same value")
	assert.Equal(t, cID, event.CustomerID, "CustomerID not the same value")
	assert.Equal(t, retVal["CustomerName"], event.CustomerName, "CustomerName not the same value")
	assert.Equal(t, retVal["ProgramName"], event.ProgramName, "ProgramName not the same value")

	pubSub := redisClient.Subscribe("channelTest")
	defer pubSub.Close()

	NotifySubscribers(event, *redisClient, "channelTest")

	msg, err := pubSub.Receive()

	if err != nil {
		panic(err)
	}

	fmt.Println(msg)
}

func prepareData() model.Message {
	event := model.Message{
		ProgramID:    "programId",
		Checksum:     "checksum",
		CustomerID:   1234,
		CustomerName: "customerName",
		ProgramName:  "programName",
		IsValid:      true}

	return event
}
