package io.hiis.service.core.api

import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import zio.ZIO

trait ModuleEndpoints[R] {
  def endpoints: ZIO[R, Throwable, List[ServerEndpointT[Any, Any]]] = ZIO.succeed(List.empty)
}

object ModuleEndpoints {
  def fromControllers(controllers: Controller*): List[ServerEndpointT[Any, Any]] =
    controllers.flatMap(controller => controller.endpoints.map(_.tags(List(controller.tag)))).toList
}

trait ModuleApp[R] {
  def app: ZIO[R, Throwable, Any]
}
