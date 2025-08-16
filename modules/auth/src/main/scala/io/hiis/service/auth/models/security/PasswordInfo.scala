package io.hiis.service.auth.models.security

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.hiis.service.core.models.UserId

import java.time.Instant

final case class PasswordInfo(
    id: UserId,
    hashes: List[String],
    lastUpdated: Instant
)

object PasswordInfo {
  implicit val encoder: Encoder[PasswordInfo] = deriveEncoder
  implicit val decoder: Decoder[PasswordInfo] = deriveDecoder
}
