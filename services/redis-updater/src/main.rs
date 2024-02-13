/*
 * Copyright 2010-2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

use std::env;
use tracing_subscriber::filter::{EnvFilter, LevelFilter};
use aws_lambda_events::event::kinesis::KinesisEvent;
use lambda_runtime::{run, service_fn, Error, LambdaEvent};
use redis::Commands;
use redis::cluster::ClusterClient;
use std::collections::HashMap;

mod model {
    pub mod item;
}

use model::item::Item;

/// This is the main body for the function.
/// Write your code inside it.
/// There are some code example in the following URLs:
/// - https://github.com/awslabs/aws-lambda-rust-runtime/tree/main/examples
/// - https://github.com/aws-samples/serverless-rust-demo/
async fn function_handler(event: LambdaEvent<KinesisEvent>) -> Result<(), Error> {
    // Extract some useful information from the request

    let kinesis_data= event.payload;

    for record in kinesis_data.records {
        let data = record.kinesis.data;

        let bytes = data.0;

        let payload = String::from_utf8(bytes)?;
        let item: Item = serde_json::from_str(&payload)?;

        println!("Received item '{}'", item);

        let map = struct_to_map(&item);
        // Connect to Redis
        let mut redis_conn = get_redis_client()?;

        for (key, value) in map.into_iter() {
            println!("{} / {}", key, value);
            redis_conn.hset(&item.program_id, key, value.to_string())?;
        }
    }

    Ok(())
}

// Function to convert struct into HashMap
fn struct_to_map(my_struct: &Item) -> HashMap<String, serde_json::Value> {
    let json_str = serde_json::to_string(my_struct).unwrap();
    serde_json::from_str(&json_str).unwrap()
}

#[tokio::main]
async fn main() -> Result<(), Error> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::builder()
                .with_default_directive(LevelFilter::INFO.into())
                .from_env_lossy(),
        )
        // disable printing the name of the module in every log line.
        .with_target(false)
        // disabling time is handy because CloudWatch will add the ingestion time.
        .without_time()
        .init();

    run(service_fn(function_handler)).await
}

// Initialize Redis client
fn get_redis_client() -> redis::RedisResult<redis::cluster::ClusterConnection> {
    let redis_host = match env::var_os("REDIS_HOST") {
        Some(v) => v.into_string().unwrap(),
        None => panic!("REDIS_HOST is not set")
    };

    let redis_port = match env::var_os("REDIS_PORT") {
        Some(v) => v.into_string().unwrap(),
        None => panic!("REDIS_PORT is not set")
    };

    let connection_string = format!("redis://{}:{}/", redis_host, redis_port);
    println!("Connect to Redis URL {}", connection_string);

    let nodes = vec![connection_string];
    let client = ClusterClient::new(nodes).unwrap();
    client.get_connection()
}
