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

package com.amazon.vo;

public class TrackingMessage {
    private String userAgent;
    private String programId;
    private String programName;
    private String checksum;
    private Integer customerId;
    private String customerName;
    private String messageId;
    private boolean isValid;

    public TrackingMessage() {
    }

    public TrackingMessage(String userAgent, String programId, String checksum, Integer customerId,
                           String customerName, boolean isValid, String messageId, String programName) {
        this.userAgent = userAgent;
        this.programId = programId;
        this.checksum = checksum;
        this.customerId = customerId;
        this.customerName = customerName;
        this.isValid = isValid;
        this.messageId = messageId;
        this.programName = programName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    @Override
    public String toString() {
        return "TrackingMessage{" +
                "userAgent='" + userAgent + '\'' +
                ", programId='" + programId + '\'' +
                ", programName='" + programName + '\'' +
                ", checksum='" + checksum + '\'' +
                ", customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                ", messageId='" + messageId + '\'' +
                ", isValid=" + isValid +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackingMessage that = (TrackingMessage) o;

        return getProgramId() != null ? getProgramId().equals(that.getProgramId()) : that.getProgramId() == null;
    }

    @Override
    public int hashCode() {
        return getProgramId() != null ? getProgramId().hashCode() : 0;
    }
}
