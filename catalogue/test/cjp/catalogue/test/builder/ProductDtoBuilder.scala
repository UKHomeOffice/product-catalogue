package cjp.catalogue.test.builder

import org.joda.time.{LocalDate, DateTime}

import cjp.catalogue.model._
import cjp.catalogue.service.{CatalogueAttributeDto, ProductDto}


object ProductDtoBuilder {
  def apply(name: Option[String] = Some("a-title"),
            title: String = "a title",
            description: String = "a description",
            attributes: Map[String, CatalogueAttributeDto] = Map.empty,
            serviceAddOns: List[ServiceAddOn] = Nil,
            tags: List[String] = Nil,
            version: Option[Int] = None,
            lastModified: DateTime = DateTime.now,
            effectiveFrom: LocalDate = LocalDate.now,
            productBlockList: List[ProductBlockList] = Nil) =
    ProductDto(
      name = name,
      title = title,
      description = description,
      attributes = attributes,
      serviceAddOns = serviceAddOns,
      tags = tags,
      version = version,
      lastModified = lastModified,
      effectiveFrom = effectiveFrom,
      productBlockList = productBlockList
  )
}