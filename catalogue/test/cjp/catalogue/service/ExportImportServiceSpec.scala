package cjp.catalogue.service

import cjp.catalogue.model.{CatalogueAttributeDefinition, PersistedCatalogueProduct, ProductTimeline, TimelineVersion}
import cjp.catalogue.repository.{ProductRepository, ProductTimelineRepository}
import cjp.catalogue.test.builder.{CatalogueProductBuilder, ServiceAddOnBuilder}
import org.joda.time.{DateTime, DateTimeUtils}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

class ExportImportServiceSpec extends WordSpec with BeforeAndAfter with Matchers with MockitoSugar {
  val productRepository = mock[ProductRepository]
  val productTimelineRepository = mock[ProductTimelineRepository]
  val attributeService = mock[AttributeService]

  val exportImportService = new ExportImportService(productRepository, productTimelineRepository, attributeService)
  val now = DateTime.now()
  before {
    reset(productRepository, productTimelineRepository)
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

  "export" should {
    "include attributes service addOns and all product versions referenced by time line" in {

      when(productTimelineRepository.findAll).thenReturn(List(ProductTimeline(productName = "A", versions = List(
        TimelineVersion(0, now.minusDays(20)),
        TimelineVersion(1, now)
      ))))
      val catalogueProduct0: PersistedCatalogueProduct = CatalogueProductBuilder(title = "0 title").toPersistedCatalogueProduct(0)
      val catalogueProduct1: PersistedCatalogueProduct = CatalogueProductBuilder(title = "1 title").toPersistedCatalogueProduct(1)
      val catalogueAttributes = List(CatalogueAttributeDefinition("attr", "", "very kind", false, "facet"))


      when(productRepository.findByNameAndVersion("A", 0)).thenReturn(Some(catalogueProduct0))
      when(productRepository.findByNameAndVersion("A", 1)).thenReturn(Some(catalogueProduct1))

      when(attributeService.catalogueAttributes).thenReturn(catalogueAttributes)

      exportImportService.export shouldBe CatalogueData(
        attributes = catalogueAttributes,
        products = Map("A" -> ProductExport(Map("0" -> catalogueProduct0, "1" -> catalogueProduct1), List(TimelineVersion(0, now.minusDays(20)), TimelineVersion(1, now))))
      )

    }
  }

}
