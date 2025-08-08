package io.hiis.service.core.models.auth

import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.hiis.service.core.models.UserId

/** Created by Ludovic Temgoua Abanda (icemc) on 17/01/2023 */

trait Identity {
  def id: UserId
}

object Identity {
  implicit class StringToUserId(value: String) {
    def toIdentity: Identity = new Identity {
      override def id: UserId = UserId(value)
    }
  }

  implicit def encode: Encoder[Identity] = (a: Identity) =>
    Json.obj("userId" -> Json.fromString(a.id.value))
  implicit def decode: Decoder[Identity] = (c: HCursor) =>
    for {
      id <- c.downField("userId").as[String]
    } yield id.toIdentity
}
