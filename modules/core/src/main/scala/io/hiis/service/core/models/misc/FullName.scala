package io.hiis.service.core.models.misc

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

final case class FullName(
    firstName: String,
    middleName: Option[String],
    lastName: String
)

object FullName {
  implicit val decoder: Decoder[FullName] = deriveDecoder
  implicit val encoder: Encoder[FullName] = deriveEncoder
}
