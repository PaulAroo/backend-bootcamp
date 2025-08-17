package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.rest.request.Credentials
import io.hiis.service.auth.models.rest.response.LoginResponse
import io.hiis.service.auth.services.{ PasswordService, RefreshTokenService, UserService }
import io.hiis.service.core.api.Api.ApiError.{ forbidden, BadRequest, Forbidden }
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.SilentLogging
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import zio.ZIO

import java.time.Instant

final case class NoMFASigninController(
    userService: UserService,
    passwordService: PasswordService,
    refreshTokenService: RefreshTokenService
)(implicit authTokenService: AuthTokenService)
    extends Controller
    with SilentLogging {
  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  private val login: ServerEndpointT[Any, Any] = UnsecuredEndpoint(forbidden).post
    .in("login")
    .in(jsonBody[Credentials])
    .out(jsonBody[LoginResponse])
    .name(s" login")
    .summary(s" login")
    .description(s"Login  using institution id and password")
    .serverLogic { request => credentials =>
      for {
        maybeUser <- userService
          .getByEmail(credentials.identifier)
          .mapError(implicit error => BadRequest("Error with credentials"))
        result <- maybeUser
          .map { user =>
            if (!user.isActivated) {
              ZIO.fail(Forbidden("Account not verified"))
            } else {
              passwordService
                .validate(user.id, credentials.password)
                .flatMap { isAuth =>
                  if (isAuth) {
                    for {
                      jwt          <- authTokenService.create(user)
                      refreshToken <- refreshTokenService.create(user.id)
                      _            <- userService.updateLastLoginTime(user.id, Instant.now())
                    } yield LoginResponse(user, jwt, refreshToken)
                  } else {
                    ZIO.fail(BadRequest("Error with credentials"))
                  }
                }
            }
          }
          .getOrElse(ZIO.fail(BadRequest("Error with credentials")))
      } yield result
    }

  override def endpoints = List(login)

  override def tag: String = "Authentication"
}
