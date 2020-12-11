  # Go parameters
    GOCMD=go
    GOBUILD=$(GOCMD) build
    GOCLEAN=$(GOCMD) clean
    GOTEST=$(GOCMD) test
    GOGET=$(GOCMD) get
    BINARY_NAME=kinesis-consumer
    BINARY_UNIX=$(BINARY_NAME)
    
    all: test build
    build: 
			$(GOBUILD) -o $(BINARY_NAME) -v
    test: 
			$(GOTEST) -v ./...
    clean: 
			$(GOCLEAN)
			rm -f $(BINARY_NAME)
			rm -f $(BINARY_UNIX)
			rm -f $(BINARY_UNIX).zip
    run:
			$(GOBUILD) -o $(BINARY_NAME) -v ./...
			./$(BINARY_NAME)
    deps:
			$(GOGET) github.com/aws/aws-lambda-go/events
			$(GOGET) github.com/aws/aws-lambda-go/lambda
			$(GOGET) github.com/aws/aws-lambda-go/lambda/messages
			$(GOGET) github.com/aws/aws-lambda-go/lambdacontext
			$(GOGET) github.com/aws/aws-sdk-go/aws
			$(GOGET) github.com/golang/protobuf/proto
			$(GOGET) github.com/nu7hatch/gouuid
			$(GOGET) github.com/stretchr/testify/assert
			$(GOGET) github.com/jmespath/go-jmespath

    # Cross compilation
    build:
			CGO_ENABLED=0 GOOS=linux GOARCH=amd64 $(GOBUILD) -o $(BINARY_UNIX) cmd/*.go
			chmod +x ./$(BINARY_UNIX)
			zip -j ./$(BINARY_UNIX).zip ./$(BINARY_UNIX)
