package org.selwyn.kproducer.producer

import java.nio.ByteBuffer

import org.selwyn.kproducer.codec.AvroCodec
import org.selwyn.kproducer.model.{AWSCredentials, AWSRegion, KProducerOutcome}

import scala.util.Try
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.amazonaws.services.kinesis.model.{PutRecordRequest, PutRecordResult}
import org.apache.avro.generic.GenericRecord

class KinesisAvroProducer(client: AmazonKinesis, avroCodec: AvroCodec)
    extends KinesisClient(client)
    with KProducer[GenericRecord, PutRecordResult] {

  override def produce(topic: String, key: String, payload: GenericRecord): KProducerOutcome[PutRecordResult] =
    avroCodec
      .encodeBytes(payload)
      .fold[KProducerOutcome[PutRecordResult]](
        f => KProducerOutcome(payload = None, errorPayload = Some(f)),
        s =>
          produce(
            new PutRecordRequest()
              .withStreamName(topic)
              .withData(ByteBuffer.wrap(s))
              .withPartitionKey(key))
      )
}

object KinesisAvroProducer {

  def createKinesisClient(creds: AWSCredentials,
                          endpoint: String,
                          region: AWSRegion): Either[Throwable, AmazonKinesis] =
    // If the AWSCredentials.provider is a Right, Try to create AmazonKinesis, otherwise pass along the Left
    creds.provider
      .map(
        r =>
          Try(
            AmazonKinesisClientBuilder
              .standard()
              .withCredentials(r)
              .withEndpointConfiguration(new EndpointConfiguration(endpoint, region.entryName))
              .build()).toEither
      )
      .joinRight
}
