package cjp.catalogue.test.builder

import cjp.catalogue.model._
import org.bson.types.ObjectId
import org.joda.time.DateTime

object CatalogueProductBuilder {

  def apply(_id: ObjectId = ObjectId.get,
            name: String = "a-title",
            title: String = "a title",
            description: String = "a description",
            attributes: Map[String, CatalogueAttribute] = Map.empty,
            serviceAddOns: List[ServiceAddOn] = Nil,
            tags: List[String] = Nil,
            created: DateTime = DateTime.now,
            lastModified: DateTime = DateTime.now,
            version: Int = 0) =

    NewCatalogueProduct(
      name = name,
      title = title,
      description = description,
      attributes = attributes,
      serviceAddOns = serviceAddOns,
      tags = tags,
      created = created,
      lastModified = lastModified
    )

  def newProduct : NewCatalogueProduct = CatalogueProductBuilder()
  def persistedProduct : PersistedCatalogueProduct = CatalogueProductBuilder().toPersistedCatalogueProduct(0)
}
