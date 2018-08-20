package org.selwyn.kproducer

import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

import scala.io.Source

object TestObjects {

  val userSchema: Schema = schemaFromResource("user-schema.avsc")
  val lensSchema: Schema = schemaFromResource("lens-config-schema.avsc")

  def schemaFromResource(filename: String): Schema = {
    new Parser()
      .parse(Source.fromURL(getClass.getResource(s"/${filename}")).mkString)
  }
}
