package cjp.catalogue.service

import org.joda.time.DateTime

import cjp.catalogue.model.{PersistedCatalogueProduct}


case class ProductSummaryDto(name: String,
                             title: String,
                             description: String,
                             version: Int,
                             lastModified: DateTime,
                             tags: List[String])

object ProductSummaryDto {

  def apply(product: PersistedCatalogueProduct) = {

    new ProductSummaryDto(
      name = product.name,
      title = product.title,
      description = product.description,
      version = product.version,
      lastModified = product.lastModified,
      tags = product.tags
    )
  }

}