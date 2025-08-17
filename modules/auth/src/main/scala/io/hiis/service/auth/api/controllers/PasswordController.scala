package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.Totp
import io.hiis.service.auth.models.rest.Message
import io.hiis.service.auth.models.rest.request.Password
import io.hiis.service.auth.models.rest.response.Token
import io.hiis.service.auth.models.security.TokenActions
import io.hiis.service.auth.services.{
  PasswordService,
  RefreshTokenService,
  TotpService,
  UserService
}
import io.hiis.service.core.api.Api.ApiError.{ notFound, BadRequest, InternalServerError, NotFound }
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.Constants
import io.hiis.service.core.models.misc.Email
import io.hiis.service.core.models.misc.Notification.EmailNotification
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.Logging
import io.hiis.service.notification.services.NotificationService
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import zio.ZIO

import scala.util.Try

final case class PasswordController(
    totpService: TotpService,
    userService: UserService,
    passwordService: PasswordService,
    refreshTokenService: RefreshTokenService,
    notificationService: NotificationService
)(implicit authTokenService: AuthTokenService)
    extends Controller
    with Logging {

  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  val sendOtp: ServerEndpointT[Any, Any] =
    UnsecuredEndpoint(notFound).get
      .in("password" / "recover")
      .in(query[String]("email"))
      .out(jsonBody[Token])
      .name(s"recover  password")
      .summary(s"recover  password")
      .description(s"Request for one time password to recover  password")
      .serverLogic { request => identifier =>
        for {
          email <- ZIO.fromTry(Try(Email(identifier)))
          user <- userService.getByEmail(email).flatMap {
            case Some(value) => ZIO.succeed(value)
            case None        => ZIO.fail(NotFound("User not found"))
          }

          totp <-
            totpService
              .createTotpToken(user, TokenActions.RESET_PASSWORD)
              .tap(totp =>
                notificationService.sendEmail(
                  EmailNotification(
                    Constants.ORG_EMAIL,
                    user.email,
                    "Your OTP",
                    txt.otp(totp.otp).body
                  )
                )
              )
              .map(_.crypto)

        } yield Token(totp)
      }

  val recoverPassword: ServerEndpointT[Any, Any] = UnsecuredEndpoint(notFound).post
    .in("password" / "recover")
    .in(query[Int]("code"))
    .in(query[String]("token"))
    .in(jsonBody[Password])
    .out(jsonBody[Message])
    .name(s"reset  password")
    .summary(s"reset  password")
    .description(s"Reset  password using OTP, secret and new password")
    .serverLogic { request => value =>
      val (code, token, password) = value
      (totpService
        .validate(Totp(code.toString, token)) <*> totpService.find(token))
        .mapError(_ => InternalServerError())
        .flatMap {
          case (true, Some(totp)) if totp.action == TokenActions.RESET_PASSWORD =>
            passwordService
              .update(totp.user, password.password)
              .flatMap(
                _.map(_ =>
                  refreshTokenService
                    .revokeAll(totp.user)
                    .map(_ => Message("Password was updated successfully"))
                )
                  .getOrElse(
                    ZIO.fail(BadRequest("Unable to update password, try another password"))
                  )
              )
          case _ => ZIO.fail(BadRequest("Bad request"))
        }
    }

  val resetPassword: ServerEndpointT[Any, Any] =
    SecuredEndpoint().post
      .in("password" / "change")
      .in(jsonBody[Password])
      .out(jsonBody[Message])
      .name(s"change  password")
      .summary(s"change  password")
      .description(s"Change  password")
      .serverLogic { request => password =>
        for {
          result <- passwordService
            .update(request.identity.id, password.password)
            .mapError(error => InternalServerError(error.getMessage))
            .flatMap(
              _.map(_ =>
                refreshTokenService
                  .revokeAll(request.identity.id)
                  .map(_ => Message("Password was updated successfully"))
              )
                .getOrElse(
                  ZIO.fail(BadRequest("Unable to update password, try another password"))
                )
            )
        } yield result
      }

  override def endpoints: List[ServerEndpointT[Any, Any]] =
    List(sendOtp, recoverPassword, resetPassword)

  override def tag: String = "Password"
}
