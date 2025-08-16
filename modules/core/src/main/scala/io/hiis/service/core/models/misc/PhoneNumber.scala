package io.hiis.service.core.models.misc

import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.EncoderOps
import io.hiis.service.core.models.misc.Countries.{ CM, Country, LU }
import io.hiis.service.core.models.misc.DialCodes.{ CAMEROON, DialCode, LUXEMBOURG }
import sttp.tapir.Schema

import scala.util.Try

//TODO move such common code to a single library that could be used globally

/**
 * Phone number
 * @param dailCode
 * @param number
 */
case class PhoneNumber(
    dailCode: DialCodes.DialCode,
    number: Long
) {
  require(
    CountryCode(dailCode)
      .flatMap(countryCode =>
        Try {
          val phoneNumberUtil = PhoneNumberUtil.getInstance()
          phoneNumberUtil.isValidNumber(
            phoneNumberUtil
              .parse(s"${countryCode.code.toString}$number", countryCode.country.toString)
          )
        }.toOption
      )
      .getOrElse(false),
    s"Failed to validate phone number: $this"
  )

  override def toString: String = s"$dailCode$number"
}

object PhoneNumber {
  implicit val decoder: Decoder[PhoneNumber] = new Decoder[PhoneNumber] {
    override def apply(c: HCursor): Result[PhoneNumber] = (for {
      dailCode <- c.downField("dailCode").as[DialCodes.DialCode]
      number   <- c.downField("number").as[Int]
    } yield (dailCode, number)).flatMap(value =>
      Try(PhoneNumber(value._1, value._2)).toOption
        .toRight(DecodingFailure("Invalid Phone number", Nil))
    )
  }

  implicit val encoder: Encoder[PhoneNumber] = deriveEncoder
  implicit val schema: Schema[PhoneNumber]   = Schema.derived
}

object DialCodes extends Enumeration {
  type DialCode = Value

  val CAMEROON   = Value("+237")
  val LUXEMBOURG = Value("+352")

  implicit val decoder: Decoder[DialCode] = Decoder.decodeEnumeration(DialCodes)
  implicit val encoder: Encoder[DialCode] = Encoder.encodeEnumeration(DialCodes)
}

object Countries extends Enumeration {
  type Country = Value

  val CM = Value("CM")
  val LU = Value("LU")

  implicit val decoder: Decoder[Country] = Decoder.decodeEnumeration(Countries)
  implicit val encoder: Encoder[Country] = Encoder.encodeEnumeration(Countries)
}

sealed trait CountryCode {
  def country: Countries.Country

  def code: DialCodes.DialCode
}

object CountryCode {
  implicit val encoder: Encoder[CountryCode] = (a: CountryCode) =>
    Json.obj(
      "country" -> a.country.asJson,
      "code"    -> a.code.asJson
    )

  implicit val decoder: Decoder[CountryCode] = (c: HCursor) =>
    (for {
      country <- c.downField("country").as[Countries.Country]
      code    <- c.downField("code").as[DialCodes.DialCode]
    } yield (country, code)).flatMap { countryAndCode =>
      CountryCode(countryAndCode._1, countryAndCode._2)
        .toRight(
          DecodingFailure("Invalid country", Nil)
        )
    }

  case object Cameroon extends CountryCode {
    override def country: Country = Countries.CM

    override def code: DialCodes.DialCode = DialCodes.CAMEROON
  }

  case object Luxembourg extends CountryCode {
    override def country: Country = Countries.LU

    override def code: DialCodes.DialCode = DialCodes.LUXEMBOURG
  }

  // TODO add more countries here

  def apply(code: DialCodes.DialCode): Option[CountryCode] = {
    code match {
      case DialCodes.CAMEROON   => Some(Cameroon)
      case DialCodes.LUXEMBOURG => Some(Luxembourg)

      // TODO add more countries here
      case _ => None
    }
  }

  def apply(country: String): Option[CountryCode] =
    Try(Countries.withName(country)).toOption.flatMap {
      case CM => Some(Cameroon)
      case LU => Some(Luxembourg)

      // TODO add more countries here
      case _ => None
    }

  def apply(country: Country, code: DialCode): Option[CountryCode] =
    (country, code) match {
      case (CM, CAMEROON)   => Some(Cameroon)
      case (LU, LUXEMBOURG) => Some(Cameroon)

      // TODO add more countries here
      case _ => None
    }
}
