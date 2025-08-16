package io.hiis.service.auth

import io.hiis.service.auth.api.controllers.{
  AccountVerificationController,
  ProfileController,
  SignupController
}
import io.hiis.service.auth.services.{ PasswordService, TotpService, UserService }
import io.hiis.service.core.api.ModuleEndpoints
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.Logging
import io.hiis.service.notification.services.NotificationService
import zio.ZIO

object AuthMain
    extends Logging
    with ModuleEndpoints[
      AuthTokenService
        with NotificationService
        with TotpService
        with PasswordService
        with UserService
    ] {

  override def endpoints: ZIO[
    AuthTokenService
      with NotificationService
      with TotpService
      with PasswordService
      with UserService,
    Nothing,
    List[ServerEndpointT[Any, Any]]
  ] = for {
    userService         <- ZIO.service[UserService]
    passwordService     <- ZIO.service[PasswordService]
    totpService         <- ZIO.service[TotpService]
    notificationService <- ZIO.service[NotificationService]
    authTokenService    <- ZIO.service[AuthTokenService]
  } yield ModuleEndpoints.fromControllers(
    SignupController(userService, passwordService, totpService, notificationService),
    AccountVerificationController(totpService, userService, authTokenService, notificationService),
    ProfileController(userService)(authTokenService)
  )
}
