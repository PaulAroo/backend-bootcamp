package io.hiis.service.application.api.controllers

import io.hiis.service.application.api.utils.ControllerSpec
import sttp.client3._
import sttp.model.{ Header, StatusCode }
import zio.ZLayer
import zio.test.assertTrue

import java.util.UUID

object HealthControllerSpec extends ControllerSpec {
  override def spec = suite("Health Controller Spec")(
    test("health endpoint should respond with OK") {
      for {
        backend <- backendStub
        response <- basicRequest
          .header(Header("x-request-id", UUID.randomUUID().toString))
          .get(uri"http://test.com/health")
          .send(backend)
      } yield assertTrue(response.code == StatusCode.Ok)
    }
  ).provide(ZLayer.succeed(HealthController))
}
