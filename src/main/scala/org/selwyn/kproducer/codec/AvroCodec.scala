package org.selwyn.kproducer.codec

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io._
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import io.circe.parser._
import java.io.ByteArrayOutputStream

import io.circe.Json
import org.selwyn.kproducer.avro.ExtendedJsonDecoder

import scala.util.Try

class AvroCodec(schema: Schema) {
  val decoderFactory: DecoderFactory = DecoderFactory.get()

  val avroReader: DatumReader[GenericRecord] =
    new SpecificDatumReader[GenericRecord](schema)
  val encoderFactory: EncoderFactory = EncoderFactory.get()

  val avroWriter: DatumWriter[GenericRecord] =
    new SpecificDatumWriter[GenericRecord](schema)

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def decodeBytes(bytes: Array[Byte]): Either[Throwable, GenericRecord] =
    Try {
      val decoder = decoderFactory.binaryDecoder(bytes, null)
      avroReader.read(null, decoder)
    }.toEither

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def encodeBytes(record: GenericRecord): Either[Throwable, Array[Byte]] =
    Try {
      val stream        = new ByteArrayOutputStream
      val binaryEncoder = encoderFactory.binaryEncoder(stream, null)
      avroWriter.write(record, binaryEncoder)
      binaryEncoder.flush()
      stream.toByteArray
    }.toEither

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def decodeJson(json: Json): Either[Throwable, GenericRecord] =
    Try {
      val decoder = new ExtendedJsonDecoder(schema, json.toString())
      avroReader.setSchema(schema)
      avroReader.read(null, decoder)
    }.toEither

  def encodeJson(record: GenericRecord, pretty: Boolean): Either[Throwable, Json] = {
    val stream = new ByteArrayOutputStream
    Try {
      val encoder = encoderFactory.jsonEncoder(schema, stream, pretty)
      avroWriter.write(record, encoder)
      encoder.flush()
      stream.flush()
    }.map(_ => parse(new String(stream.toByteArray))).toEither.joinRight
  }
}
