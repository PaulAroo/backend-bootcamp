package io.hiis.service.auth.services

import io.hiis.service.auth.models.Totp
import io.hiis.service.auth.models.security.TokenActions.TokenAction
import io.hiis.service.auth.models.security.{ TokenActions, TotpToken }
import io.hiis.service.auth.services.database.TotpMongodbService
import io.hiis.service.auth.services.security.TotpSha512
import io.hiis.service.core.models.Config.MongodbConfig
import io.hiis.service.core.models.auth.Identity
import io.hiis.service.core.utils.Logging
import mongo4cats.bson.Document
import mongo4cats.bson.syntax._
import mongo4cats.circe.deriveJsonBsonValueEncoder
import mongo4cats.zio.ZMongoClient
import zio.{ Task, ZIO, ZLayer }

import java.time.Instant
import java.util.UUID

trait TotpService extends TotpSha512 {

  def find(crypto: String): Task[Option[TotpToken]]

  def remove(crypto: String): Task[Unit]

  def removeExpired(): Task[Unit]

  def createTotpToken(user: Identity, action: TokenAction): Task[Totp]

  def findUser(userId: String): Task[Option[TotpToken]]

  def validate(totp: Totp): Task[Boolean]
}

final case class TotpServiceImpl(
    databaseService: TotpMongodbService
) extends TotpService
    with Logging {

  override def find(crypto: String): Task[Option[TotpToken]] =
    databaseService.get(Document("crypto" := crypto))

  override def remove(crypto: String): Task[Unit] =
    databaseService.deleteOne(Document("crypto" := crypto)).as(())

  override def removeExpired(): Task[Unit] = {
    import io.hiis.service.core.utils.ImplicitJsonFormats.InstantFormat._
    logDebug("Deleting expired totp tokens") *> databaseService
      .deleteMany(Document("expiry" := Document("$lt" := Instant.now())))
  }

  override def createTotpToken(user: Identity, action: TokenAction): Task[Totp] = {
    val crypto    = UUID.randomUUID().toString
    val expiresOn = Instant.now().plusSeconds(TOTP_TIME_INTERVAL.toSeconds)
    for {
      otp <- getTotp(crypto)
      notifAndToken <-
        if (action == TokenActions.EMAIL_VERIFICATION)
          save(TotpToken(crypto, user.id, action, expiresOn, 0, Some(user.email.value)))
        else
          save(TotpToken(crypto, user.id, action, expiresOn, 0))
    } yield Totp(otp, notifAndToken.crypto)
  }

  override def findUser(userId: String): Task[Option[TotpToken]] =
    databaseService.get(Document("user" := userId))

  override def validate(totp: Totp): Task[Boolean] = find(totp.crypto).flatMap {
    case Some(token) =>
      if (token.attempts > 3)
        ZIO.succeed(false)
      else if (token.expiry.isBefore(Instant.now))
        wrongAttempt(totp.crypto)
      else
        validateTotp(totp.otp, totp.crypto).flatMap {
          case true => ZIO.succeed(true)
          case _    => wrongAttempt(totp.crypto).map(_ => false)
        }
    case None => ZIO.succeed(false)
  }

  private def wrongAttempt(crypto: String): Task[Boolean] = {
    for {
      maybeToken <- find(crypto)
      result <- databaseService
        .updateOne(
          Document("crypto" := crypto),
          Document("$set"   := Document("attempts" := maybeToken.get.attempts + 1))
        )
        .map(_ => false)
    } yield result
  }

  private def save(token: TotpToken): Task[TotpToken] = databaseService.save(token)
}

object TotpService {
  val live: ZLayer[MongodbConfig with ZMongoClient, Nothing, TotpServiceImpl] =
    ZLayer.fromZIO(for {
      mongodbClient <- ZIO.service[ZMongoClient]
      mongodbConfig <- ZIO.service[MongodbConfig]
      totpDatabase <- ZIO
        .service[TotpMongodbService]
        .provide(
          TotpMongodbService.live,
          ZLayer.succeed(mongodbClient),
          ZLayer.succeed(mongodbConfig)
        )
      totpService = TotpServiceImpl(totpDatabase)
    } yield totpService)
}
