package org.selwyn.kproducer

import org.selwyn.kproducer.codec.AvroCodec

import scala.collection.JavaConverters._
import scala.io.Source
import java.nio.ByteBuffer

import com.amazonaws.auth._
import com.amazonaws.services.kinesis.model._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{
  AmazonKinesis,
  AmazonKinesisClientBuilder
}

import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

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

    val avroFilename = "user.avsc"
    val schema: Schema = new Parser()
      .parse(Source.fromURL(getClass.getResource(s"/${avroFilename}")).mkString)
    val avro: AvroCodec = new AvroCodec(schema)

    val complete = client.map(c => {

      // PRODUCE
      val user: GenericRecord = new GenericData.Record(schema)
      user.put("id", 1234)
      user.put("name", "john dow")
      user.put("email", "john.doe@gmail.com")

      val data: ByteBuffer = ByteBuffer.wrap(avro.encode(user))
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
