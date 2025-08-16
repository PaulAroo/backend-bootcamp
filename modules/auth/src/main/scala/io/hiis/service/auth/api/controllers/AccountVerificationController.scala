package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.Totp
import io.hiis.service.auth.models.rest.Identifier
import io.hiis.service.auth.models.rest.response.{ LoginResponse, Token }
import io.hiis.service.auth.models.security.TokenActions
import io.hiis.service.auth.services.{ TotpService, UserService }
import io.hiis.service.core.api.Api.ApiError.BadRequest
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.Constants
import io.hiis.service.core.models.misc.Notification.EmailNotification
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.Logging
import io.hiis.service.notification.services.NotificationService
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.jsonBody
import zio.ZIO

import java.time.Instant

final case class AccountVerificationController(
    totpService: TotpService,
    userService: UserService,
    authTokenService: AuthTokenService,
    notificationService: NotificationService
) extends Controller
    with Logging {

  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  private val verifyAccount: ServerEndpointT[Any, Any] = UnsecuredEndpoint().get
    .in("account" / "verification")
    .in(query[Int]("otp"))
    .in(query[String]("token"))
    .out(jsonBody[LoginResponse])
    .name(s"verify account")
    .summary(s"verify  account")
    .description(s"Verify account using secret and OTP")
    .serverLogic { request => value =>
      val (otp, token) = value
      (totpService
        .validate(Totp(otp.toString, token)) <*> totpService.find(token))
        .flatMap {
          case (true, Some(totp)) if totp.action == TokenActions.ACCOUNT_VERIFICATION =>
            for {
              user <- userService.get(totp.user).flatMap {
                case Some(value) => ZIO.succeed(value)
                case None        => ZIO.fail(BadRequest("Invalid request"))
              }

              _ <- userService
                .updateActivationState(user.id, state = true)
                .map(_ => userService.updateLastLoginTime(user.id, Instant.now()))

              _   <- totpService.remove(token)
              jwt <- authTokenService.create(user)
            } yield LoginResponse(user, jwt)
          case _ => ZIO.fail(BadRequest("Failed to validate otp"))
        }
    }

  private val resendVerification: ServerEndpointT[Any, Any] = UnsecuredEndpoint().post
    .in("account" / "verification" / "resend")
    .in(jsonBody[Identifier])
    .out(jsonBody[Token])
    .name(s"resend  verification code")
    .summary(s"resend verification code")
    .description(s"Resend  verification code to phone number")
    .serverLogic { request => identifier =>
      userService
        .getByEmail(identifier.email)
        .flatMap {
          case Some(user) if !user.isActivated =>
            totpService
              .createTotpToken(user, TokenActions.ACCOUNT_VERIFICATION)
              .tap(totp =>
                notificationService
                  .sendEmail(
                    EmailNotification(
                      Constants.ORG_EMAIL,
                      user.email,
                      "Your OTP",
                      txt.otp(otp = totp.otp).body
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

  override def tag: String = "Account Verification"
}
