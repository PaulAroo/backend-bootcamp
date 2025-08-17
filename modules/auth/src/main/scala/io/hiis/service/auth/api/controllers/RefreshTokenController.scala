package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.AuthTokens
import io.hiis.service.auth.services.RefreshTokenService
import io.hiis.service.core.api.Api.ApiError.{ forbidden, unauthorized, BadRequest, Unauthorized }
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.Constants
import io.hiis.service.core.models.auth.JwtToken
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.Logging
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe._
import zio.ZIO

import java.time.Instant

final case class RefreshTokenController(
    refreshTokenService: RefreshTokenService
)(implicit authTokenService: AuthTokenService)
    extends Controller
    with Logging {

  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  private val refresh: ServerEndpointT[Any, Any] = UnsecuredEndpoint(unauthorized, forbidden).get
    .in("refresh")
    .in(auth.bearer[String]())
    .in(header[String](Constants.CustomHeaders.REFRESH_TOKEN_HEADER))
    .out(jsonBody[AuthTokens])
    .name(s"refresh  authorization token")
    .summary(s"refresh  authorization token")
    .description(s"Refresh  authorization token using refresh token")
    .serverLogic { _ => value =>
      val (authToken, refreshToken) = (value._1, value._2)

      (refreshTokenService.getBody(refreshToken).flatMap {
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(BadRequest("Bad request"))
      } <*> authTokenService.getBody(authToken).flatMap {
        case Some(value) => ZIO.succeed(value)
        case None        => ZIO.fail(BadRequest("Bad request"))
      })
        .flatMap {
          case (refreshId, authId) if authId.id == refreshId.userId =>
            (refreshTokenService.isValid(refreshToken) <*> authTokenService.isValid(authToken))
              .flatMap {
                case (_, true) => // Auth token still valid
                  ZIO.succeed(
                    AuthTokens(JwtToken(authToken, Instant.now()), refreshToken)
                  )

                case (true, false) => // Generate a new auth token for user
                  for {
                    _       <- refreshTokenService.extend(refreshToken)
                    newAuth <- authTokenService.create(authId)
                  } yield AuthTokens(newAuth, refreshToken)

                case _ => ZIO.fail(Unauthorized("Could not refresh token"))
              }
          case _ => ZIO.fail(Unauthorized("Could not refresh token"))
        }
    }

  override def endpoints: List[ServerEndpointT[Any, Any]] = List(refresh)

  override def tag: String = "Authentication"
}
