package io.hiis.service.auth.services.database

import io.hiis.service.auth.models.security.PasswordInfo
import io.hiis.service.core.models.Config.MongodbConfig
import io.hiis.service.core.services.database.MongodbService
import mongo4cats.bson.Document
import mongo4cats.zio.{ ZMongoClient, ZMongoCollection }
import zio.{ Task, ZLayer }

trait PasswordMongodbService extends MongodbService[PasswordInfo]

final case class PasswordMongodbServiceImpl(mongodbConfig: MongodbConfig, client: ZMongoClient)
    extends PasswordMongodbService {
  override protected def collection: Task[ZMongoCollection[Document]] = for {
    database   <- client.getDatabase(mongodbConfig.database)
    collection <- database.getCollection("Passwords")
  } yield collection
}

object PasswordMongodbService {
  val live: ZLayer[MongodbConfig with ZMongoClient, Nothing, PasswordMongodbServiceImpl] =
    ZLayer.fromFunction(PasswordMongodbServiceImpl.apply _)
}
