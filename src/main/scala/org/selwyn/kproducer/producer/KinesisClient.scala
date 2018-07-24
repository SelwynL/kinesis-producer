package org.selwyn.kproducer.producer

import org.selwyn.kproducer.model.KProducerOutcome
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.{PutRecordRequest, PutRecordResult}

import scala.util.Try

abstract class KinesisClient(client: AmazonKinesis) {
  protected def produce(payload: PutRecordRequest): KProducerOutcome[PutRecordResult] =
    Try(client.putRecord(payload)).toEither
      .fold[KProducerOutcome[PutRecordResult]](
      l => KProducerOutcome(payload = None, errorPayload = Some(l)),
      r => KProducerOutcome(payload = Some(r), errorPayload = None))
}
