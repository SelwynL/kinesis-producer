package org.selwyn.kproducer

import org.selwyn.kproducer.codec.AvroCodec
import org.selwyn.kproducer.model.{AWSCredentials, AWSRegion, KProducerOutcome}
import org.selwyn.kproducer.producer.KinesisAvroProducer

import scala.collection.JavaConverters._
import scala.io.Source
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.{GetRecordsRequest, PutRecordResult, Record, StreamDescription}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

object Main {

  def main(args: Array[String]): Unit = {
    println("Running....")

    val streamName = "config-store-local"
    val partitionKey = "config-store-local-key"
    val endpoint = "http://localhost:7000"
    val region = AWSRegion.USEast1

    //val (lensConfig, avroCodec) : (GenericRecord, AvroCodec) = generateUser(1)
    val (lensConfig, avroCodec) : (GenericRecord, AvroCodec) = generateLensConfig()

    val credentials: AWSCredentials = AWSCredentials("default", "default")
    val eitherKinesisClient: Either[Throwable, AmazonKinesis] =
      KinesisAvroProducer.createKinesisClient(credentials, endpoint, region)

    val eitherKinesisProducer: Either[Throwable, KinesisAvroProducer] =
      eitherKinesisClient.map(client => new KinesisAvroProducer(client, avroCodec))

    // PRODUCE to Kinesis
    val produceResult: Either[Throwable, KProducerOutcome[PutRecordResult]] = eitherKinesisProducer.map(producer => {
      producer.produce(streamName, partitionKey, lensConfig)
    })
    println(s">>> PRODUCE RESULT: $produceResult")

    // CONSUME from Kinesis
    val consumeResult: Either[Throwable, List[Record]] = eitherKinesisClient.map(client => {
      val limit = 2000
      val shardIteratorType = "TRIM_HORIZON"

      val streamDescription: StreamDescription = client.describeStream(streamName).getStreamDescription
      println(s"STREAM DESCRIPTION: $streamDescription")

      streamDescription.getShards.asScala.toList.flatMap {
        shard => {
          println(s"SHARD: $shard")
          val shardIterator: String = client.getShardIterator(streamName, shard.getShardId, shardIteratorType).getShardIterator
          val recordRequest = new GetRecordsRequest().withShardIterator(shardIterator).withLimit(limit)
          client.getRecords(recordRequest).getRecords.asScala.toList
        }
      }
    })
    println(s">>> CONSUME RESULT: $consumeResult")
  }

  private def avroSchema(avroResourceFilename: String): Schema = {
    new Parser().parse(Source.fromURL(getClass.getResource(s"/${avroResourceFilename}")).mkString)
  }

  private def generateUser(id: Int): (GenericRecord, AvroCodec)= {
    val schema = avroSchema("user.avsc")
    val avroCodec: AvroCodec = new AvroCodec(schema)
    val user: GenericRecord = new GenericData.Record(schema)
    user.put("id", id)
    user.put("name", "john dow")
    user.put("email", "john.doe@gmail.com")

    (user, avroCodec)
  }

  private def generateLensConfig(): (GenericRecord, AvroCodec) = {
    val schema = avroSchema("lens_config.avsc")
    val avroCodec: AvroCodec = new AvroCodec(schema)

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

    (lensConfig, avroCodec)
  }
}
