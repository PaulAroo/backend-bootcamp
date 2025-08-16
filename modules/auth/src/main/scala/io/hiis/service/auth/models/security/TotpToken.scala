package io.hiis.service.auth.models.security

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import io.hiis.service.auth.models.security.TokenActions.TokenAction
import io.hiis.service.core.models.UserId

import java.time.Instant

object TokenActions extends Enumeration {
  type TokenAction = Value
  val RESET_PASSWORD            = Value("RESET_PASSWORD")
  val ACCOUNT_VERIFICATION      = Value("ACCOUNT_VERIFICATION")
  val EMAIL_VERIFICATION        = Value("EMAIL_VERIFICATION")
  val TWO_FACTOR_AUTHENTICATION = Value("TWO_FACTOR_AUTHENTICATION")

  implicit val tokenActionEncoder: Encoder[TokenAction] =
    Encoder.encodeEnumeration(TokenActions)
  implicit val tokenActionDecoder: Decoder[TokenAction] =
    Decoder.decodeEnumeration(TokenActions)
}

/**
 * A token for two-factor authentication
 *
 * @param crypto
 *   Shared secret between client and server
 * @param user
 *   The user associated to this token
 * @param expiry
 *   The date-time the token expires.
 * @param attempts
 *   Number of wrong attempts
 */
case class TotpToken(
    crypto: String,
    user: UserId,
    action: TokenAction,
    expiry: Instant,
    attempts: Int,
    data: Option[String] = None
) extends Serializable

object TotpToken {

  implicit val decoder: Decoder[TotpToken] = deriveDecoder
  implicit val encoder: Encoder[TotpToken] = deriveEncoder
}
