package io.hiis.service.auth.services

import io.circe.Json
import io.circe.generic.auto.{ exportDecoder, exportEncoder }
import io.circe.syntax.EncoderOps
import io.hiis.service.auth.models.User
import io.hiis.service.auth.services.UserService.{
  EmailAndPhoneNumberErrors,
  EmailError,
  Failed,
  PhoneNumberError,
  Success,
  UniquenessResult
}
import io.hiis.service.auth.services.database.{ UserMongodbService, UserMongodbServiceImpl }
import io.hiis.service.core.models.Config.MongodbConfig
import io.hiis.service.core.models.UserId
import io.hiis.service.core.models.misc.{ Email, FullName, PhoneNumber }
import mongo4cats.bson.Document
import mongo4cats.bson.syntax._
import mongo4cats.circe.deriveJsonBsonValueEncoder
import mongo4cats.zio.ZMongoClient
import zio.{ Task, ZIO, ZLayer }

import java.net.URI
import java.time.Instant

trait UserService {

  /**
   * Save a user object to data store
   * @param user
   *   Employee to be saved
   * @return
   *   saved Employee
   */
  def save(user: User): Task[User]

  /**
   * Get a user from data store using id
   * @param id
   *   user id
   * @return
   *   User object if found
   */
  def get(userId: UserId): Task[Option[User]]

  /**
   * Get all users
   * @return
   *   List of User objects
   */
  def get: Task[List[User]]

  /**
   * Get admin user
   * @return
   *   user object if found
   */
  def getAdmin: Task[Option[User]]

  /**
   * Get a user from data store using id
   * @param ids
   *   user ids
   * @return
   *   List of users
   */
  def get(userIds: Set[UserId]): Task[List[User]]

  /**
   * Get a user from data store using identifier
   * @param email
   *   user email
   * @return
   *   User (T) object if found
   */
  def getByEmail(email: Email): Task[Option[User]]

  /**
   * Updates user object in data store
   * @param user
   *   the user object to be updated
   * @return
   *   updated user
   */
  def update(user: User): Task[User]

  /**
   * Verifies if an identifier is already being used
   * @param email
   *   email
   * @return
   *   true if email is already being used otherwise false
   */
  def isEmailUsed(email: Email): Task[Boolean]

  /**
   * Update account activation state
   * @param id
   *   user id
   * @param state
   *   new state
   * @return
   *   unit
   */
  def updateActivationState(id: UserId, state: Boolean): Task[Unit]

  /**
   * Update last login time
   * @param id
   *   user id
   * @param lastLoginAt
   *   last login time
   * @return
   *   unit
   */
  def updateLastLoginTime(id: UserId, lastLoginAt: Instant): Task[Unit]

  /**
   * Check if user details are unique (keys)
   * @param id
   *   userid
   * @param email
   *   email
   * @param phoneNumber
   *   phone number
   * @return
   */
  def isUnique(
      id: UserId,
      email: Email,
      phoneNumber: Option[PhoneNumber]
  ): Task[UniquenessResult]

  /**
   * Find user using Bson object
   * @param query
   *   bson document object with query details
   * @return
   *   user if found
   */
  def find(query: Document): Task[Option[User]]

  /**
   * Update the user profile image
   * @param id
   *   user id
   * @param imageUrl
   *   image url
   * @return
   */
  def updateImage(id: UserId, imageUrl: URI): Task[Unit]

  /**
   * Update users name
   * @param id
   *   user id
   * @param name
   *   name
   * @return
   */
  def updateName(id: UserId, name: FullName): Task[Unit]

}

