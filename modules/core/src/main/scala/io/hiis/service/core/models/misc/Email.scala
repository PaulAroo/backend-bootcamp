package io.hiis.service.core.models.misc

import io.circe.{ Decoder, Encoder }
import org.apache.commons.validator.routines.EmailValidator
import sttp.tapir.Schema

final case class Email(value: String) {
  require(EmailValidator.getInstance().isValid(value), "Invalid email address")
}

object Email {
  implicit val encoder: Encoder[Email] = Encoder[String].contramap[Email](_.value)
  implicit val decoder: Decoder[Email] = Decoder[String].map(Email.apply)

  implicit val schemaSttp: Schema[Email] =
    Schema.schemaForString.map(value => Some(Email(value)))(_.value)
}
