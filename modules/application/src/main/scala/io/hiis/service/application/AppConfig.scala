package io.hiis.service.application

import io.hiis.service.core.models.Config.{
  AppServerConfig,
  AuthConfig,
  MailServerConfig,
  MongodbConfig,
  TwilioConfig
}
import io.hiis.service.core.utils.ZIOConfigNarrowOps
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.{ Config, TaskLayer, ZLayer }

object AppConfig {
  final case class ConfigDescriptor(
      mongodb: MongodbConfig,
      appServer: AppServerConfig,
      auth: AuthConfig,
      mailServerConfig: MailServerConfig,
      twilioConfig: TwilioConfig
  )

  val appConfig: ZLayer[Any, Config.Error, ConfigDescriptor] =
    ZLayer.fromZIO(TypesafeConfigProvider.fromResourcePath().load(deriveConfig[ConfigDescriptor]))

  private type AllConfig = AppServerConfig
    with AuthConfig
    with MailServerConfig
    with TwilioConfig
    with MongodbConfig

  val live: TaskLayer[AllConfig] =
    appConfig.narrow[MongodbConfig](_.mongodb) >+>
      appConfig.narrow[AppServerConfig](_.appServer) >+>
      appConfig.narrow[AuthConfig](_.auth) >+>
      appConfig.narrow[MailServerConfig](_.mailServerConfig) >+>
      appConfig.narrow[TwilioConfig](_.twilioConfig)

}
