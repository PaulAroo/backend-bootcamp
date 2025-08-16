package io.hiis.service.core.models.misc

import io.circe.{ Decoder, Encoder }
import sttp.tapir.Schema
import zio.{ Task, ZIO }

import scala.util.Try

//TODO move such common code to a single library that could be used globally
object Languages extends Enumeration {
  type Language = Value

  val EN: Language = Value("EN")
  val FR: Language = Value("FR")
  val ES: Language = Value("ES")

  def fromString(value: String): Task[Language] =
    ZIO.fromTry(Try(Languages.withName(value.toUpperCase)))

  implicit val decoder: Decoder[Language] = Decoder.decodeEnumeration(Languages)
  implicit val encoder: Encoder[Language] = Encoder.encodeEnumeration(Languages)
  implicit val schema: Schema[Language]   = Schema.derivedEnumerationValue[Language]
}
