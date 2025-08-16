package io.hiis.service.auth.services.database

import io.hiis.service.auth.models.security.RefreshToken
import io.hiis.service.core.models.Config.MongodbConfig
import io.hiis.service.core.services.database.MongodbService
import mongo4cats.bson.Document
import mongo4cats.zio.{ ZMongoClient, ZMongoCollection }
import zio.{ Task, ZLayer }

trait RefreshTokenMongodbService extends MongodbService[RefreshToken]

final case class RefreshTokenMongodbServiceImpl(mongodbConfig: MongodbConfig, client: ZMongoClient)
    extends RefreshTokenMongodbService {
  override protected def collection: Task[ZMongoCollection[Document]] = for {
    database   <- client.getDatabase(mongodbConfig.database)
    collection <- database.getCollection("Refresh-tokens")
  } yield collection
}

object RefreshTokenMongodbService {
  val live =
    ZLayer.fromFunction(RefreshTokenMongodbServiceImpl.apply _)
}
