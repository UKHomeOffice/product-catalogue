package cjp.catalogue.repository

import cjp.catalogue.utils.Logging
import com.mongodb.DBCollection
import com.novus.salat.global._
import com.novus.salat.dao._
import cjp.catalogue.mongo.{SalatRepository, MongoConnector}

import com.novus.salat._
import com.mongodb.casbah.Imports._
import cjp.catalogue.model.CatalogueAttributeDefinition


class AttributeRepository(mc: MongoConnector)(implicit ctx: Context)
  extends SalatRepository[CatalogueAttributeDefinition]("attribute", mc.db)
  with Logging {
  collection.ensureIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true))

  def findAllAttributes = logDurationOf("findAllAttributes") { findAll}

  def stageImport(attributes : List[CatalogueAttributeDefinition]) = {
    mc.db().getCollection("attribute_backup").drop()
    mc.db().getCollection("attribute_stage").drop()

    val stage = mc.db().getCollection("attribute_stage")
    stage.ensureIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true))
    stage.insert(attributes.map(_grater.asDBObject(_)):_*)
  }
  
  def renameStage = {
    mc.db().getCollection("attribute").rename("attribute_backup")
    mc.db().getCollection("attribute_stage").rename("attribute")
  }
}