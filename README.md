# kproducer
Avro/Json message producer for Kinesis and Kafka

## Getting started
In order to run this application locally you'll need to bootstrap the development environment

### 1. SSL
To secure Kafka with SSL you'll need a keystore and trustore. If you already have these, add them to the appropriate `ssl/keystore` and `ssl/truststore` directories. If not, generate them by executing the provided script, and follow along with the prompts:
```
> ./ssl/generate-kafka-ssl.sh
```
To configure file locations and file names used by `docker-compose`, adjust the `.env` file.

### 2. Docker Compose
When SSL files are in place, create the required services using Docker Compose. This starts Kinesalite and Kafka. For Docker installation see [Docker](#docker-setup) section
```
> docker-compose up
```

### 3. Run Application
Start the application to generate some events. Running against Kinesalite (Kinesis) locally requires disable of `CBOR` authentication.
```
> AWS_CBOR_DISABLE=true sbt run
```

## Docker Setup
The local development environment is instantiated though `docker-compose`, this requires the install of Docker. See this [StackOverflow](https://stackoverflow.com/a/43365425) post for installing Docker on Mac with brew

## Kinesalite Setup with AWS CLI
Localstack is used to mock a Kinesis environment locally. See Localstack [documentation](https://github.com/localstack/localstack) for more info on other AWS services.

To interact with Kinesalite, install AWS CLI
```
> brew install awscli
```

Run `aws configure` or create a `~/.aws/credentials` file to set fake credentials
```
[default]
aws_access_key_id = FAKE
aws_secret_access_key = FAKE
region = us-east-1
```

Create a stream in Kinesalite
```
> aws --endpoint-url 'http://localhost:7000' kinesis create-stream --stream-name logs --shard-count 1
> aws --endpoint-url 'http://localhost:7000' kinesis list-streams
```

Read from the stream. Get the `ShardId` using `describe-stream`. Then use it to `get-shard-iterator`. Use the `ShardIterator` returned to `get-records`
```
> aws --endpoint-url 'http://localhost:7000' kinesis describe-stream --stream-name logs
> aws --endpoint-url 'http://localhost:7000' kinesis get-shard-iterator --stream-name logs --shard-id shardId-000000000000 --shard-iterator-type TRIM_HORIZON
> aws --endpoint-url 'http://localhost:7000' kinesis get-records --shard-iterator AAAAAAAAAAHYwGk5KXvs76kK7t9zG207lzzbBPYRirS0xBO1UEiYV3ehOQQDRetKtzIKnrFmNdYiizsRT4TMmZoz4YB7wVtd5ABc3Q9yBkX6SETnhxr0YqZO7GfFQ1jtmrj1On5LhBzQhGpktprCz3er8A+n38smV53M0Q2QROQwsKMQj7F0k5NocUouCjY/FMw20aV0w3Y=
```

See more Kinesis CLI documentation here: https://docs.aws.amazon.com/cli/latest/reference/kinesis/index.html
