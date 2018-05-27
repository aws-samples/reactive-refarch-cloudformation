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
package model

// Message is an exported type that
// contains all values for a tracking message
type Message struct {
	ProgramID    string `json:"programId"`
	Checksum     string `json:"checksum"`
	CustomerID   int32  `json:"customerId"`
	CustomerName string `json:"customerName"`
	ProgramName  string `json:"programName"`
	IsValid      bool   `json:"valid"`
}
