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
use aws_config::meta::region::RegionProviderChain;
use aws_lambda_events::event::kinesis::KinesisEvent;
use lambda_runtime::{run, service_fn, Error, LambdaEvent};
use quick_protobuf::{MessageRead, BytesReader};
use aws_sdk_dynamodb::{client::Client};
mod persistence;
use persistence::DynamoDBStore;

use tracing::log::info;

mod model {
    pub mod item;
    pub mod tracking;
}

// Import the Item struct into the current scope
use model::item::Item;
use model::tracking::TrackingEvent;
use crate::persistence::StorePut;


/// This is the main body for the function.
/// Write your code inside it.
/// There are some code example in the following URLs:
/// - https://github.com/awslabs/aws-lambda-rust-runtime/tree/main/examples
/// - https://github.com/aws-samples/serverless-rust-demo/
async fn function_handler(event: LambdaEvent<KinesisEvent>, store: &DynamoDBStore) -> Result<(), Error> {
    // Extract some useful information from the request

    let protobuf_data= event.payload;

    for record in protobuf_data.records {
        let data = record.kinesis.data;
        let mut reader = BytesReader::from_bytes(&data);
        let event = TrackingEvent::from_reader(&mut reader, &data).expect("Cannot read TrackingEvent");

        let my_item = Item {
            program_id: event.programid.to_string(),
            checksum: event.checksum.to_string(),
            customer_id: event.customer_id,
            user_agent: event.user_agent.to_string(),
            program_name: event.program_name.to_string(),
            customer_name: event.customer_name.to_string(),
            is_valid: event.is_valid,
            message_id: event.message_id.to_string()
        };

        info!("Received item '{}'", my_item);

        store.put(&my_item).await.expect("Something went wrong!");

    }

    Ok(())
}

#[tokio::main]
async fn main() -> Result<(), Error> {

    tracing_subscriber::fmt()
        .with_max_level(tracing::Level::INFO)
        // disable printing the name of the module in every log line.
        .with_target(false)
        // disabling time is handy because CloudWatch will add the ingestion time.
        .without_time()
        .init();

    let table_name = match env::var_os("TABLE_NAME") {
        Some(v) => v.into_string().unwrap(),
        None => panic!("$TABLE_NAME is not set")
    };

    let region_provider = RegionProviderChain::default_provider().or_else("eu-west-1");
    let config = aws_config::from_env().region(region_provider).load().await;

    let client = Client::new(&config);

    let ddb_store = DynamoDBStore::new(client, table_name.to_string());
    let ddb_store_ref = & ddb_store;

    //run(service_fn(function_handler)).await

    let func = service_fn(move |event| async move { function_handler(event, ddb_store_ref).await });
    run(func).await
}
