package io.hiis.service.application

import io.hiis.service.application.api.controllers.{
  HealthController,
  HomeController,
  MetricsController,
  OptionsController,
  VersionController
}
import io.hiis.service.application.services.MetricsService
import io.hiis.service.core.api.{ ApiGateway, ModuleEndpoints }
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.build.BuildInfo
import io.hiis.service.core.models.Config.AppServerConfig
import io.hiis.service.core.utils.Logging
import mongo4cats.zio.ZMongoClient
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio._
import zio.metrics.connectors.{ prometheus, MetricsConfig }
import zio.metrics.jvm.DefaultJvmMetrics

import java.net.http.HttpClient
import io.hiis.service.auth.AuthMain
import io.hiis.service.auth.services.{
  PasswordService,
  RefreshTokenService,
  TotpService,
  UserService
}
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.notification.services.NotificationService
object Application extends ZIOAppDefault with Logging {

  object UtilityEndpoints extends ModuleEndpoints[MetricsService] {
    override def endpoints: ZIO[MetricsService, Throwable, List[ServerEndpointT[Any, Any]]] = {
      for {
        metricsService <- ZIO.service[MetricsService]
      } yield ModuleEndpoints.fromControllers(
        HealthController,
        HomeController,
        OptionsController,
        VersionController,
        MetricsController(metricsService)
      )
    }
  }

  val mongodbClient: ZLayer[Any, Throwable, ZMongoClient] = AppConfig.appConfig.flatMap(layer =>
    ZLayer.scoped[Any](ZMongoClient.fromConnectionString(layer.get.mongodb.uri))
  )

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Logging.logger ++ AppConfig.appConfig

  private val backend =
    ZLayer.succeed(
      HttpClientZioBackend.usingClient(
        HttpClient.newHttpClient()
      )
    )

  val gatewayApp: ZIO[
    AuthTokenService
      with NotificationService
      with TotpService
      with PasswordService
      with UserService
      with RefreshTokenService
      with MetricsService
      with AppServerConfig,
    Throwable,
    Unit
  ] = for {
    appConfig     <- ZIO.service[AppServerConfig]
    utilityRoutes <- UtilityEndpoints.endpoints
    authRoutes    <- AuthMain.endpoints
    _ <- (ZIO
      .service[ApiGateway]
      .flatMap(_.start) <& logInfo(
      s"Started ${BuildInfo.name} API Gateway Server on port:${appConfig.port}"
    ))
      .provide(
        ApiGateway.live(appConfig, utilityRoutes, authRoutes)
      )
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    gatewayApp.provide(
      AppConfig.live,

      // Provide services for auth routes

      AuthTokenService.live,
      NotificationService.live,
      TotpService.live,
      PasswordService.live,
      UserService.live,
      RefreshTokenService.live,

      // Metrics ZLayers
      ZLayer.succeed(MetricsConfig(15.seconds)),
      prometheus.publisherLayer,
      prometheus.prometheusLayer,

      // Enable the ZIO internal metrics and the default JVM metricsConfig
      Runtime.enableRuntimeMetrics,
      DefaultJvmMetrics.live.unit,
      MetricsService.live,

      // Mongodb client
      mongodbClient
    )
}
