package io.hiis.service.auth

import io.hiis.service.auth.api.controllers.{
  AccountVerificationController,
  LogoutController,
  NoMFASigninController,
  ProfileController,
  SignupController
}
import io.hiis.service.auth.services.{
  PasswordService,
  RefreshTokenService,
  TotpService,
  UserService
}
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
        with RefreshTokenService
    ] {

  override def endpoints: ZIO[
    AuthTokenService
      with NotificationService
      with TotpService
      with PasswordService
      with UserService
      with RefreshTokenService,
    Nothing,
    List[ServerEndpointT[Any, Any]]
  ] = for {
    userService         <- ZIO.service[UserService]
    passwordService     <- ZIO.service[PasswordService]
    totpService         <- ZIO.service[TotpService]
    notificationService <- ZIO.service[NotificationService]
    authTokenService    <- ZIO.service[AuthTokenService]
    refreshTokenService <- ZIO.service[RefreshTokenService]
  } yield ModuleEndpoints.fromControllers(
    LogoutController(refreshTokenService)(authTokenService),
    NoMFASigninController(userService, passwordService, refreshTokenService)(authTokenService),
    SignupController(userService, passwordService, totpService, notificationService),
    AccountVerificationController(
      totpService,
      userService,
      authTokenService,
      refreshTokenService,
      notificationService
    ),
    ProfileController(userService)(authTokenService)
  )
}
