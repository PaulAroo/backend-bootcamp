package io.hiis.service.core.services.security

import io.hiis.service.core.models.Config.AuthConfig
import io.hiis.service.core.models.auth.Identity
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm
import zio.ZLayer

trait AuthTokenService extends JwtService[Identity] {
  override protected def algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS384
}

final case class AuthTokenServiceImpl(config: AuthConfig) extends AuthTokenService {
  override protected def key: String = config.authTokenKey

  override def maxAge: Long = config.authTokenMaxAge
}

object AuthTokenService {
  val live: ZLayer[AuthConfig, Nothing, AuthTokenServiceImpl] =
    ZLayer.fromFunction(AuthTokenServiceImpl.apply _)
}
