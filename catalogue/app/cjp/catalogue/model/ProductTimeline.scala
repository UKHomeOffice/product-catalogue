package cjp.catalogue.model

import org.joda.time.DateTime
import com.mongodb.casbah.commons.Imports.{ObjectId => Oid}
import org.bson.types.ObjectId

case class ProductTimeline(_id: Oid = ObjectId.get(), productName: String, versions: List[TimelineVersion] = Nil) {

  def findEffectiveVersion(effectiveAt: DateTime): Option[TimelineVersion] = versions.sortWith((a, b) => a.from.isAfter(b.from)).find(!_.from.isAfter(effectiveAt))

  def findDraftVersion(): Option[TimelineVersion] = versions.find(_.from.isAfter(DateTime.now))
}

case class TimelineVersion(version: Int, from: DateTime, to: Option[DateTime] = None)