final case class UserServiceImpl(database: UserMongodbService) extends UserService {

  /**
   * Save a user object to data store
   *
   * @param user
   *   Employee to be saved
   * @return
   *   saved Employee
   */
  override def save(user: User): Task[User] = database.save(user)

  /**
   * Get a user from data store using id
   *
   * @param id
   *   user id
   * @return
   *   User object if found
   */
  override def get(userId: UserId): Task[Option[User]] =
    database.get(Document("id" := userId.value))

  /**
   * Get all users
   *
   * @return
   *   List of User objects
   */
  override def get: Task[List[User]] = database.getMany(Document.empty)

  /**
   * Get a user from data store using id
   *
   * @param ids
   *   user ids
   * @return
   *   List of users
   */
  override def get(userIds: Set[UserId]): Task[List[User]] = database.getMany(
    Document.parse(Json.obj("id" -> Json.obj("$in" -> userIds.map(_.value).asJson)).noSpaces)
  )

  /**
   * Get a user from data store using identifier
   *
   * @param email
   *   user email
   * @return
   *   User (T) object if found
   */
  override def getByEmail(email: Email): Task[Option[User]] =
    database.get(Document("email" := email.value))

  /**
   * Updates user object in data store
   *
   * @param user
   *   the user object to be updated
   * @return
   *   updated user
   */
  override def update(user: User): Task[User] = database.updateOne(
    Document("id"   := user.id.value),
    Document("$set" := Document.parse(user.asJson.noSpaces))
  ) *> ZIO.succeed(user)

  /**
   * Verifies if an identifier is already being used
   *
   * @param email
   *   email
   * @return
   *   true if email is already being used otherwise false
   */
  override def isEmailUsed(email: Email): Task[Boolean] =
    database.get(Document("email" := email.value)).map(_.isDefined)

  /**
   * Update account activation state
   *
   * @param id
   *   user id
   * @param state
   *   new state
   * @return
   *   unit
   */
  override def updateActivationState(id: UserId, state: Boolean): Task[Unit] = database
    .updateOne(
      Document("id"   := id.value),
      Document("$set" := Document("isActivated" := state))
    )
    .unit

  /**
   * Update last login time
   *
   * @param id
   *   user id
   * @param lastLoginAt
   *   last login time
   * @return
   *   unit
   */
  override def updateLastLoginTime(id: UserId, lastLoginAt: Instant): Task[Unit] = database
    .updateOne(
      Document("id"   := id.value),
      Document("$set" := Document("lastLoginAt" := lastLoginAt))
    )
    .unit

  /**
   * Check if user details are unique (keys)
   * @param id
   *   userid
   * @param email
   *   email
   * @param phoneNumber
   *   phone number
   * @return
   */
  override def isUnique(
      id: UserId,
      email: Email,
      phoneNumber: Option[PhoneNumber]
  ): Task[UniquenessResult] = database
    .get(
      phoneNumber
        .map { phone =>
          Document.parse(
            Json
              .obj(
                "$or" -> Json.arr(
                  Json.obj("id"    -> id.asJson),
                  Json.obj("phone" -> phone.asJson),
                  Json.obj("email" -> email.asJson)
                )
              )
              .noSpaces
          )
        }
        .getOrElse(
          Document.parse(
            Json
              .obj(
                "$or" -> Json.arr(
                  Json.obj("id"    -> id.asJson),
                  Json.obj("phone" -> phoneNumber.asJson)
                )
              )
              .noSpaces
          )
        )
    )
    .flatMap {
      case None => ZIO.succeed(Success)
      case Some(value)
          if value.phone.nonEmpty && value.phone == phoneNumber && value.email == email =>
        ZIO.succeed(EmailAndPhoneNumberErrors)
      case Some(value) if phoneNumber.nonEmpty && value.phone == phoneNumber =>
        ZIO.succeed(PhoneNumberError)
      case Some(value) if value.email == email => ZIO.succeed(EmailError)
      case _                                   => ZIO.succeed(Failed)
    }

  /**
   * Find user using Bson object
   * @param query
   *   bson document object with query details
   * @return
   *   user if found
   */
  override def find(query: Document): Task[Option[User]] = database.get(query)

  /**
   * Update the user profile image
   *
   * @param id
   *   user id
   * @param imageUrl
   *   image url
   * @return
   */
  override def updateImage(id: UserId, imageUrl: URI): Task[Unit] = database
    .updateOne(
      Document("id"   := id.value),
      Document("$set" := Document("image" := imageUrl.toString))
    )
    .unit

  /**
   * Update users name
   *
   * @param id
   *   user id
   * @param name
   *   name
   * @return
   */
  override def updateName(id: UserId, name: FullName): Task[Unit] = database
    .updateOne(
      Document("id"   := id.value),
      Document("$set" := Document("name" := name))
    )
    .unit

  /**
   * Get admin user
   *
   * @return
   *   user object if found
   */
  override def getAdmin: Task[Option[User]] = ???
}

object UserService {

  val live: ZLayer[ZMongoClient with MongodbConfig, Nothing, UserService] = ZLayer.fromZIO(
    for {
      mongodbConfig <- ZIO.service[MongodbConfig]
      client        <- ZIO.service[ZMongoClient]
    } yield UserServiceImpl(UserMongodbServiceImpl(mongodbConfig, client))
  )

  sealed trait UniquenessResult

  case object Success                   extends UniquenessResult
  case object Failed                    extends UniquenessResult
  case object PhoneNumberError          extends UniquenessResult
  case object EmailError                extends UniquenessResult
  case object EmailAndPhoneNumberErrors extends UniquenessResult

}
