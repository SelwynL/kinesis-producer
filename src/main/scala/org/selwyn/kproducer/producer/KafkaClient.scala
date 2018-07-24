package org.selwyn.kproducer.producer

import org.selwyn.kproducer.model.KProducerOutcome

import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata, KafkaProducer => KafkaProducerClient}
import scala.util.Try

abstract class KafkaClient(client: KafkaProducerClient[String, Array[Byte]]) {
  protected def produce(payload: ProducerRecord[String, Array[Byte]]): KProducerOutcome[RecordMetadata] = {
    // TODO: blocking call waiting on java.concurrent.Future.get()
    Try(client.send(payload).get()).toEither
          .fold[KProducerOutcome[RecordMetadata]](
          l => KProducerOutcome(payload = None, errorPayload = Some(l)),
          r => KProducerOutcome(payload = Some(r), errorPayload = None))
  }
}
