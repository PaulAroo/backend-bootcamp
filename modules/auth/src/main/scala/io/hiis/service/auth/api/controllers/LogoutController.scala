package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.hiis.service.auth.models.rest.Message
import io.hiis.service.auth.services.RefreshTokenService
import io.hiis.service.core.api.Api.ApiError.{ forbidden, unauthorized, BadRequest }
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.Constants
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.Logging
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe._
import zio.ZIO

final case class LogoutController(
    refreshTokenService: RefreshTokenService
)(implicit authTokenService: AuthTokenService)
    extends Controller
    with Logging {
  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "auth"

  private val logout: ServerEndpointT[Any, Any] = UnsecuredEndpoint(unauthorized, forbidden).get
    .in("logout")
    .in(auth.bearer[String]())
    .in(header[Option[String]](Constants.CustomHeaders.REFRESH_TOKEN_HEADER))
    .in(query[Option[Boolean]]("all"))
    .out(jsonBody[Message])
    .name(s"logout")
    .summary(s"logout")
    .description(
      s"Logout and invalidate a refresh token if sent.\n" +
        s"Invalidates all user refresh tokens if query param all = true or ${Constants.CustomHeaders.REFRESH_TOKEN_HEADER} header is empty"
    )
    .serverLogic { _ => value =>
      val (authToken, refreshToken, all) = (value._1, value._2, value._3.getOrElse(false))

      for {
        authBody <- authTokenService.getBody(authToken).flatMap {
          case Some(value) => ZIO.succeed(value)
          case None        => ZIO.fail(BadRequest("Invalid authorization token"))
        }
        _ <-
          if (all || refreshToken.isEmpty)
            refreshTokenService.revokeAll(authBody.id)
          else if (refreshToken.isDefined)
            refreshTokenService
              .getBody(refreshToken.get)
              .flatMap {
                case Some(value) => ZIO.succeed(value)
                case None        => ZIO.fail(BadRequest("Invalid refresh token"))
              }
              .flatMap(body => refreshTokenService.revoke(body.id))
          else
            ZIO.fail(
              BadRequest(
                s"Can not invalidate refresh token. Either set ${Constants.CustomHeaders.REFRESH_TOKEN_HEADER} header or set query parameter `all` to true."
              )
            )
      } yield Message(
        "Logout successful. Note that it may take a while for the authorization tokens to expire"
      )
    }

  override def endpoints: List[ServerEndpointT[Any, Any]] = List(logout)

  override def tag: String = "Logout"
}
