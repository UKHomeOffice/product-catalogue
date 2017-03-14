package cjp.catalogue.mongo

import com.mongodb.WriteConcern
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoCollection, MongoDB}
import com.novus.salat._
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId

abstract class SalatRepository[T <: CaseClass](val collectionName: String, getDb: () => MongoDB, mc: Option[MongoCollection] = None)(implicit manifest: Manifest[T], ctx: Context)
  extends SalatDAO[T, ObjectId](collection = mc.getOrElse(getDb()(collectionName))) {

  def findAll: List[T] = {
    find(MongoDBObject.empty).toList
  }

  def removeAll = {
    collection.remove(MongoDBObject.empty)
  }

  def drop() {
    collection.drop()
  }

  def save(bulk: List[T], wc: WriteConcern = WriteConcern.SAFE) {
    bulk.foreach {
      super.save(_, wc)
    }
  }

  def findById(id: String): Option[T] = {
    findOneById(new ObjectId(id))
  }

}

