package cjp.catalogue.model

import com.mongodb.casbah.commons.Imports.{ObjectId => Oid}
import org.bson.types.ObjectId

import org.joda.time.DateTime

case class PersistedCatalogueProduct(_id: Oid,
                                     name: String,
                                     version: Int = 0,
                                     title: String,
                                     description: String,
                                     attributes: Map[String, CatalogueAttribute] = Map.empty,
                                     serviceAddOns: List[ServiceAddOn] = Nil,
                                     tags: List[String] = Nil,
                                     created: DateTime = DateTime.now(),
                                     lastModified: DateTime = DateTime.now(),
                                     productBlockList: List[ProductBlockList] = Nil)

case class NewCatalogueProduct(name: String,
                               title: String,
                               description: String,
                               attributes: Map[String, CatalogueAttribute] = Map.empty,
                               serviceAddOns: List[ServiceAddOn] = Nil,
                               tags: List[String] = Nil,
                               created: DateTime,
                               lastModified: DateTime,
                               productBlockList: List[ProductBlockList] = Nil) {

  def toPersistedCatalogueProduct(version: Int = 0, id: ObjectId = ObjectId.get()) =
    PersistedCatalogueProduct(
      _id = id,
      name = name,
      version = version,
      title = title,
      description = description,
      attributes = attributes,
      serviceAddOns = serviceAddOns,
      tags = tags,
      created = created,
      lastModified = lastModified,
      productBlockList = productBlockList
    )
}


