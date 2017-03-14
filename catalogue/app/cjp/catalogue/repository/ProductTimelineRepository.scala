package cjp.catalogue.repository

import cjp.catalogue.model.{ServiceAddOn, ProductTimeline, TimelineVersion}
import cjp.catalogue.utils.Logging
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.global._
import org.joda.time.DateTime
import cjp.catalogue.mongo.{SalatRepository, MongoConnector}

class ProductTimelineAlreadyExistsException(name: String) extends RuntimeException(s"Timeline for product $name already exists")

class ProductTimelineDoesNotExistException(name: String) extends RuntimeException(s"Timeline for product $name does not exist")

class ProductTimelineRepository(mc: MongoConnector)(implicit ctx: Context)
  extends SalatRepository[ProductTimeline]("productTimelines", mc.db)
  with Logging {

  collection.ensureIndex(MongoDBObject("productName" -> 1), new BasicDBObject("unique", true))

  def createNewTimeline(productName: String): ProductTimeline = logDurationOf("createNewTimeline") {
    findByProductName(productName) match {
      case Some(_) => throw new ProductTimelineAlreadyExistsException(productName)
      case None =>
        val timeline = ProductTimeline(productName = productName)
        super.insert(timeline)
        timeline
    }
  }


  def stageImport(timeLines : List[ProductTimeline]) = {
    mc.db().getCollection("productTimelines_backup").drop()
    mc.db().getCollection("productTimelines_stage").drop()

    val stage = mc.db().getCollection("productTimelines_stage")
    stage.ensureIndex(new BasicDBObject("productName", 1), new BasicDBObject("unique", true))
    stage.insert(timeLines.map(_grater.asDBObject(_)):_*)
  }

  def renameStage = {
    mc.db().getCollection("productTimelines").rename("productTimelines_backup")
    mc.db().getCollection("productTimelines_stage").rename("productTimelines")
  }


  def setActiveProduct(productName: String, productVersion: Int, effectiveFrom: DateTime = DateTime.now): WriteResult = {

    findByProductName(productName) match {

      case Some(timeline) =>
        if (timeline.versions.size > 0) {
          throw new IllegalArgumentException(s"Cannot set active product for [$productName] as it can only be set on an empty timeline")
        }
        val updatedTimeline = timeline.copy(versions = List(TimelineVersion(productVersion, effectiveFrom)))
        super.update(byProductName(timeline.productName), updatedTimeline, upsert = false, multi = false, WriteConcern.Safe)

      case None => throw new ProductTimelineDoesNotExistException(productName)
    }
  }

  def setDraftProduct(productName: String, productVersion: Int, effectiveFrom: DateTime): WriteResult = {
    findByProductName(productName) match {
      case Some(timeline) =>
        val updatedTimeline = timeline.copy(versions = timeline.versions ++ List(TimelineVersion(productVersion, effectiveFrom)))
        super.update(byProductName(timeline.productName), updatedTimeline, upsert = false, multi = false, WriteConcern.Safe)
      case None => throw new ProductTimelineDoesNotExistException(productName)
    }
  }

  def updateDraftProduct(productName: String, productVersion: Int, effectiveFrom: DateTime): WriteResult = {
    findByProductName(productName) match {

      case Some(timeline) =>
        val currentDraft = timeline.findDraftVersion().getOrElse{
          throw new IllegalStateException(s"Draft does not exist on the timeline for product with name [$productName]")
        }
        val newDraft = TimelineVersion(productVersion, effectiveFrom)
        val updatedTimeline = timeline.copy(versions = timeline.versions.map {
          case `currentDraft` => newDraft
          case other => other
        })
        super.update(byProductName(timeline.productName), updatedTimeline, upsert = false, multi = false, WriteConcern.Safe)

      case None => throw new ProductTimelineDoesNotExistException(productName)
    }
  }

  def findByProductName(productName: String): Option[ProductTimeline] = find(byProductName(productName)).limit(1).toList.headOption

  private def byProductName(productName: String) = MongoDBObject("productName" -> productName)
}
