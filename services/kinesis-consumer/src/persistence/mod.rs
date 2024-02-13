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

use crate::{Error};
use async_trait::async_trait;

mod dynamodb;
pub use dynamodb::DynamoDBStore;

use crate::model::item::Item;

pub trait Store: StoreGetAll + StoreGet + StorePut + StoreDelete {}

/// Trait for retrieving all products
///
/// This trait is implemented by the different storage backends. It provides
/// the basic interface for retrieving all products.

#[async_trait]
pub trait StoreGetAll: Send + Sync {
    async fn all(&self) -> Result<(), Error>;
}

/// Trait for retrieving a single product
#[async_trait]
pub trait StoreGet: Send + Sync {
    async fn get(&self, id: &str) -> Result<(), Error>;
}

/// Trait for storing a single product
#[async_trait]
pub trait StorePut: Send + Sync {
    async fn put(&self, event: &Item) -> Result<(), Error>;
}

/// Trait for deleting a single product
#[async_trait]
pub trait StoreDelete: Send + Sync {
    async fn delete(&self, id: &str) -> Result<(), Error>;
}
