package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.rest.request.Credentials
import io.hiis.service.auth.models.rest.response.Token
import io.hiis.service.auth.models.security.TokenActions
import io.hiis.service.auth.services.{ PasswordService, TotpService, UserService }
import io.hiis.service.core.api.Api.ApiError.{ forbidden, BadRequest, Forbidden }
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.Constants
import io.hiis.service.core.models.misc.Notification.EmailNotification
import io.hiis.service.core.utils.Logging
import io.hiis.service.notification.services.NotificationService
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import zio.ZIO

final case class MFASigninController(
    userService: UserService,
    notificationService: NotificationService,
    totpService: TotpService,
    passwordService: PasswordService
) extends Controller
    with Logging {
  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  private val login: ServerEndpointT[Any, Any] = UnsecuredEndpoint(forbidden).post
    .in("login")
    .in(jsonBody[Credentials])
    .out(jsonBody[Token])
    .name(s" login")
    .summary(s" login")
    .description(s"Login  using institution id and password")
    .serverLogic { request => credentials =>
      for {
        user <- userService.getByEmail(credentials.identifier).flatMap {
          case Some(value) => ZIO.succeed(value)
          case None        => ZIO.fail(BadRequest("Error with credentials"))
        }

        result <-
          if (!user.isActivated) {
            ZIO.fail(Forbidden("Account not verified"))
          } else {
            passwordService
              .validate(user.id, credentials.password)
              .flatMap { isAuth =>
                if (isAuth) {
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
                } else {
                  ZIO.fail(BadRequest("Error with credentials"))
                }
              }
          }

      } yield result
    }

  override def endpoints: List[ServerEndpointT[Any, Any]] = List(login)

  override def tag: String = "Authentication"
}
