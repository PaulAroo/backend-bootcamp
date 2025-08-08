package io.hiis.service.core.api

import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.models.Config.AppServerConfig
import io.hiis.service.core.utils.Logging
import zio._

/** Created by Ludovic Temgoua Abanda (icemc) on 19/01/2023 */

final case class ApiGateway(routes: List[ServerEndpointT[Any, Any]]*)(
    override val config: AppServerConfig
) extends ApiGatewayT
    with Logging

object ApiGateway {
  // Compose your ApiGateway layer here
  def live(
      config: AppServerConfig,
      routes: List[ServerEndpointT[Any, Any]]*
  ): ULayer[ApiGateway] =
    ZLayer.succeed(ApiGateway(routes: _*)(config))
}
