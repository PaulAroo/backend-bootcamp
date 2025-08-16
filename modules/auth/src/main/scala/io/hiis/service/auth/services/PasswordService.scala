package io.hiis.service.auth.services

import io.circe.Json
import io.circe.syntax.EncoderOps
import io.hiis.service.auth.models.security.PasswordInfo
import io.hiis.service.auth.services.database.PasswordMongodbService
import io.hiis.service.auth.services.security.PasswordHashService
import io.hiis.service.core.models.Config.MongodbConfig
import io.hiis.service.core.models.UserId
import mongo4cats.bson.Document
import mongo4cats.bson.syntax._
import mongo4cats.circe.deriveJsonBsonValueEncoder
import mongo4cats.zio.ZMongoClient
import zio.{ Task, ZIO, ZLayer }

import java.time.Instant

trait PasswordService {

  /**
   * Registers a password for the user
   * @param id
   *   user id
   * @param password
   *   the users password
   * @return
   *   the passwordInfo
   */
  def create(id: UserId, password: String): Task[PasswordInfo]

  /**
   * Update the users password
   * @param id
   *   user id
   * @param newPassword
   *   new user password
   * @return
   *   the updated passwordInfo
   */
  def update(id: UserId, newPassword: String): Task[Option[PasswordInfo]]

  /**
   * Validate a users password
   * @param id
   *   user id
   * @param password
   *   the plain password
   * @return
   */
  def validate(id: UserId, password: String): Task[Boolean]
}

final case class PasswordServiceImpl(
    databaseService: PasswordMongodbService,
    hashService: PasswordHashService
) extends PasswordService {

  /**
   * Registers a password for the user
   *
   * @param id
   *   user id
   * @param password
   *   the users password
   * @return
   *   the passwordInfo
   */
  override def create(id: UserId, password: String): Task[PasswordInfo] = for {
    hashed <- hashService.hash(password)
    passwordInfo <- databaseService.save(
      PasswordInfo(id, List(hashed), Instant.now())
    )
  } yield passwordInfo

  /**
   * Update the users password
   *
   * @param id
   *   user id
   * @param newPassword
   *   new user password
   * @return
   *   the updated passwordInfo
   */
  override def update(id: UserId, newPassword: String): Task[Option[PasswordInfo]] = for {
    oldInfo <- databaseService.get(Document("id" := id.value))
    hashed  <- hashService.hash(newPassword)
    updatedInfo <- oldInfo
      .map(info =>
        ZIO
          .foreach(info.hashes)(password => hashService.validate(newPassword, password))
          .map(_.exists(_ == true))
          .flatMap {
            case true =>
              ZIO.succeed(None)
            case _ =>
              databaseService
                .updateOne(
                  Document("id" := id.value),
                  Document.parse(
                    Json
                      .obj(
                        "$push" -> Json.obj(
                          "hashes" -> Json.obj(
                            "$each"     -> List(hashed).asJson,
                            "$position" -> 0.asJson
                          )
                        )
                      )
                      .noSpaces
                  )
                )
          }
      )
      .getOrElse(ZIO.succeed(None))
  } yield updatedInfo

  /**
   * Validate a users password
   *
   * @param id
   *   user id
   * @param password
   *   the plain password
   * @return
   */
  override def validate(id: UserId, password: String): Task[Boolean] = for {
    maybeInfo <- databaseService.get(Document("id" := id.value))
    matches <- maybeInfo
      .map(info => hashService.validate(password, info.hashes.head))
      .getOrElse(ZIO.succeed(false))
  } yield matches
}

object PasswordService {
  val live: ZLayer[MongodbConfig with ZMongoClient, Any, PasswordService] =
    ZLayer.fromZIO(for {
      mongodbClient <- ZIO.service[ZMongoClient]
      mongodbConfig <- ZIO.service[MongodbConfig]
      passwordDatabase <- ZIO
        .service[PasswordMongodbService]
        .provide(
          PasswordMongodbService.live,
          ZLayer.succeed(mongodbClient),
          ZLayer.succeed(mongodbConfig)
        )
      passwordHasher <- ZIO.service[PasswordHashService].provide(PasswordHashService.live)
      passwordService = PasswordServiceImpl(passwordDatabase, passwordHasher)
    } yield passwordService)
}
