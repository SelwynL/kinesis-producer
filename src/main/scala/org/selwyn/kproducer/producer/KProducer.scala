package org.selwyn.kproducer.producer

import org.selwyn.kproducer.model.KProducerOutcome

trait KProducer[P, R] {
  def produce(topic: String, key: String, payload: P): KProducerOutcome[R]
}
