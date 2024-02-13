import { Stack, StackProps, RemovalPolicy, CfnOutput, Duration, App, CfnParameter } from 'aws-cdk-lib';
import { Construct } from 'constructs';

import { aws_s3 as s3 } from 'aws-cdk-lib';
import { aws_ec2 as ec2 } from 'aws-cdk-lib';
import { aws_ecs as ecs } from 'aws-cdk-lib';
import { aws_kinesis as kinesis } from 'aws-cdk-lib';
import { aws_ecs_patterns as ecs_patterns } from 'aws-cdk-lib';
import { aws_dynamodb as dynamodb } from 'aws-cdk-lib';
import { aws_iam as iam } from 'aws-cdk-lib';
import { aws_lambda as lambda } from 'aws-cdk-lib';


import { RedisDB as redis_db } from 'cdk-redisdb';

export class ReactiveCdkAppStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const ExecutionRolePolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      resources: ['*'],
      actions: [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "logs:CreateLogGroup",
        "logs:DescribeLogStreams",
        "logs:CreateLogStream",
        "logs:DescribeLogGroups",
        "logs:PutLogEvents",
        "xray:PutTraceSegments",
        "xray:PutTelemetryRecords",
        "xray:GetSamplingRules",
        "xray:GetSamplingTargets",
        "xray:GetSamplingStatisticSummaries",
        'ssm:GetParameters'
      ]
    });

    const TaskRolePolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      resources: ['*'],
      actions: [
        "ecs:DescribeTasks",
        "ecs:ListTasks"
        ]
    });

    // Parameters
    const containerImage = new CfnParameter(this, 'containerImage', {
      type: 'String',
      description: 'The name of the container image to deploy',
    });

    const s3BucketName = new CfnParameter(this, 's3Bucket', {
      type: 'String',
      description: 'The name of the S3 bucket which stores the Lambda deployment packages'
    });

    // VPC
    const vpc = new ec2.Vpc(this, "reactive-system-vpc", {
      maxAzs: 3
    });

    // DynamoDB
    const ddbTable = new dynamodb.Table(this, "ProgramTable", {
      partitionKey: { name: "id", type: dynamodb.AttributeType.STRING, },
      sortKey: {name: "customer_id", type: dynamodb.AttributeType.NUMBER },
      tableName: "ProgramTable",
      readCapacity: 5,
      writeCapacity: 5,
      removalPolicy: RemovalPolicy.DESTROY, // NOT recommended for production code
    });

    // Elasticache
    
    const ecSecurityGroup = new ec2.SecurityGroup(this, 'elasticache-sg', {
      vpc: vpc,
      description: 'SecurityGroup associated with the ElastiCache Redis Cluster',
      allowAllOutbound: true,
    });
    
    ecSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(6379), 'Redis from anywhere');
    
    const redis = new redis_db(this, 'redisdb-repl-group', {
      nodes: 1,
      nodeType: 'cache.t4g.small',
      engineVersion: '7.1',
      replicas: 1,
      existingVpc: vpc,
      existingSecurityGroup: ecSecurityGroup
    })

    // Kinesis
    const eventStream = new kinesis.Stream(this, "reactive-system-event-stream", {
      retentionPeriod: Duration.hours(24),
      shardCount: 1,
      streamName: "reactive-system-event-stream"
    });

    const eventConsumer = new kinesis.CfnStreamConsumer(this, "reactive-system-event-stream-consumer", {
      consumerName: "reactive-system-event-stream-consumer",
      streamArn: eventStream.streamArn
    });

    const redisUpdateStream = new kinesis.Stream(this, "reactive-system-redis-update-stream", {
      retentionPeriod: Duration.hours(24),
      shardCount: 1,
      streamName: "reactive-system-redis-update-stream"
    });
    
    // ECS cluster
    const cluster = new ecs.Cluster(this, "reactive-system-cluster", {
      vpc: vpc
    });

    const logging = new ecs.AwsLogDriver({
      streamPrefix: "reactive-system"
    })

    const taskRole = new iam.Role(this, "reactive-system-taskRole", {
      roleName: "reactive-system-taskRole",
      assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com")
    });
    
    taskRole.addToPolicy(TaskRolePolicy);

    const taskDef = new ecs.FargateTaskDefinition(this, "reactive-system-taskdef", {
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.X86_64,
      },
      taskRole: taskRole,
      cpu: 1024,
      memoryLimitMiB: 2048
    });  

    taskDef.addToExecutionRolePolicy(ExecutionRolePolicy);

    const container = taskDef.addContainer("reactive-system", {
      image: ecs.ContainerImage.fromRegistry(containerImage.valueAsString),
      logging,
      environment: {
        "REDIS_HOST": redis.replicationGroup.attrConfigurationEndPointAddress,
        "REDIS_PORT": "6379",
        "EVENT_STREAM": eventStream.streamName
      }
    });

    container.addPortMappings({
      containerPort: 8080,
      hostPort: 8080,
      protocol: ecs.Protocol.TCP
    });

    const fargateService = new ecs_patterns.ApplicationLoadBalancedFargateService(this, "reactive-system-service", {
      cluster: cluster,
      taskDefinition: taskDef,
      publicLoadBalancer: true,
      desiredCount: 3,
      listenerPort: 8080
    });
    
    fargateService.targetGroup.configureHealthCheck({
      path: "/health/check",
      timeout: Duration.seconds(5),
      healthyThresholdCount: 2,
      unhealthyThresholdCount: 2
    });

    const scaling = fargateService.service.autoScaleTaskCount({ maxCapacity: 6 });
    scaling.scaleOnCpuUtilization("CpuScaling", {
      targetUtilizationPercent: 10,
      scaleInCooldown: Duration.seconds(60),
      scaleOutCooldown: Duration.seconds(60)
    });

    const bucket = s3.Bucket.fromBucketName(this, 'existingBucket', s3BucketName.valueAsString);

    // Lambda
    const kinesisConsumerFunction = new lambda.Function(this, "reactive-system-kinesis-consumer-function", {
      runtimeManagementMode: lambda.RuntimeManagementMode.AUTO,
      runtime: lambda.Runtime.PROVIDED_AL2023,
      functionName: "reactive-system-kinesis-consumer-function",
      handler: "kinesis-consumer",
      code: lambda.Code.fromBucket(bucket, "kinesis-consumer.zip"),
      memorySize: 128,
      timeout: Duration.seconds(30),
      environment: {
        "TABLE_NAME": ddbTable.tableName
      }
    });

    const redisUpdateFunction = new lambda.Function(this, "reactive-system-redis-update-function", {
      runtimeManagementMode: lambda.RuntimeManagementMode.AUTO,
      runtime: lambda.Runtime.PROVIDED_AL2023,
      functionName: "reactive-system-redis-update-function",
      handler: "redis-updater",
      code: lambda.Code.fromBucket(bucket, "redis-updater.zip"),
      memorySize: 128,
      timeout: Duration.seconds(30),
      vpc: vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      environment: {
        "REDIS_HOST": redis.replicationGroup.attrConfigurationEndPointAddress,
        "REDIS_PORT": "6379",
        "REDIS_CHANNEL": "channel1"
      }
    });

    const eventSourceMappingConsumer = new lambda.EventSourceMapping(this, "reactive-system-mapping-consumer", {
      target: kinesisConsumerFunction,
      eventSourceArn: eventConsumer.streamArn,
      startingPosition: lambda.StartingPosition.TRIM_HORIZON,
      enabled: true
    });

    const eventSourceMappingRedis = new lambda.EventSourceMapping(this, "reactive-system-mapping-redis", {
      target: redisUpdateFunction,
      eventSourceArn: redisUpdateStream.streamArn,
      startingPosition: lambda.StartingPosition.TRIM_HORIZON,
      enabled: true
    });
    
    eventStream.grantReadWrite(fargateService.taskDefinition.taskRole)
    eventStream.grantRead(kinesisConsumerFunction);
    redisUpdateStream.grantRead(redisUpdateFunction);
    
    ddbTable.grantReadWriteData(kinesisConsumerFunction);
    
    new CfnOutput(this, "LoadBalancerDNS", { value: fargateService.loadBalancer.loadBalancerDnsName });
  }
}
  
const app = new App();

new ReactiveCdkAppStack(app, "CdkappStack", {
  env: {
    region: "eu-west-1",
    account: process.env.CDK_DEFAULT_ACCOUNT,
  }
});
