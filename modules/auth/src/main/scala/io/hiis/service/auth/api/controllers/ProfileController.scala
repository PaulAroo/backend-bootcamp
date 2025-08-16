package io.hiis.service.auth.api.controllers

import io.circe.generic.auto.{exportDecoder, exportEncoder}
import io.hiis.service.auth.models.rest.Message
import io.hiis.service.auth.models.rest.request.ImageRequest
import io.hiis.service.auth.services.UserService
import io.hiis.service.core.api.Api.ApiError.notFound
import io.hiis.service.core.api.Controller
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.misc.{FullName, LanguageString}
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.Logging
import sttp.tapir.EndpointInput
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.jsonBody

import java.net.URI

final case class ProfileController(userService: UserService)(implicit
    authTokenService: AuthTokenService
) extends Controller
    with Logging {

  override protected def BaseUrl: EndpointInput[Unit] = super.BaseUrl / "profile"

  private val updateImage: ServerEndpointT[Any, Any] =
    SecuredEndpoint(notFound).post
      .in("image")
      .in(jsonBody[ImageRequest])
      .out(jsonBody[Message])
      .name("update profile image")
      .summary("update profile image")
      .description("update profile image")
      .serverLogic { implicit request => input =>
        userService
          .updateImage(request.identity.id, input.url)
          .map(_ => Message("Profile image updated"))
      }

  private val updateName: ServerEndpointT[Any, Any] =
    SecuredEndpoint(notFound).post
      .in("name")
      .in(jsonBody[FullName])
      .out(jsonBody[Message])
      .name("update name")
      .summary("update name")
      .description("update name")
      .serverLogic { implicit request => input =>
        userService.updateName(request.identity.id, input).map(_ => Message("Profile name updated"))
      }

  override def tag: String = "Profile"

  override def endpoints: List[ServerEndpointT[Any, Any]] =
    List(updateName, updateImage)
}