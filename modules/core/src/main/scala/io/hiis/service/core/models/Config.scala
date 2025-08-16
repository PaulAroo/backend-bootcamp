package io.hiis.service.core.models

object Config {

  final case class MongodbConfig(uri: String, database: String)

  final case class AuthConfig(
      authTokenKey: String,
      authTokenMaxAge: Long,
      refreshTokenKey: String,
      refreshTokenMaxAge: Long
  )

  final case class AppServerConfig(host: String, port: Int, serviceURL: Option[String] = None)

  abstract class ExternalServiceConfig(val name: String, val host: String)

  final case class MailServerConfig(
      host: String,
      port: Int,
      username: String,
      password: String,
      ssl: Boolean,
      sender: String
  )

  final case class TwilioConfig(account: String, token: String)
}
