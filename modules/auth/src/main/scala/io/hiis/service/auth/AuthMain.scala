package io.hiis.service.auth

import io.hiis.service.auth.api.controllers.{
  AccountVerificationController,
  LogoutController,
  NoMFASigninController,
  PasswordController,
  ProfileController,
  RefreshTokenController,
  SignupController
}
import io.hiis.service.auth.services.{
  PasswordService,
  RefreshTokenService,
  TotpService,
  UserService
}
import io.hiis.service.core.api.{ ModuleApp, ModuleEndpoints }
import io.hiis.service.core.api.tapir.TapirT.ServerEndpointT
import io.hiis.service.core.services.security.AuthTokenService
import io.hiis.service.core.utils.Logging
import io.hiis.service.notification.services.NotificationService
import zio.{ durationInt, Schedule, ZIO }

object AuthMain
    extends Logging
    with ModuleEndpoints[
      AuthTokenService
        with NotificationService
        with TotpService
        with PasswordService
        with UserService
        with RefreshTokenService
    ]
    with ModuleApp[RefreshTokenService with TotpService] {

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
    RefreshTokenController(refreshTokenService)(authTokenService),
    NoMFASigninController(userService, passwordService, refreshTokenService)(authTokenService),
    SignupController(userService, passwordService, totpService, notificationService),
    AccountVerificationController(
      totpService,
      userService,
      authTokenService,
      refreshTokenService,
      notificationService
    ),
    PasswordController(
      totpService,
      userService,
      passwordService,
      refreshTokenService,
      notificationService
    )(authTokenService),
    ProfileController(userService)(authTokenService)
  )

  override def app: ZIO[RefreshTokenService with TotpService, Throwable, Any] = {
    // Expired totp token cleanup job
    ZIO
      .service[TotpService]
      .flatMap(_.removeExpired().repeat(Schedule.fixed(10.minutes))) <&
      // Expired refresh token cleanup job
      ZIO
        .service[RefreshTokenService]
        .flatMap(_.removeExpired().repeat(Schedule.fixed(1.hour)))
  }
}
