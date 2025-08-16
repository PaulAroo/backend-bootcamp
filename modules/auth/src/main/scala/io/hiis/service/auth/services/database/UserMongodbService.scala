package io.hiis.service.auth.services.database

import io.hiis.service.auth.models.User
import io.hiis.service.core.models.Config.MongodbConfig
import io.hiis.service.core.services.database.MongodbService
import mongo4cats.bson.Document
import mongo4cats.zio.{ ZMongoClient, ZMongoCollection }
import zio.{ Task, ZLayer }

trait UserMongodbService extends MongodbService[User]

final case class UserMongodbServiceImpl(mongodbConfig: MongodbConfig, client: ZMongoClient)
    extends UserMongodbService {
  override protected def collection: Task[ZMongoCollection[Document]] = for {
    database   <- client.getDatabase(mongodbConfig.database)
    collection <- database.getCollection("Users")
  } yield collection
}

object UserMongodbService {
  val live: ZLayer[MongodbConfig with ZMongoClient, Nothing, UserMongodbService] =
    ZLayer.fromFunction(UserMongodbServiceImpl.apply _)
}
