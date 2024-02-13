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

use serde::{Serialize, Deserialize};
use std::fmt;

#[derive(Debug, Serialize, Deserialize)]
pub struct Item {
    #[serde(rename = "ProgramId")]
    pub program_id: String,
    #[serde(rename = "Checksum")]
    pub checksum: String,
    #[serde(rename = "CustomerId")]
    pub customer_id: i32,
    #[serde(rename = "UserAgent")]
    pub user_agent: String,
    #[serde(rename = "ProgramName")]
    pub program_name: String,
    #[serde(rename = "CustomerName")]
    pub customer_name: String,
    #[serde(rename = "IsValid")]
    pub is_valid: bool,
    #[serde(rename = "MessageId")]
    pub message_id: String
}

impl fmt::Display for Item {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "({}, {}, {}, {}, {}, {}, {}, {})",
               self.program_id, self.checksum, self.customer_id,
               self.user_agent, self.program_name, self.customer_name,
               self.is_valid, self.message_id
        )
    }
}