package org.selwyn.kproducer.codec

import org.apache.avro.generic.{GenericData, GenericRecord}
import org.scalatest._
import Matchers._
import io.circe.Json
import io.circe.parser._
import org.apache.avro.Schema
import org.selwyn.kproducer.TestObjects

class AvroCodecSpec extends FlatSpec {

  val userSchema: Schema = TestObjects.userSchema
  val userAvroCodec: AvroCodec = new AvroCodec(userSchema)

  val lensConfigSchema: Schema = TestObjects.lensSchema
  val lensAvroCodec: AvroCodec = new AvroCodec(lensConfigSchema)

  "AvroCodec" should "decode Json to GenericRecord" in {
    val json = parse("""
       {
          "id": 1234,
          "name": "john doe",
          "email" : {
            "string" : "john.doe@gmail.com"
          }
       }
      """).getOrElse(
      throw new RuntimeException("Unable parse user json string"))

    val expected: GenericRecord = new GenericData.Record(userSchema)
    expected.put("id", 1234)
    expected.put("name", "john doe")
    expected.put("email", "john.doe@gmail.com")

    val result = userAvroCodec.decodeJson(json)
    result.isRight should be(true)
    result.right.get should be(expected)
  }

  it should "fallback to default value for decode Json to GenericRecord" in {
    // missing email field should default to predefined value
    val json = parse("""
       {
          "id": 1234,
          "name": "john doe"
       }
      """).getOrElse(throw new RuntimeException("Unable parse json string"))

    val expected: GenericRecord = new GenericData.Record(userSchema)
    expected.put("id", 1234)
    expected.put("name", "john doe")
    expected.put("email", null)

    val result = userAvroCodec.decodeJson(json)
    result.isRight should be(true)
    result.right.get should be(expected)
  }

  it should "encode GenericRecord to Json" in {
    val userRecord: GenericRecord = new GenericData.Record(userSchema)
    userRecord.put("id", 1234)
    userRecord.put("name", "john doe")
    userRecord.put("email", "john.doe@gmail.com")

    val expected = parse("""
       {
          "id": 1234,
          "name": "john doe",
          "email" : {
            "string" : "john.doe@gmail.com"
          }
       }
       """)

    val encoded: Either[Throwable, Json] =
      userAvroCodec.encodeJson(userRecord, true)
    encoded should be(expected)
  }

  it should "fallback to default value for encode GenericRecord to Json" in {
    // missing email field should default to predefined value
    val userRecord: GenericRecord = new GenericData.Record(userSchema)
    userRecord.put("id", 1234)
    userRecord.put("name", "john doe")

    val expected = parse("""
       {
          "id": 1234,
          "name": "john doe",
          "email": null
       }
       """)

    val encoded: Either[Throwable, Json] =
      userAvroCodec.encodeJson(userRecord, true)
    encoded should be(expected)
  }

  it should "encode complex GenericRecord to Json" in {
    import collection.JavaConverters._

    val lensSchema: Schema = lensConfigSchema.getField("lens").schema

    val fieldSchema: Schema =
      lensSchema.getField("fields").schema.getElementType
    val fieldTypeSchema: Schema = fieldSchema.getField("fieldType").schema
    val indexTypeSchema: Schema = fieldSchema.getField("indexType").schema
    val sortOrderSchema: Schema = fieldSchema.getField("sortOrder").schema

    val field: GenericRecord = new GenericData.Record(fieldSchema)
    field.put("name", "employeeId")
    field.put("indexType",
              new GenericData.EnumSymbol(indexTypeSchema, "LOOKUP_INDEX"))
    field.put("fieldType",
              new GenericData.EnumSymbol(fieldTypeSchema, "STRING_FIELD"))
    field.put("description", "Employee ID")
    field.put("required", true)
    field.put("sortOrder",
              new GenericData.EnumSymbol(sortOrderSchema, "IGNORE_SORT_ORDER"))

    val lens: GenericRecord = new GenericData.Record(lensSchema)
    lens.put("name", "C1_PHONEBOOK_Employee")
    lens.put("version", "0.1")
    lens.put("fields", List(field).asJava)

    val streamReader: Map[String, String] = Map(
      "auto.offset.reset" -> "latest",
      "topic.subscriptions" -> "test-employee",
      "group.id" -> "test-employee-group",
      "bootstrap.servers" -> "localhost:9093",
      "schema.registry.url" -> "http://localhost:8081"
    )

    val operationalStoreSchema: Schema =
      lensConfigSchema.getField("operationalStore").schema
    val storeTypeSchema: Schema =
      operationalStoreSchema.getField("storeType").schema
    val regionSchema: Schema = operationalStoreSchema.getField("region").schema
    val operationalStore: GenericRecord =
      new GenericData.Record(operationalStoreSchema)
    operationalStore.put("storeType",
                         new GenericData.EnumSymbol(storeTypeSchema, "REST"))
    operationalStore.put("region",
                         new GenericData.EnumSymbol(regionSchema, "US_EAST_1"))
    operationalStore.put("connectionString", "http://localhost:6379")

    val lensConfigRecord: GenericRecord =
      new GenericData.Record(lensConfigSchema)
    lensConfigRecord.put("lensKey", "someKey")
    lensConfigRecord.put("lens", lens)
    lensConfigRecord.put("streamReader", streamReader.asJava)
    lensConfigRecord.put("operationalStore", operationalStore)

    val expected = parse(
      """
        |{
        |  "lensKey" : "someKey",
        |  "lens" : {
        |    "name" : "C1_PHONEBOOK_Employee",
        |    "version" : "0.1",
        |    "fields" : [
        |      {
        |        "name" : "employeeId",
        |        "indexType" : "LOOKUP_INDEX",
        |        "fieldType" : "STRING_FIELD",
        |        "description" : "Employee ID",
        |        "required" : true,
        |        "sortOrder" : "IGNORE_SORT_ORDER"
        |      }
        |    ],
        |    "includeRecord" : null
        |  },
        |  "streamReader" : {
        |    "auto.offset.reset" : "latest",
        |    "topic.subscriptions" : "test-employee",
        |    "group.id" : "test-employee-group",
        |    "bootstrap.servers" : "localhost:9093",
        |    "schema.registry.url" : "http://localhost:8081"
        |  },
        |  "operationalStore" : {
        |    "storeType" : "REST",
        |    "region" : "US_EAST_1",
        |    "connectionString" : "http://localhost:6379",
        |    "clientId" : null,
        |    "clientSecret" : null
        |  }
        |}
      """.stripMargin
    )

    val encoded: Either[Throwable, Json] =
      lensAvroCodec.encodeJson(lensConfigRecord, true)
    encoded should be(expected)
  }
}
