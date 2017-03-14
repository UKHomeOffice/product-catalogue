package cjp.catalogue.service

import cjp.catalogue.model.{ProductBlockList, ServiceAddOn}
import org.joda.time.{LocalDate, DateTime}

case class ProductDto(name: Option[String] = None,
                      title: String,
                      description: String,
                      attributes: Map[String, CatalogueAttributeDto] = Map.empty,
                      serviceAddOns: List[ServiceAddOn] = Nil,
                      tags: List[String],
                      version: Option[Int],
                      lastModified: DateTime = DateTime.now,
                      effectiveFrom: LocalDate,
                      productBlockList: List[ProductBlockList]) {
}
