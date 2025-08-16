package io.hiis.service.auth.services

import io.circe.Json
import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.circe.syntax.EncoderOps
import io.hiis.service.auth.models.security.RefreshToken
import io.hiis.service.auth.services.RefreshTokenService.RefreshTokenIdentity
import io.hiis.service.auth.services.database.RefreshTokenMongodbService
import io.hiis.service.core.models.Config.{ AuthConfig, MongodbConfig }
import io.hiis.service.core.models.UserId
import io.hiis.service.core.services.security.JwtService
import io.hiis.service.core.utils.Logging
import mongo4cats.bson.Document
import mongo4cats.bson.syntax._
import mongo4cats.zio.ZMongoClient
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm
import zio.{ Task, ZIO, ZLayer }

import java.time.Instant

trait RefreshTokenService extends JwtService[RefreshTokenIdentity] {

  /**
   * Verifies if a refresh token is valid
   * @param token
   *   the refresh token
   * @return
   *   true if valid otherwise false
   */
  def isValid(token: String): Task[Boolean]

  /**
   * Creates a new refresh token for a user
   * @param userId
   *   the users id
   * @return
   *   the refresh token
   */
  def create(userId: UserId): Task[String]

  /**
   * Removes all expired refresh token from storage
   * @return
   */
  def removeExpired(): Task[Unit]

  /**
   * Get the refresh token object from storage
   * @param token
   * @return
   */
  def get(token: String): Task[Option[RefreshToken]]

  /**
   * Revokes all user refresh tokens
   * @param userId
   *   the users id
   * @return
   */
  def revokeAll(userId: UserId): Task[Unit]

  /**
   * Revokes a refresh token
   * @param id
   *   the tokens id
   * @return
   */
  def revoke(id: String): Task[Unit]

  /**
   * Extends the expiry date of a refresh token
   * @param token
   *   the token
   * @return
   */
  def extend(token: String): Task[Unit]
}

final case class RefreshTokenServiceImpl(
    database: RefreshTokenMongodbService,
    config: AuthConfig
) extends RefreshTokenService
    with Logging {

  override def maxAge: Long = RefreshToken.MAX_LIVE_DAYS * RefreshToken.DAY_IN_SECONDS

  override protected def key: String = config.refreshTokenKey

  override protected def algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS512

  /**
   * Verifies if a refresh token is valid
   *
   * @param token
   *   the refresh token
   * @return
   *   true if valid otherwise false
   */
  override def isValid(token: String): Task[Boolean] =
    validate(token).flatMap(identity =>
      database.get(Document("id" := identity.id)).flatMap {
        case Some(refreshToken) =>
          val now = Instant.now()
          if (refreshToken.maxLiveDate.isBefore(now)) ZIO.succeed(false)
          else if (refreshToken.expiresAt.isBefore(now)) {
            // Check if it has been used in the last 14 days
            if (
              refreshToken.lastUsedAt
                .isAfter(now.minusSeconds(RefreshToken.DAY_IN_SECONDS * RefreshToken.WINDOW_DAYS))
            ) ZIO.succeed(true)
            else ZIO.succeed(false)
          } else ZIO.succeed(true)
        case None => ZIO.succeed(false)
      }
    )

  /**
   * Creates a new refresh token for a user
   *
   * @param userId
   *   the users id
   * @return
   *   the refresh token
   */
  override def create(userId: UserId): Task[String] =
    database
      .save(RefreshToken(userId))
      .flatMap(token => create(RefreshTokenIdentity(token.id, token.userId)).map(_.token))

  /**
   * Removes all expired refresh token from storage
   *
   * @return
   */
  override def removeExpired(): Task[Unit] = {

    val now = Instant.now()

    logDebug("Deleting expired refresh tokens") *> database
      .deleteMany(
        Document.parse(
          Json
            .obj(
              "$or" -> Json
                .arr(
                  Json.obj("expiresAt"   -> Json.obj("$lt" -> now.asJson)),
                  Json.obj("maxLiveDate" -> Json.obj("$lt" -> now.asJson)),
                  Json.obj(
                    "expiresAt" -> Json.obj("$gt" -> now.asJson),
                    "lastUsedAt" -> Json.obj(
                      "$lt" -> now
                        .minusSeconds(RefreshToken.DAY_IN_SECONDS * RefreshToken.WINDOW_DAYS)
                        .asJson
                    )
                  )
                )
            )
            .noSpaces
        )
      )
  }

  /**
   * Get the refresh token object from storage
   * @param token
   *   the token
   * @return
   */
  override def get(token: String): Task[Option[RefreshToken]] = for {
    identity <- validate(token)
    result   <- database.get(Document("id" := identity.id))
  } yield result

  /**
   * Revokes all user refresh tokens
   *
   * @param userId
   *   the users id
   * @return
   */
  override def revokeAll(userId: UserId): Task[Unit] =
    database.deleteMany(Document("userId" := userId.value))

  /**
   * Revokes a refresh token
   * @param id
   *   the tokens id
   * @return
   */
  def revoke(id: String): Task[Unit] = database.deleteMany(Document("id" := id))

  /**
   * Extends the expiry date of a refresh token
   *
   * @param token
   *   the token
   * @return
   */
  override def extend(token: String): Task[Unit] =
    validate(token).flatMap(identity =>
      database.get(Document("id" := identity.id)).flatMap {
        case Some(refreshToken) =>
          val now = Instant.now()
          if (refreshToken.maxLiveDate.isBefore(now)) ZIO.unit
          // Check if it has been used in the last 14 days and extend the expiry date
          else if (
            refreshToken.expiresAt.isBefore(now) && refreshToken.lastUsedAt
              .isAfter(now.minusSeconds(RefreshToken.DAY_IN_SECONDS * RefreshToken.WINDOW_DAYS))
          ) {
            database
              .updateOne(
                Document("id" := identity.id),
                Document(
                  "$set" := Document.parse(
                    refreshToken
                      .copy(
                        lastUsedAt = now,
                        expiresAt = Instant.ofEpochMilli(
                          Math.min(
                            now
                              .plusSeconds(RefreshToken.DAY_IN_SECONDS * RefreshToken.WINDOW_DAYS)
                              .toEpochMilli,
                            refreshToken.maxLiveDate.toEpochMilli
                          )
                        )
                      )
                      .asJson
                      .noSpaces
                  )
                )
              )
          }.unit
          else ZIO.unit
        case None => ZIO.unit
      }
    )
}

object RefreshTokenService {
  val live
      : ZLayer[AuthConfig with MongodbConfig with ZMongoClient, Nothing, RefreshTokenServiceImpl] =
    ZLayer.fromZIO(for {
      mongodbClient <- ZIO.service[ZMongoClient]
      mongodbConfig <- ZIO.service[MongodbConfig]
      authConfig    <- ZIO.service[AuthConfig]
      tokenDatabase <- ZIO
        .service[RefreshTokenMongodbService]
        .provide(
          RefreshTokenMongodbService.live,
          ZLayer.succeed(mongodbClient),
          ZLayer.succeed(mongodbConfig)
        )
    } yield RefreshTokenServiceImpl(tokenDatabase, authConfig))

  case class RefreshTokenIdentity(id: String, userId: UserId)
}
