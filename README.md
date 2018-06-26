# kproducer

## Run
Running against Kinesalite requires disable of `CBOR` authentication
```
AWS_CBOR_DISABLE=true sbt run
```

## Kinesalite
Use NPM to install Kinesalite (Mock AWS Kinesis for local development)
```
> brew install node
> npm install -g kinesalite
```

Start on port `7000`
```
> kinesalite --port 7000
```

Install AWS CLI
```
> brew install awscli
```

Use AWS CLI to communicate with Kinesalite, ensure you run `aws configure` or create a `~/.aws/credentials` file to set fake credentials
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
