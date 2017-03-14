package cjp.catalogue.service

import cjp.catalogue.model._
import cjp.catalogue.repository.{ProductTimelineRepository, ProductRepository}
import cjp.catalogue.utils.Logging
import com.mongodb.casbah.commons.Imports._
import org.bson.types.ObjectId


case class CatalogueData(attributes: List[CatalogueAttributeDefinition], products: Map[String, ProductExport])

case class ProductExport(versions: Map[String, ProductVersionExport], timeline: List[TimelineVersion])

case class ProductVersionExport(title: String,
                                description: String,
                                attributes: Map[String, CatalogueAttribute] = Map.empty,
                                serviceAddOns: List[ServiceAddOn] = Nil,
                                tags: List[String] = Nil,
                                productBlockList: List[ProductBlockList] = Nil)

object ProductVersionExport {
  implicit def persistedCatalogueProductToProductVersionExport(pcp: PersistedCatalogueProduct): ProductVersionExport = {
    ProductVersionExport(pcp.title, pcp.description, pcp.attributes, pcp.serviceAddOns, pcp.tags, pcp.productBlockList)
  }
}

class ExportImportService(productRepository: ProductRepository,
                          productTimeLineRepository: ProductTimelineRepository,
                          attributeService: AttributeService) extends Logging {

  def export: CatalogueData = {
    val products: Map[String, ProductExport] = productTimeLineRepository.findAll.foldLeft(Map.empty[String, ProductExport]) {
      (mp, tl) =>
        val versions = tl.versions.foldLeft(Map.empty[String, ProductVersionExport])((pm, v) => pm + (v.version.toString -> productRepository.findByNameAndVersion(tl.productName, v.version).get))
        mp + (tl.productName -> ProductExport(versions, tl.versions))
    }

    CatalogueData(attributeService.catalogueAttributes, products)
  }

  def importCatalogue(export: CatalogueData): Unit = {

    val (products, timeLines) = export.products.foldLeft((List.empty[PersistedCatalogueProduct], List.empty[ProductTimeline])) {
      case ((lspc, lspt), (pn, pex)) =>
        val products = pex.versions.map {
          case (version, pv) =>
            PersistedCatalogueProduct(ObjectId.get(), pn, version.toInt, pv.title, pv.description, pv.attributes, pv.serviceAddOns, pv.tags, productBlockList = pv.productBlockList)
        }
        (products.toList ::: lspc, ProductTimeline(productName = pn, versions = pex.timeline) :: lspt)
    }

    info("Importing attributes...")
    attributeService.importAttributes(export.attributes)


    info("Importing product timelines...")
    productTimeLineRepository.stageImport(timeLines)

    info("Importing products...")
    productRepository.stageImport(products)


    info("switching staged imports...")
    productTimeLineRepository.renameStage
    productRepository.renameStage

  }

}
