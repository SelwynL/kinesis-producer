package org.selwyn.kproducer.codec

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.{
  DatumReader,
  DatumWriter,
  DecoderFactory,
  EncoderFactory
}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import java.io.ByteArrayOutputStream

class AvroCodec(schema: Schema) {
  val decoderFactory: DecoderFactory = DecoderFactory.get()
  val avroReader: DatumReader[GenericRecord] =
    new SpecificDatumReader[GenericRecord](schema)
  val encoderFactory: EncoderFactory = EncoderFactory.get()
  val avroWriter: DatumWriter[GenericRecord] =
    new SpecificDatumWriter[GenericRecord](schema)
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def decode(bytes: Array[Byte]): GenericRecord = {
    val decoder = decoderFactory.binaryDecoder(bytes, null)
    avroReader.read(null, decoder)
  }
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def encode(record: GenericRecord): Array[Byte] = {
    val stream = new ByteArrayOutputStream
    val binaryEncoder =
      encoderFactory.binaryEncoder(stream, null)
    avroWriter.write(record, binaryEncoder)
    binaryEncoder.flush()
    stream.toByteArray
  }
}
