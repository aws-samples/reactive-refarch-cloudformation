# Kinesis Consumer
This AWS Lambda function consumes data from an Amazon Kinesis Date Stream and persists the data in an Amazon DynamoDB table.

## General concept
In this Lambda function, the invocation code is separated from the business logic for better testing. The invocation code is triggered with a maximum number of 100 Kinesis events. Protobuf is used to reduce message size and saturate the Kinesis Stream. Unwrapping the data from Protobuf to Java objects is implemented in the invocation layer (`KinesisConsumerHandler`) of the Lambda function. The objects are passed to the business logic (`KinesisConsumer`) and used to persist data in a DynamoDB table. In order to reduce cold startup time, only few additional libraries are used as dependecies. In contract to the `redis-updater`-function, it is not necessary to access resources in private subnets, so it is sufficient to use the default networking environment of Lambda. 

## Configuration

The configuration of this Lambda-function is really simple: the only dynamic parameter that needs to be passed as an ENV-variale is the name of the DynamoDB-table. This parameter is called `TABLE_NAME`. During the creation of the architecture using Amazon CloudFormation, certain names and tags are created dynamically, so it's not possible to hardcode the name of the table.