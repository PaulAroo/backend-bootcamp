package io.hiis.service.core.models.auth

import io.circe.syntax.EncoderOps
import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.hiis.service.core.models.{ Schemas, UserId }
import io.hiis.service.core.models.misc.Sexes.Sex
import io.hiis.service.core.models.misc.{ Email, FullName, PhoneNumber, Sexes }
import sttp.tapir.Schema.SName
import sttp.tapir.SchemaType.{ SProduct, SProductField }
import sttp.tapir.{ FieldName, Schema }

import java.net.URI

/** Created by Ludovic Temgoua Abanda (icemc) on 17/01/2023 */

trait Identity {
  def id: UserId
  def phone: Option[PhoneNumber]
  def email: Email
}

object Identity {

  implicit def encode: Encoder[Identity] = (a: Identity) =>
    Json.obj(
      "userId" -> Json.fromString(a.id.value),
      "phone"  -> a.phone.asJson,
      "email"  -> a.email.asJson
    )

  implicit def decode: Decoder[Identity] = (c: HCursor) =>
    for {
      id_    <- c.downField("userId").as[String]
      phone_ <- c.downField("phone").as[Option[PhoneNumber]]
      email_ <- c.downField("email").as[Email]
    } yield new Identity {
      override def id: UserId = UserId(id_)

      override def phone: Option[PhoneNumber] = phone_

      override def email: Email = email_
    }
}

trait SimpleIdentity extends Identity {
  def name: Option[FullName]
  def sex: Option[Sex]
  def image: Option[URI]
}

object SimpleIdentity {
  implicit val uriSchemaSttp: Schema[URI] =
    Schema.schemaForString.map(value => Some(new URI(value)))(_.toString)

  implicit def encode: Encoder[SimpleIdentity] = (a: SimpleIdentity) =>
    Json.obj(
      "userId" -> a.id.asJson,
      "phone"  -> a.phone.asJson,
      "email"  -> a.email.asJson,
      "name"   -> a.name.asJson,
      "sex"    -> a.sex.asJson,
      "image"  -> a.image.asJson
    )

  implicit def decode: Decoder[SimpleIdentity] = (c: HCursor) =>
    for {
      id_    <- c.downField("userId").as[UserId]
      phone_ <- c.downField("phone").as[Option[PhoneNumber]]
      email_ <- c.downField("email").as[Email]
      name_  <- c.downField("name").as[Option[FullName]]
      sex_   <- c.downField("sex").as[Option[Sex]]
      image_ <- c.downField("image").as[Option[URI]]
    } yield new SimpleIdentity {

      override def phone: Option[PhoneNumber] = phone_

      override def id: UserId = id_

      override def email: Email = email_

      override def name: Option[FullName] = name_

      override def sex: Option[Sex] = sex_

      override def image: Option[URI] = image_
    }

  implicit val schema: Schema[SimpleIdentity] = Schema(
    schemaType = SProduct[SimpleIdentity](
      List(
        SProductField[SimpleIdentity, UserId](
          FieldName("id"),
          UserId.schemaSttp,
          input => Some(input.id)
        ),
        SProductField[SimpleIdentity, Option[PhoneNumber]](
          FieldName("phone"),
          Schema.derived[PhoneNumber].asOption,
          input => Some(input.phone)
        ),
        SProductField[SimpleIdentity, Email](
          FieldName("email"),
          Email.schemaSttp,
          input => Some(input.email)
        ),
        SProductField[SimpleIdentity, Option[FullName]](
          FieldName("name"),
          Schema.derived[FullName].asOption,
          input => Some(input.name)
        ),
        SProductField[SimpleIdentity, Option[Sex]](
          FieldName("sex"),
          Sexes.schema.asOption,
          input => Some(input.sex)
        ),
        SProductField[SimpleIdentity, Option[URI]](
          FieldName("image"),
          Schemas.uriSchema.asOption,
          input => Some(input.image)
        )
      )
    ),
    name = Some(SName("SimpleIdentity"))
  )

}
