package io.hiis.service.application

import io.hiis.service.core.models.Config.{ AppServerConfig, AuthConfig, MongodbConfig }
import io.hiis.service.core.utils.ZIOConfigNarrowOps
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.{ Config, TaskLayer, ZLayer }

object AppConfig {
  final case class ConfigDescriptor(
      mongodb: MongodbConfig,
      appServer: AppServerConfig,
      auth: AuthConfig
  )

  val appConfig: ZLayer[Any, Config.Error, ConfigDescriptor] =
    ZLayer.fromZIO(TypesafeConfigProvider.fromResourcePath().load(deriveConfig[ConfigDescriptor]))

  private type AllConfig = AppServerConfig with AuthConfig

  val live: TaskLayer[AllConfig] =
//    appConfig.narrow[MongodbConfig](_.mongodb) >+>
    appConfig.narrow[AppServerConfig](_.appServer) >+>
      appConfig.narrow[AuthConfig](_.auth)

}
