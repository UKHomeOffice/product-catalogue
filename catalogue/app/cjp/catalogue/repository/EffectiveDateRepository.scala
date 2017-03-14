package cjp.catalogue.repository

import cjp.catalogue.utils.Logging
import com.novus.salat.Context
import com.novus.salat.global._
import cjp.catalogue.mongo.{SalatRepository, MongoConnector}
import org.joda.time.DateTime
import com.mongodb.casbah.commons.MongoDBObject

case class EffectiveDateTime(dateTime: DateTime)

class EffectiveDateRepository(mc: MongoConnector)(implicit ctx: Context)
  extends SalatRepository[EffectiveDateTime]("effectiveDate", mc.db)
  with Logging {

  def find = logDurationOf("find") { findAll.headOption.map(_.dateTime) }

  def set(dateTime: DateTime) = logDurationOf("set") { super.update(MongoDBObject(), MongoDBObject("dateTime" -> dateTime), true) }

  def reset = logDurationOf("reset") { removeAll }
}