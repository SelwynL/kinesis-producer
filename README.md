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

### 3. Create Kinesis Stream
Create a stream in Kinesalite as a target to produce/consume data from
```
> aws --endpoint-url 'http://localhost:7000' kinesis create-stream --stream-name config-store-local --shard-count 1
```

### 4. Run Application
To run without building a fat jar use `sbt`. This will bring you into an SBT shell (shows `kproducer>`) where you can run further SBT commands. Running against Kinesalite (Kinesis) locally requires disable of `CBOR` authentication, so set that inline.

```
> AWS_CBOR_DISABLE=true sbt
kproducer> 
```

The CLI parameters can be displayed by running `run --help`:
```
kproducer> run --help

Usage: sbt run [options]
Produce AVRO payload to Kinesis based on schema
Where the supported options are the following:

   -i | --in | --input  inputFile         Path to the JSON input file.
   -s | --schema  schemaFile              Path to the Avro schema file.
   -n | --stream-name  streamName         Name of the Kinesis stream.
  [-p | --partition-key  partitionKey]    The field in the Avro schema to use as a partition key. If
                                          not defined, defaults to the 'streamName'.
  [-e | --end-point  endpoint]            Kinesis endpoint to use.
                                          (default: http://localhost:7000)
  [-r | --region  region]                 The AWS region for the Kinesis endpoint.
                                          (default: us-east-1)
  [-h | --h | --help]                     Show this help message.
  [remaining]                             All remaining arguments that aren't associated with flags.

You can also use --foo=bar syntax. Arguments shown in [...] are optional. All others are required.
```

Optionally you can build a fat jar and run CLI commands against it:
```
> sbt clean assembly
```

Running `--help` against the fatjar to show usage:
```
> java -jar target/scala-2.12/kproducer-0.0.1.jar --help

Usage: sbt run [options]
Produce AVRO payload to Kinesis based on schema
Where the supported options are the following:

   -i | --in | --input  inputFile         Path to the JSON input file.
   -s | --schema  schemaFile              Path to the Avro schema file.
   -n | --stream-name  streamName         Name of the Kinesis stream.
  [-p | --partition-key  partitionKey]    The field in the Avro schema to use as a partition key. If
                                          not defined, defaults to the 'streamName'.
  [-e | --end-point  endpoint]            Kinesis endpoint to use.
                                          (default: http://localhost:7000)
  [-r | --region  region]                 The AWS region for the Kinesis endpoint.
                                          (default: us-east-1)
  [-h | --h | --help]                     Show this help message.
  [remaining]                             All remaining arguments that aren't associated with flags.

You can also use --foo=bar syntax. Arguments shown in [...] are optional. All others are required.
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
> aws --endpoint-url 'http://localhost:7000' kinesis create-stream --stream-name config-store-local --shard-count 1
> aws --endpoint-url 'http://localhost:7000' kinesis list-streams
```

Read from the stream. Get the `ShardId` using `describe-stream`. Then use it to `get-shard-iterator`. Use the `ShardIterator` returned to `get-records`
```
> aws --endpoint-url 'http://localhost:7000' kinesis describe-stream --stream-name config-store-local
> aws --endpoint-url 'http://localhost:7000' kinesis get-shard-iterator --stream-name config-store-local --shard-id shardId-000000000000 --shard-iterator-type TRIM_HORIZON
> aws --endpoint-url 'http://localhost:7000' kinesis get-records --shard-iterator AAAAAAAAAAHYwGk5KXvs76kK7t9zG207lzzbBPYRirS0xBO1UEiYV3ehOQQDRetKtzIKnrFmNdYiizsRT4TMmZoz4YB7wVtd5ABc3Q9yBkX6SETnhxr0YqZO7GfFQ1jtmrj1On5LhBzQhGpktprCz3er8A+n38smV53M0Q2QROQwsKMQj7F0k5NocUouCjY/FMw20aV0w3Y=
```

See more Kinesis CLI documentation here: https://docs.aws.amazon.com/cli/latest/reference/kinesis/index.html
