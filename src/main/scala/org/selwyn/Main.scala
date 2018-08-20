package org.selwyn.kproducer

import java.net.URL

import org.selwyn.kproducer.codec.AvroCodec
import org.selwyn.kproducer.model.{AWSCredentials, AWSRegion, KProducerOutcome}
import org.selwyn.kproducer.producer.KinesisAvroProducer

import scala.io.Source
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.PutRecordResult
import com.concurrentthought.cla.{Args, Opt}
import io.circe.parser._
import org.apache.avro.Schema.Parser
import org.selwyn.kproducer.config.KProducerConfig

import scala.util.Try

object Main {

  def main(args: Array[String]): Unit = {

    val input = Opt.string(
      name = "inputFile",
      flags = Seq("-i", "--in", "--input"),
      help = "Path to the JSON input file.",
      requiredFlag = true
    )

    val schema = Opt.string(
      name = "schemaFile",
      flags = Seq("-s", "--schema"),
      help = "Path to the Avro schema file.",
      requiredFlag = true
    )

    val streamName = Opt.string(
      name = "streamName",
      flags = Seq("-n", "--stream-name"),
      help = "Name of the Kinesis stream.",
      requiredFlag = true
    )

    val partitionKey = Opt.string(
      name = "partitionKey",
      flags = Seq("-p", "--partition-key"),
      help = "The field in the Avro schema to use as a partition key. If not defined, defaults to the 'streamName'."
    )

    val endpoint = Opt.string(
      name = "endpoint",
      flags = Seq("-e", "--end-point"),
      default = Some("http://localhost:7000"),
      help = "Kinesis endpoint to use."
    )

    val region = Opt.string(
      name = "region",
      flags = Seq("-r", "--region"),
      default = Some("us-east-1"),
      help = "The AWS region for the Kinesis endpoint."
    )

    val initialArgs = Args("sbt run",
                           "Produce AVRO payload to Kinesis based on schema",
                           "",
                           Seq(input, schema, streamName, partitionKey, endpoint, region))

    run(initialArgs.process(args))
  }

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  protected def run(args: Args): Unit = {
    if (args.failures.isEmpty) {
      args.printValues()

      val payloadFilename: String = args.getOrElse("inputFile", "")
      val schemaFilename: String  = args.getOrElse("schemaFile", "")
      val streamName: String      = args.getOrElse("streamName", "")
      val endpoint: String        = args.getOrElse("endpoint", "http://localhost:7000")
      val regionString: String    = args.getOrElse("region", "us-east-1")
      val partitionKey: String    = args.getOrElse("partitionKey", streamName)

      val eitherConfig: Either[Throwable, KProducerConfig] = for {
        endpointUrl       <- Try(new URL(endpoint)).toEither
        region            <- Try(AWSRegion.withName(regionString)).toEither
        payloadFileString <- Try(Source.fromFile(payloadFilename).getLines().mkString).toEither
        schemaFileString  <- Try(Source.fromFile(schemaFilename).getLines().mkString).toEither
        jsonPayload       <- parse(payloadFileString)
        schema            <- Try(new Parser().parse(schemaFileString)).toEither
      } yield KProducerConfig(schema, jsonPayload, streamName, partitionKey, endpointUrl, region)
      println(s"CONFIG: $eitherConfig")

      val eitherResult: Either[Throwable, KProducerOutcome[PutRecordResult]] =
        for {
          config <- eitherConfig
          // TODO: make AWSCredentials CLI parameters
          client <- KinesisAvroProducer.createKinesisClient(AWSCredentials("default", "default"),
                                                            config.endpoint.toString,
                                                            config.region)
          result <- produce(client, config)
        } yield result
      println(s"RESULT: $eitherResult")
    }
  }

  protected def produce(client: AmazonKinesis,
                        config: KProducerConfig): Either[Throwable, KProducerOutcome[PutRecordResult]] = {
    val avroCodec: AvroCodec = new AvroCodec(config.schema)
    avroCodec
      .decodeJson(config.payload)
      .map(record => {
        println(s"GENERIC RECORD: $record")
        val producer: KinesisAvroProducer =
          new KinesisAvroProducer(client, avroCodec)
        println(s"PRODUCER: $producer")
        producer.produce(config.streamName, config.partitionKey, record)
      })
  }
}

//    // CONSUME from Kinesis
//    val consumeResult: Either[Throwable, List[Record]] = eitherKinesisClient.map(client => {
//      val limit = 2000
//      val shardIteratorType = "TRIM_HORIZON"
//
//      val streamDescription: StreamDescription = client.describeStream(streamName).getStreamDescription
//      println(s"STREAM DESCRIPTION: $streamDescription")
//
//      streamDescription.getShards.asScala.toList.flatMap {
//        shard => {
//          println(s"SHARD: $shard")
//          val shardIterator: String = client.getShardIterator(streamName, shard.getShardId, shardIteratorType).getShardIterator
//          val recordRequest = new GetRecordsRequest().withShardIterator(shardIterator).withLimit(limit)
//          client.getRecords(recordRequest).getRecords.asScala.toList
//        }
//      }
//    })
//    println(s">>> CONSUME RESULT: $consumeResult")
