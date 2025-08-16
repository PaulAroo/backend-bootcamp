package io.hiis.service.auth.services.database

import io.hiis.service.auth.models.security.TotpToken
import io.hiis.service.core.models.Config.MongodbConfig
import io.hiis.service.core.services.database.MongodbService
import mongo4cats.bson.Document
import mongo4cats.zio.{ ZMongoClient, ZMongoCollection }
import zio.{ Task, ZLayer }

trait TotpMongodbService extends MongodbService[TotpToken]

final case class TotpMongodbServiceImpl(mongodbConfig: MongodbConfig, client: ZMongoClient)
    extends TotpMongodbService {
  override protected def collection: Task[ZMongoCollection[Document]] = for {
    database   <- client.getDatabase(mongodbConfig.database)
    collection <- database.getCollection("TOTPs")
  } yield collection
}

object TotpMongodbService {
  val live: ZLayer[MongodbConfig with ZMongoClient, Nothing, TotpMongodbServiceImpl] =
    ZLayer.fromFunction(TotpMongodbServiceImpl.apply _)
}
