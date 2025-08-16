package io.hiis.backoffice.template.api.utils

import io.hiis.service.core.api.Controller
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError

trait ControllerSpec {
  protected def controller: Controller

  final def backendStub =
    TapirStubInterpreter(SttpBackendStub.apply(new RIOMonadError[Any]))
      .whenServerEndpointsRunLogic(controller.endpoints)
      .backend()
}
