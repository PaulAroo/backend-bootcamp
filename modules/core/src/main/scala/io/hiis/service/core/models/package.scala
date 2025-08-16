package io.hiis.service.core

import io.circe.{ Decoder, Encoder }
import sttp.tapir.Schema

import java.net.URI

package object models {
  final case class UserId(value: String) extends AnyVal

  object UserId {
    implicit val encoder: Encoder[UserId] = Encoder[String].contramap[UserId](_.value)
    implicit val decoder: Decoder[UserId] = Decoder[String].map(UserId.apply)

    implicit val schemaSttp: Schema[UserId] =
      Schema.schemaForString.map(value => Some(UserId(value)))(_.value)
  }

  object Schemas {
    implicit val uriSchema: Schema[URI] =
      Schema.schemaForString
        .map(value => Some(new URI(value)))(_.toString)
        .description(
          "URL. Example: https://example.com"
        )
        .encodedExample(
          new URI("https://example.com")
        )
  }

}
