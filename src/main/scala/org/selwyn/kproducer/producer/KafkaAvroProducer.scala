package org.selwyn.kproducer.producer

import org.selwyn.kproducer.codec.AvroCodec
import org.selwyn.kproducer.model.KProducerOutcome

import java.util.Properties
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata, KafkaProducer => KafkaProducerClient}
import scala.util.Try

class KafkaAvroProducer(client: KafkaProducerClient[String, Array[Byte]], avroCodec: AvroCodec)
    extends KafkaClient(client)
    with KProducer[GenericRecord, RecordMetadata] {

  override def produce(topic: String, key: String, payload: GenericRecord): KProducerOutcome[RecordMetadata] =
    avroCodec
      .encodeBytes(payload)
      .fold(
        f => KProducerOutcome(payload = None, errorPayload = Some(f)),
        s => produce(new ProducerRecord(topic, key, s))
      )
}

object KafkaAvroProducer {

  def createKafkaClient(props: Properties): Either[Throwable, KafkaProducerClient[String, Array[Byte]]] =
    Try(new KafkaProducerClient[String, Array[Byte]](props)).toEither

}
