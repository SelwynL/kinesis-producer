package org.selwyn.kproducer.model

import enumeratum._
import enumeratum.EnumEntry._

import scala.collection.immutable

sealed abstract class AWSRegion(override val entryName: String)
    extends EnumEntry
    with Hyphencase
    with Snakecase
    with Camelcase

object AWSRegion extends Enum[AWSRegion] {
  val values: immutable.IndexedSeq[AWSRegion] = findValues

  case object USEast1 extends AWSRegion("us-east-1")
  case object USEast2 extends AWSRegion("us-east-2")
  case object USWest1 extends AWSRegion("us-west-1")
  case object USWest2 extends AWSRegion("us-west-2")
}
