package cjp.catalogue.service

import cjp.catalogue.model._
import cjp.catalogue.repository.{ProductRepository, ProductTimelineRepository}
import cjp.catalogue.test.builder._
import com.mongodb.WriteResult
import org.bson.types.ObjectId
import org.joda.time.DateTimeZone.UTC
import org.joda.time.{LocalDate, DateTime, DateTimeUtils, LocalTime}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

class ProductManagementServiceSpec extends WordSpec with BeforeAndAfter with Matchers with MockitoSugar  {

  val productRepository = mock[ProductRepository]
  val productTimelineRepository = mock[ProductTimelineRepository]
  val attributeService = mock[AttributeService]
  val service = spy(new ProductManagementService(productRepository, productTimelineRepository, attributeService))

  before {
    reset(productRepository, productTimelineRepository)

    DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2014-08-01T12:00:00Z").getMillis)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

  "getSummaries" should {

    "should fetch active product as summaries" in {
      val version = 1
      val product = CatalogueProductBuilder(
        name = "name",
        title = "title",
        description = "some description",
        created = new DateTime(),
        tags = List("visa")).toPersistedCatalogueProduct(version)
      val timelineWithActiveProduct = ProductTimeline(productName = product.name, versions = List(TimelineVersion(product.version, DateTime.now.minusDays(7))))

      when(productTimelineRepository.findAll).thenReturn(List(timelineWithActiveProduct))
      when(productTimelineRepository.findByProductName(product.name)).thenReturn(Some(timelineWithActiveProduct))
      when(productRepository.findByNameAndVersion(product.name, product.version)).thenReturn(Some(product))

      service.getSummaries should be {
        List(ProductSummaryDto(
          name = product.name,
          title = product.title,
          description = product.description,
          version = version,
          lastModified = product.created,
          tags = product.tags
        ))
      }
    }

    "should fetch draft product if no active product is available" in {
      val version = 1
      val product = CatalogueProductBuilder(
        name = "name",
        title = "title",
        description = "some description",
        created = new DateTime()).toPersistedCatalogueProduct(version)
      val timelineWithDraft = ProductTimeline(productName = product.name, versions = List(TimelineVersion(product.version, DateTime.now.plusDays(7))))

      when(productTimelineRepository.findAll).thenReturn(List(timelineWithDraft))
      when(productTimelineRepository.findByProductName(product.name)).thenReturn(Some(timelineWithDraft))
      when(productRepository.findByNameAndVersion(product.name, product.version)).thenReturn(Some(product))

      service.getSummaries should be {
        List(ProductSummaryDto(
          name = product.name,
          title = product.title,
          description = product.description,
          version = version,
          lastModified = product.created,
          tags = Nil
        ))
      }
    }

    "should fetch active product if both active product and draft available" in {
      val version = 1
      val product = CatalogueProductBuilder(
        name = "name",
        title = "title",
        description = "some description",
        created = new DateTime()).toPersistedCatalogueProduct(version)
      val timelineWithActiveProductAndDraft = ProductTimeline(productName = product.name, versions = List(
        TimelineVersion(product.version, DateTime.now.minusDays(7)),
        TimelineVersion(product.version + 10, DateTime.now.plusDays(7)))
      )

      when(productTimelineRepository.findAll).thenReturn(List(timelineWithActiveProductAndDraft))
      when(productTimelineRepository.findByProductName(product.name)).thenReturn(Some(timelineWithActiveProductAndDraft))
      when(productRepository.findByNameAndVersion(product.name, product.version)).thenReturn(Some(product))

      service.getSummaries should be {
        List(ProductSummaryDto(
          name = product.name,
          title = product.title,
          description = product.description,
          version = version,
          lastModified = product.created,
          tags = Nil
        ))
      }
    }
  }

  private def returnEffectiveProduct(product: PersistedCatalogueProduct) = {
    val timeline = mock[ProductTimeline]
    val productVersion = TimelineVersion(version = 1, from = DateTime.now().minusDays(5))

    when(productTimelineRepository.findByProductName(product.name)).thenReturn(Some(timeline))
    when(timeline.findEffectiveVersion(any[DateTime])).thenReturn(Some(productVersion))
    when(productRepository.findByNameAndVersion(timeline.productName, productVersion.version)).thenReturn(Some(product))
  }

  private def currentVersion = CatalogueProductBuilder()

  def draftVersion = CatalogueProductBuilder()

  "getDraft" should {
    val productName = "a-title"

    "return None if timeline for product does not exist" in {

      when(productTimelineRepository.findByProductName(productName)).thenReturn(None)

      service.getDraft(productName) shouldBe None
    }

    "return None if there is no draft on the timeline" in {
      val timeline = ProductTimeline(productName = productName, versions = Nil)

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))
      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))
      service.getDraft(productName) should equal(None)
    }

    "return the draft version" in {
      val versionNumber = 2
      val draftVersion = TimelineVersion(versionNumber, DateTime.now.plusDays(9))
      val timeline = ProductTimeline(productName = productName, versions = List(draftVersion))
      val existingProduct = CatalogueProductBuilder().toPersistedCatalogueProduct(versionNumber)

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))
      when(productRepository.findByNameAndVersion(productName, versionNumber)).thenReturn(Some(existingProduct))
      when(attributeService.catalogueAttributes).thenReturn(Nil)

      val actualDraft = service.getDraft(productName).get
      actualDraft.version shouldBe Some(versionNumber)
      actualDraft.effectiveFrom shouldBe draftVersion.from.toLocalDate
    }
  }

  "update product" should {

    val productName = "a-title"

    "throw exception if the timeline for the product does not exist" in {

      val productDto = ProductDtoBuilder(effectiveFrom = DateTime.now.plusSeconds(20).toLocalDate)

      when(productTimelineRepository.findByProductName(productName)).thenReturn(None)

      intercept[ProductNotFoundException] {
        service.update(productName, productDto)
      }
    }

    "throw exception if no products are on the timeline" in {

      val productDto = ProductDtoBuilder(effectiveFrom = DateTime.now.plusSeconds(20).toLocalDate)
      val timeline = ProductTimeline(productName = productName, versions = Nil)

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))

      intercept[ProductNotFoundException] {
        service.update(productName, productDto)
      }
    }

    "throw exception if effective date is earlier than today" in {

      val updateInThePast = ProductDtoBuilder(title = "new title", effectiveFrom = DateTime.now().minusDays(2).toLocalDate)

      intercept[IllegalStateException] {
        service.update(productName, updateInThePast)
      }
    }

    "create a new draft version when there is no existing draft" in {

      val effectiveFrom = DateTime.now.plusDays(2).toLocalDate
      val productDto = ProductDtoBuilder(title = "new title", effectiveFrom = effectiveFrom)
      val productVersion = 1
      val timeline = ProductTimeline(productName = productName, versions = List(TimelineVersion(productVersion, DateTime.now.minusDays(7))))
      val persistedDraft = CatalogueProductBuilder.persistedProduct

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))
      when(productRepository.findByNameAndVersion(productName, productVersion)).thenReturn(Some(currentVersion.toPersistedCatalogueProduct()))
      when(productRepository.addVersion(equalTo(productName), any[NewCatalogueProduct])).thenReturn(Some(persistedDraft))

      service.update(productName, productDto)

      val pCapture = ArgumentCaptor.forClass(classOf[NewCatalogueProduct])
      verify(productRepository).addVersion(equalTo(productName), pCapture.capture())
      verify(productTimelineRepository).setDraftProduct(productName, persistedDraft.version, effectiveFrom.toDateTime(new LocalTime(0, 0), UTC))

      pCapture.getValue.title should be("new title")
    }

    "update the existing draft using the serviceAddOns from the live product" in {

      val effectiveFrom = DateTime.now.plusDays(2).toLocalDate
      val productDto = ProductDtoBuilder(title = "new title", effectiveFrom = effectiveFrom)
      val draftProduct = draftVersion.toPersistedCatalogueProduct()
      val activeProductVersion = 1
      val draftProductVersion = 5
      val timeline = ProductTimeline(productName = productName, versions = List(
        TimelineVersion(activeProductVersion, DateTime.now.minusDays(7)),
        TimelineVersion(draftProductVersion, DateTime.now.plusDays(7)))
      )

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))
      when(productRepository.findByNameAndVersion(productName, activeProductVersion)).thenReturn(Some(currentVersion.toPersistedCatalogueProduct()))
      when(productRepository.findByNameAndVersion(productName, draftProductVersion)).thenReturn(Some(draftProduct))

      service.update(productName, productDto)

      val pCapture = ArgumentCaptor.forClass(classOf[NewCatalogueProduct])
      verify(productRepository).update(equalTo(draftProduct._id), equalTo(draftProduct.version), pCapture.capture())
      verify(productTimelineRepository).updateDraftProduct(productName, draftProduct.version, effectiveFrom.toDateTime(new LocalTime(0, 0), UTC))

      pCapture.getValue.serviceAddOns shouldBe currentVersion.serviceAddOns
    }

    "update the existing draft and the timeline even when there is no live product" in {

      val effectiveFrom = DateTime.now.plusDays(2).toLocalDate
      val productDto = ProductDtoBuilder(title = "new title", effectiveFrom = effectiveFrom)
      val draftProduct = draftVersion.toPersistedCatalogueProduct()
      val timeline = ProductTimeline(productName = productName, versions = List(
        TimelineVersion(draftProduct.version, DateTime.now.plusDays(7)))
      )

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))
      when(productRepository.findByNameAndVersion(productName, draftProduct.version)).thenReturn(Some(draftProduct))

      service.update(productName, productDto)

      val pCapture = ArgumentCaptor.forClass(classOf[NewCatalogueProduct])
      verify(productRepository).update(equalTo(draftProduct._id), equalTo(draftProduct.version), pCapture.capture())
      verify(productTimelineRepository).updateDraftProduct(productName, draftProduct.version, effectiveFrom.toDateTime(new LocalTime(0, 0), UTC))

      pCapture.getValue.title should be("new title")
    }
  }

  "delete draft version" should {

    val productName = "name"

    "throw exception if there is no timeline for the product" in {

      when(productTimelineRepository.findByProductName(productName)).thenReturn(None)

      val exception = intercept[IllegalArgumentException] {
        service.deleteDraft(productName)
      }

      exception.getMessage should include(s"No draft exists for product [${productName}]")
    }

    "throw exception if there is no draft" in {
      val timeline = ProductTimeline(productName = productName, versions = Nil)

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))

      val exception = intercept[IllegalArgumentException] {
        service.deleteDraft(productName)
      }

      exception.getMessage should include(s"No draft exists for product [${productName}]")
    }

    "remove the draft" in {
      val catalogueProduct = draftVersion.toPersistedCatalogueProduct()
      val timeline = ProductTimeline(productName = catalogueProduct.name, versions = List(TimelineVersion(catalogueProduct.version, DateTime.now.plusDays(7))))

      when(productTimelineRepository.findByProductName(catalogueProduct.name)).thenReturn(Some(timeline))
      when(productRepository.findByNameAndVersion(catalogueProduct.name, catalogueProduct.version)).thenReturn(Some(catalogueProduct))
      when(productRepository.delete(catalogueProduct)).thenReturn(mock[WriteResult])

      service.deleteDraft(catalogueProduct.name)

      verify(productRepository).delete(catalogueProduct)
    }
  }

  "create" should {
    "persist a new product, create a timeline for it and set it as the active product" in {

      def toDateTime(localDate: LocalDate) = localDate.toDateTime(new LocalTime(0, 0), UTC)

      val productDto = ProductDtoBuilder(
        title = "The Title",
        tags = List("tag1", "tag2")
      )
      val createdProduct = PersistedCatalogueProduct(ObjectId.get, "newProduct", 1, productDto.title, "description", Map.empty, List.empty, Nil, DateTime.now, DateTime.now)
      val serviceAddOns = List("a1", "a2")
      val expectedName = "the-title"
      when(productRepository.createNewProduct(any[NewCatalogueProduct])).thenReturn(createdProduct)

      service.create(productDto) should be(expectedName)

      val captor = ArgumentCaptor.forClass(classOf[NewCatalogueProduct])
      verify(productRepository).createNewProduct(captor.capture())
      verify(productTimelineRepository).createNewTimeline(createdProduct.name)
      verify(productTimelineRepository).setActiveProduct(createdProduct.name, createdProduct.version, toDateTime(productDto.effectiveFrom))

      captor.getValue.name should be(expectedName)
    }
  }

  "cloneProduct" should {
    val existingProductName = "existing"
    val newProductName = "new"
    val description = "description"
    val effectiveFrom = LocalDate.now
    val cloneProduct = CloneProductDto(newProductName, description, effectiveFrom)

    "return none if the existing product cannot be found" in {
      doReturn(None).when(service).getCurrent(existingProductName)
      val result = service.cloneProduct(existingProductName, cloneProduct)
      result should be(None)
    }

    "return the name of the cloned product when successful" in {
      val productDto = ProductDtoBuilder()

      doReturn(Some(productDto)).when(service).getCurrent(existingProductName)
      val captor = ArgumentCaptor.forClass(classOf[ProductDto])
      doReturn(newProductName).when(service).create(captor.capture())

      val result = service.cloneProduct(existingProductName, cloneProduct)
      result should be(Some(newProductName))

      val cloned = captor.getValue
      cloned.version should be(None)
      cloned.effectiveFrom should be(effectiveFrom)
      cloned.name should be(None)
      cloned.title should be(newProductName)
      cloned.description should be(description)
    }
  }

  "validateFacet" should {
    "throw IlegalArgumentException when facet contains attribute of the wrong kind" in {
      implicit def finder = {
        (a: String, b: String) => Option(CatalogueAttributeDefinition("", "", "Decimal", false, ""))
      }

      val facet = Map(
        "invalidType" -> CatalogueAttributeDto(true, "", "ref text", "ref/url", "f1", Nil)
      )

      intercept[IllegalArgumentException] {
        service.transformAndValidate(facet)
      }
    }

    "throw IlegalArgumentException when facet contains attribute with the wrong evidence kinds" in {
      implicit def finder = {
        (a: String, b: String) => Option(CatalogueAttributeDefinition("", "", "Boolean", false, "", List("docRef")))
      }
      val facet = Map(
        "invalidEvidence" -> CatalogueAttributeDto(true, "", "ref text", "ref/url", "f1", List(EvidenceKind("wrongDocRef", true, false)))
      )

      intercept[IllegalArgumentException] {
        service.transformAndValidate(facet)
      }
    }

    "return true when facet contains valid attributes" in {

      implicit def finder = {
        (f: String, a: String) =>
          (f, a) match {
            case ("f1", "bool") => Some(CatalogueAttributeDefinition("", "", "Boolean", false, ""))
            case ("f1", "int") => Some(CatalogueAttributeDefinition("", "", "Integer", false, ""))
            case ("f1", "Decimal") => Some(CatalogueAttributeDefinition("", "", "Decimal", false, ""))
          }
      }

      val facetIn = Map(
        "bool" -> CatalogueAttributeDto(true, "", "f1", "ref text", "ref/url", Nil),
        "int" -> CatalogueAttributeDto(BigInt(1), "", "f1", "ref text", "ref/url", Nil),
        "Decimal" -> CatalogueAttributeDto(BigDecimal(99.9), "", "f1", "ref text", "ref/url", Nil)
      )

      val facet = Map(
        "bool" -> CatalogueBooleanProductAttribute(true, "", "f1", "ref text", "ref/url", Nil),
        "int" -> CatalogueIntegerProductAttribute(1, "", "f1", "ref text", "ref/url", Nil),
        "Decimal" -> CatalogueBigDecimalProductAttribute(99.9, "", "f1", "ref text", "ref/url", Nil)
      )

      service.transformAndValidate(facetIn) should be(facet)

    }
  }

  "validateAttribute" should {
    "return true when the attribute matches the definition" in {
      val definition = CatalogueAttributeDefinition("name", "", "Boolean", false, "facet", List("docRef1", "docRef2"))
      val attribute = CatalogueBooleanProductAttribute(true, "", "", "", "facet", List(EvidenceKind("docRef1", true, true), EvidenceKind("docRef2", false, false)))
      service.validateAttribute(definition, attribute) should be(true)
    }

    "return false when the attribute kind does not match the definition" in {
      val definition = CatalogueAttributeDefinition("name", "", "Boolean", false, "facet", List("docRef1", "docRef2"))
      val attribute = CatalogueIntegerProductAttribute(99, "", "", "", "facet", List(EvidenceKind("docRef1", true, true), EvidenceKind("docRef2", false, false)))
      service.validateAttribute(definition, attribute) should be(false)
    }

    "return true when the attribute evidence kinds are a subset of the definition" in {
      val definition = CatalogueAttributeDefinition("name", "", "Boolean", false, "facet", List("docRef1", "docRef2"))
      val attribute = CatalogueBooleanProductAttribute(true, "", "", "", "facet", List(EvidenceKind("docRef1", true, true)))
      service.validateAttribute(definition, attribute) should be(true)
    }

    "return false when the attribute evidence kinds are not a subset of the definition" in {
      val definition = CatalogueAttributeDefinition("name", "", "Boolean", false, "facet", List("docRef1", "docRef2"))
      val attribute = CatalogueBooleanProductAttribute(true, "", "", "", "facet", List(EvidenceKind("docRef3", true, true)))
      service.validateAttribute(definition, attribute) should be(false)
    }
  }

  "transformAndValidate" should {
    "throw an illegal argument exception with invalid attributes" in {
      when(attributeService.finder) thenReturn {
        (a: String, b: String) => None
      }

      val attributes = Map(
        "invalidAttribute" -> CatalogueAttributeDto(true, "", "ref text", "ref/url", "identity", Nil)
      )

      intercept[IllegalArgumentException] {
        service.transformAndValidate(attributes)(attributeService.finder)
      }
    }

    "should convert an integer attribute to a decimal attribute" in {
      when(attributeService.finder) thenReturn {
        (a: String, b: String) => Option(CatalogueAttributeDefinition("xx", "", "Decimal", false, "identity"))
      }

      val attributes = Map(
        "xx" -> CatalogueAttributeDto(BigInt(123), "", "identity", "ref text", "ref/url", Nil)
      )
      val result = service.transformAndValidate(attributes)(attributeService.finder)

      val expected = Map("xx" -> CatalogueBigDecimalProductAttribute(BigDecimal(123.0), "", "identity", "ref text", "ref/url", Nil))

      result shouldBe(expected)
    }
  }

  "getEffectiveAt" should {
    "return product at given date" in {

      val product = CatalogueProductBuilder(
        name = "name",
        title = "title",
        description = "some description",
        created = new DateTime()).toPersistedCatalogueProduct(1)
      val effectiveAt = DateTime.now

      val effectiveVersion = TimelineVersion(1, effectiveAt.minusDays(4))
      val timeline = ProductTimeline(productName = product.name, versions = List(
        effectiveVersion,
        TimelineVersion(2, DateTime.now.plusDays(2))
      ))

      when(productTimelineRepository.findByProductName(product.name)).thenReturn(Some(timeline))
      when(productRepository.findByNameAndVersion(product.name, effectiveVersion.version)).thenReturn(Some(product))
      when(attributeService.catalogueAttributes).thenReturn(Nil)

      service.getEffectiveAt(product.name, effectiveAt) shouldBe Some(
        ProductDto(
          name = Some(product.name),
          title = product.title,
          description = product.description,
          version = Some(effectiveVersion.version),
          lastModified = product.created,
          tags = Nil,
          effectiveFrom = effectiveVersion.from.toLocalDate,
          productBlockList = product.productBlockList
        ))
    }

    "return None if product does not exist" in {
      val productName = "productName"
      val effectiveAt = DateTime.now.minusDays(7)
      val timeline = ProductTimeline(productName = productName, versions = List(
        TimelineVersion(2, effectiveAt)
      ))

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))
      when(productRepository.findByNameAndVersion(productName, 2)).thenReturn(None)

      service.getEffectiveAt(productName, effectiveAt) shouldBe None
    }

    "return None if product timeline does not exist for given product" in {
      val productName = "productName"

      when(productTimelineRepository.findByProductName(productName)).thenReturn(None)

      service.getEffectiveAt(productName, DateTime.now) shouldBe None
    }

    "return None if product does not exist on the timeline at given date" in {
      val productName = "productName"
      val effectiveAt = DateTime.now.minusDays(7)
      val timeline = ProductTimeline(productName = productName, versions = List(
        TimelineVersion(1, effectiveAt.plusDays(2))
      ))

      when(productTimelineRepository.findByProductName(productName)).thenReturn(Some(timeline))

      service.getEffectiveAt(productName, effectiveAt) shouldBe None
    }

    "fetch a product with an attribute having 'validValues' defined on it" in {
      val someName = "someName"
      val serviceAddOnName = "some-service-add-on"
      val serviceAddOn = ServiceAddOnBuilder(name = serviceAddOnName)

      val product = CatalogueProductBuilder(
        name = someName,
        title = "second version",
        serviceAddOns = List(serviceAddOn),
        attributes = Map("englishRequired" -> CatalogueIntegerProductAttribute(200, "label", "facet", "referenceText", "refUrl"))
      )

      returnEffectiveProduct(product.toPersistedCatalogueProduct())

      when(attributeService.catalogueAttributes).thenReturn(List(CatalogueAttributeDefinition("englishRequired", "label", "", false, "facet",Nil,Map("A1" -> 1, "A2"->2, "B1" -> 3))))


      val attributes: Map[String, CatalogueAttributeDto] = service.getEffectiveAt(someName, DateTime.now).get.attributes
      attributes("englishRequired").validValues should be(Map("A1" -> 1, "A2"->2, "B1" -> 3))

    }

    "fetch attribute with related products" in {
      val product = CatalogueProductBuilder(
        name = "some-product",
        title = "some version",
        attributes = Map("canSwitch" -> CatalogueBooleanProductAttribute(true, "label", "facet", "referenceText", "refUrl", Nil, List("related-product")))
      ).toPersistedCatalogueProduct()
      val productTimeline = ProductTimeline(productName = product.name, versions = List(TimelineVersion(product.version, from = DateTime.now.minusDays(7))))

      val relatedProduct = CatalogueProductBuilder(
        name = "related-product",
        title = "Related product"
      ).toPersistedCatalogueProduct()
      val relatedProductTimeline = ProductTimeline(productName = relatedProduct.name, versions = List(TimelineVersion(relatedProduct.version, from = DateTime.now.minusDays(7))))

      when(productTimelineRepository.findByProductName(product.name)).thenReturn(Some(productTimeline))
      when(productTimelineRepository.findByProductName(relatedProduct.name)).thenReturn(Some(relatedProductTimeline))
      when(productRepository.findByNameAndVersion(product.name, product.version)).thenReturn(Some(product))
      when(productRepository.findByNameAndVersion(relatedProduct.name, relatedProduct.version)).thenReturn(Some(relatedProduct))
      when(attributeService.catalogueAttributes).thenReturn(List(CatalogueAttributeDefinition("canSwitch", "label", "", true, "facet", Nil, Map())))

      val attributes: Map[String, CatalogueAttributeDto] = service.getEffectiveAt("some-product", DateTime.now).get.attributes
      attributes("canSwitch").relatedProducts should be(List(RelatedProductSummary("related-product", "Related product")))
    }
  }


}