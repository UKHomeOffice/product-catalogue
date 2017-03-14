package cjp.catalogue.test.builder

import cjp.catalogue.model.{ServiceAddOn, CatalogueAttribute, CataloguePublishedProduct}
import org.bson.types.ObjectId
import org.joda.time.DateTime

object CataloguePublishedProductBuilder {

  def apply(name: String = "a-title",
            title: String = "a title",
            description: String = "a description",
            attributes: Map[String, CatalogueAttribute] = Map.empty,
            serviceAddOns: List[ServiceAddOn] = Nil,
            version: Int = 1,
            updateId: Option[ObjectId] = None,
            effectiveFrom: DateTime = DateTime.now) =
    CataloguePublishedProduct(
      name = name,
      title = title,
      description = description,
      attributes = attributes,
      serviceAddOns = serviceAddOns,
      version = version,
      updateId = updateId,
      effectiveFrom = effectiveFrom
    )
}
