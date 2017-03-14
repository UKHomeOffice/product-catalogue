package cjp.catalogue.repository

import cjp.catalogue.mongo.SalatContext
import cjp.catalogue.mongo.MongoConnector

trait MongoSpecSupport {

  private val databaseName = "cataloguereptest"

  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?maxPoolSize=20&waitqueuemultiple=10"

  implicit val mongoConnectorForTest = new MongoConnector(mongoUri)

  implicit val defaultSalatContext = SalatContext.defaultCtx
}
