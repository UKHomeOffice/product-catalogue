package cjp.catalogue.test.builder

import cjp.catalogue.model._
import cjp.catalogue.service._
import org.joda.time.DateTime

object CatalogueAttributeDefBuilder {

  def apply(kind: String = "Integer") = {
    CatalogueAttributeDefinition(
      name = "attrName",
      label = "attrLabel",
      kind = kind,
      hasRelatedProducts = false,
      facet = "attrFacet",
      evidenceKinds = List(),
      validValues = Map()
    )
  }
}


object ProductVersionExportBuilder {

  def apply(
            title: String = "a title",
            description: String = "a description",
            attributes: Map[String, CatalogueAttribute] = Map.empty,
            serviceAddOns: List[ServiceAddOn] = Nil,
            tags: List[String] = Nil,
            productBlockList: List[ProductBlockList] = Nil
             ) =

    ProductVersionExport(
      title = title,
      description = description,
      attributes = attributes,
      serviceAddOns = serviceAddOns,
      tags = tags,
      productBlockList = productBlockList
    )

}


object ProductExportBuilder {

  def apply(productVersion: List[ProductVersionExport] = List(ProductVersionExportBuilder())) = {


    val (versions, timeline) = productVersion.zipWithIndex.foldLeft(Map.empty[String, ProductVersionExport], List.empty[TimelineVersion]) {
      case ((vrs, timls), (pv, index)) =>
        (vrs + (index.toString -> pv), TimelineVersion(index, DateTime.now().plusDays(index)) :: timls)
    }

    ProductExport(
      versions = versions,
      timeline = timeline
    )
  }

  def withTimeLine(timeLine: TimelineVersion) = {
    apply().copy(timeline = List(timeLine))
  }
}

object CatalogueExportBuilder {

  def apply(attributes: List[CatalogueAttributeDefinition] = List(CatalogueAttributeDefBuilder()),
            products: Map[String, ProductExport] = Map("tier2" -> ProductExportBuilder())) = {

    CatalogueData(
      attributes = attributes,
      products = products
    )
  }

}
