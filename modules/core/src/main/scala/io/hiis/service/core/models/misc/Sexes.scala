package io.hiis.service.core.models.misc

import io.circe.{ Decoder, Encoder }
import sttp.tapir.Schema

object Sexes extends Enumeration {
  type Sex = Value

  val MALE   = Value("Male")
  val FEMALE = Value("Female")

  implicit val decoder: Decoder[Sex] = Decoder.decodeEnumeration(Sexes)
  implicit val encoder: Encoder[Sex] = Encoder.encodeEnumeration(Sexes)
  implicit val schema: Schema[Sex]   = Schema.derivedEnumerationValue[Sex]
}
