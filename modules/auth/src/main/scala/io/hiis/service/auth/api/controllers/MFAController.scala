package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.Totp
import io.hiis.service.auth.models.rest.Identifier
import io.hiis.service.auth.models.rest.response.{ LoginResponse, Token }
import io.hiis.service.auth.models.security.TokenActions
import io.hiis.service.auth.services.{ RefreshTokenService, TotpService, UserService }
import io.hiis.service.core.api.Api.ApiError.BadRequest
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.Constants
import io.hiis.service.core.models.misc.Notification.EmailNotification
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.{ Logging, SilentLogging }
import io.hiis.service.notification.services.NotificationService
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import zio.ZIO

import java.time.Instant

final case class MFAController(
    totpService: TotpService,
    userService: UserService,
    authTokenService: AuthTokenService,
    refreshTokenService: RefreshTokenService,
    notificationService: NotificationService
) extends Controller
    with SilentLogging {
  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  private val verifyAccount: ServerEndpointT[Any, Any] = UnsecuredEndpoint().get
    .in("two-factor" / "verification")
    .in(query[Int]("code"))
    .in(query[String]("token"))
    .out(jsonBody[LoginResponse])
    .name(s" 2FA verification")
    .summary(s" 2FA verification")
    .description(s" 2FA verification using secret and OTP")
    .serverLogic { request => value =>
      val (code, token) = value
      (totpService
        .validate(Totp(code.toString, token)) <*> totpService.find(token))
        .flatMap {
          case (true, Some(totp)) if totp.action == TokenActions.TWO_FACTOR_AUTHENTICATION =>
            for {
              user <- userService.get(totp.user).flatMap {
                case Some(value) => ZIO.succeed(value)
                case None        => ZIO.fail(BadRequest("Invalid request"))
              }

              _ <- userService
                .updateActivationState(user.id, state = true)
                .map(_ => userService.updateLastLoginTime(user.id, Instant.now()))

              _            <- totpService.remove(token)
              jwt          <- authTokenService.create(user)
              refreshToken <- refreshTokenService.create(totp.user)
            } yield LoginResponse(user, jwt, refreshToken)
          case _ => ZIO.fail(BadRequest("Failed to validate otp"))
        }
    }

  private val resendVerification: ServerEndpointT[Any, Any] = UnsecuredEndpoint().post
    .in("two-factor" / "verification" / "resend")
    .in(jsonBody[Identifier])
    .out(jsonBody[Token])
    .name(s"resend  2FA verification code")
    .summary(s"resend  2FA verification code")
    .description(s"Resend  2FA verification code to phone number or email")
    .serverLogic { request => identifier =>
      userService
        .getByEmail(identifier.email)
        .flatMap {
          case Some(user) =>
            totpService
              .createTotpToken(user, TokenActions.TWO_FACTOR_AUTHENTICATION)
              .tap(totp =>
                notificationService
                  .sendEmail(
                    EmailNotification(
                      Constants.ORG_EMAIL,
                      user.email,
                      "Your OTP",
                      txt.otp(totp.otp).body
                    )
                  )
                  .ignoreLogged
              )
              .map(totp => Token(totp.crypto))
          case _ => ZIO.fail(BadRequest("Bad request"))
        }
    }

  override def endpoints: List[ServerEndpointT[Any, Any]] =
    List(verifyAccount, resendVerification)

  override def tag: String = "Multi-Factor Authentication"
}
