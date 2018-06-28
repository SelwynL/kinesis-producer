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

object Main {

  def main(args: Array[String]): Unit = {
    println("Running....")

    val streamName = "config-store-local"
    val partitionKey = "config-store-local-key"
    val endpoint = "http://localhost:7000"
    val region = "us-east-1"

    val client: Either[Throwable, AmazonKinesis] = for {
      provider <- getProvider()
      client = createKinesisClient(provider, endpoint, region)
      exists <- if (streamExists(client, streamName)) Right(true)
      else
        Left(
          new IllegalArgumentException(s"Unable to connect to '$streamName'"))
    } yield client

    // val avroFilename = "user.avsc"
    val avroFilename = "lens_config.avsc"

    val schema: Schema = new Parser()
      .parse(Source.fromURL(getClass.getResource(s"/${avroFilename}")).mkString)
    val avro: AvroCodec = new AvroCodec(schema)

    // val user: GenericRecord = new GenericData.Record(schema)
    //     user.put("id", i)
    //     user.put("name", "john dow")
    //     user.put("email", "john.doe@gmail.com")

    val lensSchema: Schema = schema.getField("lens").schema
    val fieldSchema: Schema =
      lensSchema.getField("fields").schema.getElementType
    val fieldTypeSchema: Schema = fieldSchema.getField("fieldType").schema
    val indexTypeSchema: Schema = fieldSchema.getField("indexType").schema
    val sortOrderSchema: Schema = fieldSchema.getField("sortOrder").schema
    val operationalStoreSchema: Schema =
      schema.getField("operationalStore").schema
    val storeTypeSchema: Schema =
      operationalStoreSchema.getField("storeType").schema

    val lens: GenericRecord = new GenericData.Record(lensSchema)
    val field: GenericRecord = new GenericData.Record(fieldSchema)
    field.put("name", "employeeId")
    field.put("indexType",
              new GenericData.EnumSymbol(indexTypeSchema, "LookupIndex"))
    field.put("fieldType",
              new GenericData.EnumSymbol(fieldTypeSchema, "StringField"))
    field.put("description", "Employee ID")
    field.put("required", true)
    field.put("sortOrder",
              new GenericData.EnumSymbol(sortOrderSchema, "IgnoreSortOrder"))
    lens.put("name", "C1_PHONEBOOK_Employee")
    lens.put("version", "0.1")
    lens.put("fields", List(field).asJava)

    val streamReader: Map[String, String] = Map(
      "group.id" -> "stream-reader-local",
      "topic.subscriptions" -> "test-employee",
      "bootstrap.servers" -> "localhost:9093",
      "schema.registry.url" -> "http://localhost:8081",
      "auto.offset.reset" -> "latest"
    )

    val operationalStore: GenericRecord =
      new GenericData.Record(operationalStoreSchema)
    operationalStore.put("storeType",
                         new GenericData.EnumSymbol(storeTypeSchema, "redis"))
    operationalStore.put("region", "us-east-1")
    operationalStore.put("connectionString", "http://localhost:6379")

    val lensConfig: GenericRecord = new GenericData.Record(schema)
    lensConfig.put("lens", lens)
    lensConfig.put("streamReader", streamReader.asJava)
    lensConfig.put("operationalStore", operationalStore)

    val complete = client.map(c => {
      val max = 1
      for (i <- 1 to max) {
        // PRODUCE
        println(s"Producing to stream: ${lensConfig}")
        //val data: ByteBuffer = ByteBuffer.wrap(avro.encode(user))
        val data: ByteBuffer = ByteBuffer.wrap(avro.encode(lensConfig))
        val result = c.putRecord(streamName, data, partitionKey)
        println(s"RESULT: ${result}")
      }

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
      "SUCCESS"
    })

    println(s"COMPLETED: ${complete}")
  }

  private def serialize(string: String) = string.getBytes;

  private def getProvider(): Either[Throwable, AWSCredentialsProvider] = {
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
