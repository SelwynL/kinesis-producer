package org.selwyn.kproducer.config

import java.net.URL

import io.circe.Json
import org.apache.avro.Schema
import org.selwyn.kproducer.model.AWSRegion

case class KProducerConfig(schema: Schema,
                           payload: Json,
                           streamName: String,
                           partitionKey: String,
                           endpoint: URL,
                           region: AWSRegion)
