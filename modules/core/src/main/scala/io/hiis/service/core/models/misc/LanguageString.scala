package io.hiis.service.core.models.misc

import io.circe.{ Decoder, Encoder, HCursor, Json }
import sttp.tapir.SchemaType.{ SProduct, SProductField }
import sttp.tapir.{ FieldName, Schema }

import scala.util.Try

sealed abstract class LanguageString(val language: Languages.Language, val value: String)

object LanguageString {
  implicit val encoder: Encoder[LanguageString] = (a: LanguageString) =>
    Json.obj(
      ("language", Json.fromString(a.language.toString)),
      ("value", Json.fromString(a.value))
    )

  implicit val decoder: Decoder[LanguageString] = (c: HCursor) => {
    for {
      language <- c.downField("language").as[String]
      value    <- c.downField("value").as[String]
    } yield apply(language, value)
  }

  implicit val languageStringSchema: Schema[LanguageString] = Schema(
    SProduct[LanguageString](
      List(
        SProductField[LanguageString, Languages.Language](
          FieldName("language"),
          Schema.derivedEnumerationValue[Languages.Language],
          obj => Some(obj.language)
        ),
        SProductField[LanguageString, String](
          FieldName("value"),
          Schema.schemaForString,
          obj => Some(obj.value)
        )
      )
    )
  )

  def apply(language: String, value: String): LanguageString =
    Try(Languages.withName(language)).getOrElse(Languages.EN) match {
      case Languages.EN => EnglishString(value)
      case Languages.FR => FrenchString(value)
      case Languages.ES => SpanishString(value)
    }
}

final case class EnglishString(override val value: String)
    extends LanguageString(Languages.EN, value)

final case class FrenchString(override val value: String)
    extends LanguageString(Languages.FR, value)

final case class SpanishString(override val value: String)
    extends LanguageString(Languages.ES, value)
