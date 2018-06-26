package org.selwyn.kproducer

import scala.collection.JavaConverters._
import java.nio.ByteBuffer

import com.amazonaws.auth._
import com.amazonaws.services.kinesis.model._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{
  AmazonKinesis,
  AmazonKinesisClientBuilder
}

case class AwsConfig(accessKey: String, secretKey: String)

object Main {

  def main(args: Array[String]): Unit = {
    println("Running....")

    val awsConfig = AwsConfig("default", "default")
    val streamName = "logs"
    val partitionKey = "logskey"
    val endpoint = "http://localhost:7000"
    val region = "us-east-1"

    val client: Either[Throwable, AmazonKinesis] = for {
      provider <- getProvider(awsConfig)
      client = createKinesisClient(provider, endpoint, region)
      exists <- if (streamExists(client, streamName)) Right(true)
      else
        Left(
          new IllegalArgumentException(s"Unable to connect to '$streamName'"))
    } yield client

    val complete = client.map(c => {

      // PRODUCE
      val json: String = "{\"name\":\"johndoe\"}"
      val data: ByteBuffer = ByteBuffer.wrap(serialize(json))
      c.putRecord(streamName, data, partitionKey)

      // CONSUME
      // val shardId = ""
      // val limit = 2000
      // val shardIteratorType = "TRIM_HORIZON"

      // val streamDescription: StreamDescription = c.describeStream(streamName).getStreamDescription()
      // println("STREAM DESCRIPTION: " + streamDescription)

      // val result: List[Record] = streamDescription.getShards().asScala.toList.map {
      //   shard => {
      //     println("SHARD: " + shard)
      //     val shardIterator: String = c.getShardIterator(streamName, shard.getShardId(), shardIteratorType).getShardIterator()
      //     val recordRequest = new GetRecordsRequest().withShardIterator(shardIterator).withLimit(limit)
      //     c.getRecords(recordRequest).getRecords().asScala.toList
      //   }
      // }.flatten

      // println("RESULTS: " + result)
    })

    println(s"COMPLETED: ${complete}")
  }

  private def serialize(string: String) = string.getBytes;

  private def getProvider(
      awsConfig: AwsConfig): Either[Throwable, AWSCredentialsProvider] = {
    Right(new DefaultAWSCredentialsProviderChain())
  }

  private def createKinesisClient(provider: AWSCredentialsProvider,
                                  endpoint: String,
                                  region: String): AmazonKinesis =
    AmazonKinesisClientBuilder
      .standard()
      .withCredentials(provider)
      .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
      .build()

  private def streamExists(client: AmazonKinesis, name: String): Boolean =
    try {
      val describeStreamResult = client.describeStream(name)
      val status = describeStreamResult.getStreamDescription.getStreamStatus
      status == "ACTIVE" || status == "UPDATING"
    } catch {
      case rnfe: ResourceNotFoundException => false
    }
}
