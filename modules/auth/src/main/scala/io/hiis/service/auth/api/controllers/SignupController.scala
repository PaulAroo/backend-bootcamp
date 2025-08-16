package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.User
import io.hiis.service.auth.models.rest.request.SignupRequest
import io.hiis.service.auth.models.rest.response.Token
import io.hiis.service.auth.models.security.TokenActions
import io.hiis.service.auth.services.{ PasswordService, TotpService, UserService }
import io.hiis.service.auth.services.UserService.{
  EmailAndPhoneNumberErrors,
  EmailError,
  Failed,
  PhoneNumberError,
  Success
}
import io.hiis.service.core.api.Api.ApiError.{ conflict, Conflict, InternalServerError }
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.misc.Notification.EmailNotification
import io.hiis.service.core.models.{ Constants, UserId }
import io.hiis.service.core.utils.Logging
import io.hiis.service.notification.services.NotificationService
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.jsonBody
import zio.ZIO

import java.util.UUID

final case class SignupController(
    userService: UserService,
    passwordService: PasswordService,
    totpService: TotpService,
    notificationService: NotificationService
) extends Controller
    with Logging {

  override val BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  val signup: ServerEndpointT[Any, Any] = UnsecuredEndpoint(conflict).post
    .in("signup")
    .in(jsonBody[SignupRequest])
    .out(jsonBody[Token])
    .name("signup")
    .summary("signup new user")
    .description("Signup a new user")
    .serverLogic { request => signup =>
      for {
        initialID <- ZIO
          .succeed(UserId(UUID.randomUUID().toString.replace("-", "")))
          .tap(id => logInfo(s"UserID: ${id.value}"))
        message <- userService
          .isUnique(
            id = initialID,
            email = signup.email,
            phoneNumber = Some(signup.phone)
          )
          .flatMap {
            case Failed => ZIO.fail(InternalServerError("Internal server error"))
            case Success =>
              for {
                user <- userService.save(
                  User(
                    id = initialID,
                    name = Some(signup.name),
                    email = signup.email,
                    phone = Some(signup.phone),
                    sex = Some(signup.sex)
                  )
                )
                _ <- passwordService.create(user.id, signup.password)
                totp <- totpService
                  .createTotpToken(user, TokenActions.ACCOUNT_VERIFICATION)
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
              } yield Token(totp.crypto)
            case EmailAndPhoneNumberErrors | EmailError | PhoneNumberError =>
              ZIO.fail(Conflict("Unable to create user. Conflict with either email or phone"))
          }
      } yield message
    }

  override def tag: String = "Authentication"

  override def endpoints: List[ServerEndpointT[Any, Any]] = List(signup)
}
