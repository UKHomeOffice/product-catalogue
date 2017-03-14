package cjp.catalogue.repository

import cjp.catalogue.model.{ProductTimeline, NewCatalogueProduct, PersistedCatalogueProduct}
import cjp.catalogue.utils.Logging
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.global._
import org.joda.time.DateTime
import cjp.catalogue.mongo.{SalatRepository, MongoConnector}

class ProductRepository (mc: MongoConnector)(implicit ctx: Context)
  extends SalatRepository[PersistedCatalogueProduct]("products", mc.db)
  with Logging {

  collection.ensureIndex(new BasicDBObject("tags", 1))
  collection.ensureIndex(MongoDBObject("name" -> 1) ++ MongoDBObject("version" -> 1), new BasicDBObject("unique", true))


  def stageImport(products : List[PersistedCatalogueProduct]) = {
    mc.db().getCollection("products_backup").drop()
    mc.db().getCollection("products_stage").drop()

    val stage = mc.db().getCollection("products_stage")
    stage.ensureIndex(new BasicDBObject("tags", 1))
    stage.ensureIndex(MongoDBObject("name" -> 1) ++ MongoDBObject("version" -> 1), new BasicDBObject("unique", true))
    stage.insert(products.map(_grater.asDBObject(_)):_*)
  }

  def renameStage = {
    mc.db().getCollection("products").rename("products_backup")
    mc.db().getCollection("products_stage").rename("products")
  }


  def createNewProduct(product: NewCatalogueProduct): PersistedCatalogueProduct = logDurationOf("createNewProduct") {
    findLatestVersion(product.name) match {
      case Some(_) => throw new ProductNameAlreadyUsedException(product.name)
      case None =>
        val persisted: PersistedCatalogueProduct = product.toPersistedCatalogueProduct()
        super.insert(persisted)
        persisted
    }
  }

  def addVersion(name: String, product: NewCatalogueProduct): Option[PersistedCatalogueProduct] = logDurationOf("addVersion") {
    findLatestVersion(name).flatMap{ latest =>
      val newVersion = product.toPersistedCatalogueProduct(latest.version + 1)
      super.insert(newVersion) map {
        _ => newVersion
      }
    }
  }

  def update(id: ObjectId, version: Int, product: NewCatalogueProduct): PersistedCatalogueProduct = logDurationOf("update") {
    val updatedVersion = product.toPersistedCatalogueProduct(version, id)
    super.update(byId(id), updatedVersion, upsert = false, multi = false, WriteConcern.Safe)
    updatedVersion
  }

  def delete(product: PersistedCatalogueProduct): WriteResult = logDurationOf("delete") {
    collection.remove(MongoDBObject("_id" -> product._id), WriteConcern.Safe)
  }

  def findByNameAndVersion(name: String, version: Int): Option[PersistedCatalogueProduct] = logDurationOf("findByNameAndVersion") {
    findOne(byNameAndVersion(name, version))
  }

  def findAllTags: List[String] = logDurationOf("findAllTags") {
    val nullString: String = null
    collection.distinct("tags", "tags" $ne nullString).toList.map (_.asInstanceOf[String]).sorted
  }

  def findAllAddOnNames: List[String] = logDurationOf("findAllAddOnNames") {
    collection.distinct("serviceAddOns.name").toList.map (_.asInstanceOf[String]).sorted
  }

  private[repository] def findLatestVersion(name:String) : Option[PersistedCatalogueProduct] =
    find(byName(name)).sort(MongoDBObject("version" -> -1)).limit(1).toList.headOption

  private def byId(id: ObjectId) =
    MongoDBObject("_id" -> id)

  private def byName(name: String) =
    MongoDBObject("name" -> name)

  private def byNameAndVersion(name: String, version: Int) =
    MongoDBObject("name" -> name) ++ MongoDBObject("version" -> version)
}
