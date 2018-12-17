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

package com.amazon.util;

public class Constants {
    public final static String KINESIS_EVENTBUS_ADDRESS = "com.amazon.kinesis.handler";
    public final static String REDIS_EVENTBUS_ADDRESS = "com.amazon.redis.handler";
    public final static String REDIS_STORE_EVENTBUS_ADDRESS = "com.amazon.redis.store.handler";
    public final static String REDIS_PURGE_EVENTBUS_ADDRESS = "com.amazon.redis.purge.handler";
    public final static String CACHE_STORE_EVENTBUS_ADDRESS = "com.amazon.cache.store.handler";
    public final static String CACHE_PURGE_EVENTBUS_ADDRESS = "com.amazon.cache.purge.handler";
    public final static String CACHE_EVENTBUS_ADDRESS = "com.amazon.cache.handler";
    public final static String CACHE_REDIS_EVENTBUS_ADDRESS = "com.amazon.cache.redis.handler";
    public final static String REDIS_PUBSUB_CHANNEL_VERTX = "io.vertx.redis.channel1";
    public final static String REDIS_PUBSUB_CHANNEL = "channel1";

    public final static String REDIS_HOST = "REDIS_HOST";
    public final static String REDIS_PORT = "REDIS_PORT";
    public final static String STREAM_NAME = "EVENT_STREAM";
}
