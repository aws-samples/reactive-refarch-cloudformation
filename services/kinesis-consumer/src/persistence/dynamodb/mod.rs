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

use super::{Store, StoreDelete, StoreGet, StoreGetAll, StorePut};

use aws_sdk_dynamodb::types::AttributeValue;

use crate::{Error};
use crate::model::item::Item;

use aws_sdk_dynamodb::{Client};
use async_trait::async_trait;
use tracing::instrument;
use tracing::log::info;
use uuid::Uuid;
use std::time::{Duration, SystemTime};

/// DynamoDB store implementation.
pub struct DynamoDBStore {
    client: Client,
    table_name: String,
}

impl DynamoDBStore {
    pub fn new(client: Client, table_name: String) -> DynamoDBStore {
        DynamoDBStore { client, table_name }
    }
}

#[async_trait]
impl StoreGetAll for DynamoDBStore {
    #[instrument(skip(self))]
    async fn all(&self) -> Result<(), Error> {
        Ok(())
    }
}

#[async_trait]
impl StoreGet for DynamoDBStore {
    #[instrument(skip(self))]
    async fn get(&self, id: &str) -> Result<(), Error> {
        Ok(())
    }
}

#[async_trait]
impl StoreDelete for DynamoDBStore {
    #[instrument(skip(self))]
    async fn delete(&self, id: &str) -> Result<(), Error> {
        Ok(())
    }
}

#[async_trait]
impl StorePut for DynamoDBStore {
    /// Create or update an item
    #[instrument(skip(self))]
    async fn put(&self, item: &Item) -> Result<(), Error> {
        info!("Putting item with id '{}' into DynamoDB table", item.customer_id);

        let message_id_av = AttributeValue::S(item.message_id.clone());
        let customer_name_av = AttributeValue::S(item.customer_name.clone());
        let checksum_av = AttributeValue::S(item.checksum.clone());
        let user_agent_av = AttributeValue::S(item.user_agent.clone());
        let program_id_av = AttributeValue::S(item.program_id.clone());
        let customer_id_av = AttributeValue::N(item.customer_id.to_string());
        let program_name_av = AttributeValue::S(item.program_name.clone());
        let uuid = Uuid::new_v4();
        let id = AttributeValue::S(uuid.to_string());
        let duration_since_epoch = SystemTime::now().duration_since(SystemTime::UNIX_EPOCH).unwrap();
        let timestamp_millis = AttributeValue::S(duration_since_epoch.as_millis().to_string());

        self.client
            .put_item()
            .table_name(&self.table_name)
            .item("id", id)
            .item("message_id", message_id_av)
            .item("customer_name", customer_name_av)
            .item("checksum", checksum_av)
            .item("user_agent", user_agent_av)
            .item("program_id", program_id_av)
            .item("customer_id", customer_id_av)
            .item("program_name", program_name_av)
            .item("updated_at", timestamp_millis)
            .send()
            .await?;

        Ok(())
    }
}