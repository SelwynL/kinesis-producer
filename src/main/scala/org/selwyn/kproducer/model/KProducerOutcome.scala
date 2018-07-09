package org.selwyn.kproducer.model

case class KProducerOutcome[P](payload: Option[P], errorPayload: Option[Throwable]) {
  val success: Boolean = payload.isDefined
}
