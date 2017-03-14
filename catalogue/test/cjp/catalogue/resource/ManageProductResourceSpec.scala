package cjp.catalogue.resource

import java.util.concurrent.TimeUnit

import cjp.catalogue.resource.CjpFormats._
import akka.util.Timeout
import cjp.catalogue.model._
import cjp.catalogue.repository.ProductNameAlreadyUsedException
import cjp.catalogue.service.{ProductDto, _}
import cjp.catalogue.test.builder.ProductDtoBuilder
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import org.json4s.native.Serialization
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import cjp.catalogue.utils.GetableFutures._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import play.api.GlobalSettings
import play.api.http.{HeaderNames, Status}
import play.api.test._

class ManageProductResourceSpec extends WordSpec with BeforeAndAfter with PlayRunners with ResultExtractors with HeaderNames with Status with Matchers with MockitoSugar {
  implicit val timeout = Timeout(10L, TimeUnit.SECONDS)


  private val productService = mock[ProductManagementService]
  private val controller = new ManageProductResource(productService)
  private val generalVisitVisa = "general-visit-visa"
  val intAttribute = CatalogueAttributeDto(5, "", "eligibility", "some useful text", "www:google.cu.uk", List(EvidenceKind("ref", true, false)))
  val boolAttribute = CatalogueAttributeDto(true, "", "eligibility", "some useful text", "www:google.cu.uk", List(EvidenceKind("ref", true, false)))
  val boolAttributeWithRelatedProducts = CatalogueAttributeDto(true, "", "eligibility", "some useful text", "www:google.cu.uk", List(EvidenceKind("ref", true, false)), List(RelatedProductSummary("tier2-general", ""), RelatedProductSummary("tier2-sportsperson", "")))
  val decimalAttribute = CatalogueAttributeDto(BigDecimal(30.99), "", "eligibility", "some useful text", "www:google.cu.uk", List(EvidenceKind("ref", true, false)))
  private val product: ProductDto = {
    ProductDtoBuilder(
      name = Some(generalVisitVisa),
      title = "A Title",
      description = "A description",
      attributes = Map(
        "intAttribute" -> intAttribute,
        "boolAttribute" -> boolAttribute,
        "boolAttributeWithRelatedProducts" -> boolAttributeWithRelatedProducts,
        "decimalAttribute" -> decimalAttribute
      ),
      serviceAddOns = List(ServiceAddOn("addOnName", "addOnTitle", "addOnDescription", 12.5, 10)),
      tags = List("Tag1"))
  }

  object TestGlobal extends GlobalSettings {
    override def getControllerInstance[A](controllerClass: Class[A]): A =
      controller.asInstanceOf[A]
  }

  before {
    reset(productService)
  }


  "calling GET /manage/product" should {

    "return a list of product summaries" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val summary = ProductSummaryDto("foo", "bar", "foo bar", 1, DateTime.now, Nil)

        when(productService.getSummaries).thenReturn(List(summary))

        val result = controller.getSummaries.apply(FakeRequest())
        val json = contentAsJson(result)
        (json(0) \ "name").as[String] shouldBe summary.name
        (json(0) \ "title").as[String] shouldBe summary.title
        (json(0) \ "description").as[String] shouldBe summary.description
      }
    }
  }

  "calling GET /manage/product/general-visit-visa" should {
    "return general visit visa details" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getCurrent(generalVisitVisa)).thenReturn(Some(product))

        val result = controller.getCurrent(generalVisitVisa).apply(FakeRequest())

        status(result) should be(200)

        val responseJson = contentAsJson(result)
        (responseJson \ "name").as[String] should be(generalVisitVisa)
        (responseJson \ "title").as[String] should be(product.title)
        (responseJson \ "description").as[String] should be(product.description)
        (responseJson \ "attributes" \ "intAttribute" \ "value").as[Int] should be(intAttribute.value.asInstanceOf[Int])
        (responseJson \ "attributes" \ "intAttribute" \ "facet").as[String] should be(intAttribute.facet)
        (responseJson \ "attributes" \ "intAttribute" \ "referenceText").as[String] should be(intAttribute.referenceText)
        (responseJson \ "attributes" \ "intAttribute" \ "referenceUrl").as[String] should be(intAttribute.referenceUrl)
        (responseJson \ "attributes" \ "intAttribute" \ "evidenceKinds" \\ "reference")(0).as[String] should be(intAttribute.evidenceKinds.head.reference)
        (responseJson \ "attributes" \ "intAttribute" \ "evidenceKinds" \\ "mandatory")(0).as[Boolean] should be(intAttribute.evidenceKinds.head.mandatory)
        (responseJson \ "attributes" \ "intAttribute" \ "evidenceKinds" \\ "specified")(0).as[Boolean] should be(intAttribute.evidenceKinds.head.specified)
        (responseJson \ "attributes" \ "boolAttribute" \ "value").as[Boolean] should be(boolAttribute.value.asInstanceOf[Boolean])
        (responseJson \ "attributes" \ "decimalAttribute" \ "value").as[Double] should be(decimalAttribute.value.asInstanceOf[BigDecimal].toDouble)
        (responseJson \ "attributes" \ "boolAttributeWithRelatedProducts" \ "relatedProducts" \\ "name")(0).as[String] should be(boolAttributeWithRelatedProducts.relatedProducts.head.name)
      }

    }

    "return a 404 response when no product is found" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getCurrent(generalVisitVisa)).thenReturn(None)

        val result = controller.getCurrent(generalVisitVisa).apply(FakeRequest())

        status(result) should be(404)
      }
    }
  }

  "calling GET /manage/product/draft/general-visit-visa " should {

    "return 204 no content when there is no draft" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getDraft(generalVisitVisa)).thenReturn(None)

        val result = controller.getDraft(generalVisitVisa).apply(FakeRequest())

        status(result) should be(204)
      }
    }

    "return 200 when there is a draft" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getDraft(generalVisitVisa)).thenReturn(Some(product))

        val result = controller.getDraft(generalVisitVisa).apply(FakeRequest())

        status(result) should be(200)

        val json = contentAsJson(result)

        (json \ "name").as[String] should be(generalVisitVisa)
        (json \ "title").as[String] should be(product.title)
        (json \ "description").as[String] should be(product.description)
        (json \ "effectiveFrom").as[LocalDate] should be(product.effectiveFrom)
        (json \ "lastModified").as[String] should be(product.lastModified.toString(ISODateTimeFormat.dateTime.withZoneUTC))
        (json \ "serviceAddOns" \\ "title").map(_.as[String]).toList.head should be(product.serviceAddOns(0).title)
        (json \ "tags")(0).as[String] should be(product.tags.head)
        (json \ "title").as[String] should be(product.title)
        (json \ "version").asOpt[Int] should be(product.version)
      }
    }
  }

  "calling DELETE /manage/product/draft/general-visit-visa" should {
    "return 204 if the draft is successfully deleted" in {

      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val result = controller.deleteDraft(generalVisitVisa).apply(FakeRequest())

        status(result) should be(204)
      }

      verify(productService).deleteDraft(generalVisitVisa)
    }

    "return 404 if there is no draft for the product" in {
      doThrow(new IllegalArgumentException("Boom!")).when(productService).deleteDraft(generalVisitVisa)

      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val result = controller.deleteDraft(generalVisitVisa).apply(FakeRequest())

        status(result) should be(404)
      }
    }
  }

  "calling POST /product" should {

    "create a new product" in {

      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

        implicit val format = CjpFormats
        val productName: String = "product-name"
        val productDto = ProductDto(Some(productName), "product-title", "description", Map.empty, List.empty, List.empty, Some(1), effectiveFrom = LocalDate.now, productBlockList = List.empty)

        when(productService.create(productDto)).thenReturn(productName)

        val jsValue: JsValue = Json.parse(Serialization.write(productDto))
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")

        val response = controller.create()(request).get

        response.header.status should be(201)
        response.header.headers("Location") should be("/manage/product/product-name")

        // Setting the lastmodified as the TZ doesn't serialise consistently
        // But the lastmodified isn't used when writing the product, this is set by the service
      }
    }

    "return a 400 if the name has already been used" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

        implicit val format = CjpFormats
        val productName: String = "product-name"
        val productDto = ProductDto(Some(productName), "product-title", "description", Map.empty, List.empty, List.empty, Some(1), effectiveFrom = LocalDate.now, productBlockList = List.empty)

        when(productService.create(productDto)).thenThrow(new ProductNameAlreadyUsedException(productDto.name get))

        val jsValue: JsValue = Json.parse(Serialization.write(productDto))
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")
        val expectedMessage: String = s"""{"message":"title [${productDto.name get}] has already been used"}"""

        val response = controller.create()(request)

        status(response) should be(400)
        contentAsJson(response).toString() should be(expectedMessage)
      }
    }
  }

  "calling POST /manage/product/clone/general-visit-visa" should {

    "return a 400 if the product title is not provided" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

        implicit val format = CjpFormats
        val productName: String = "product-name"
        val productDto = ProductDto(Some(productName), "", "description", Map.empty, List.empty, List.empty, Some(1), effectiveFrom = LocalDate.now, productBlockList = List.empty)

        val jsValue: JsValue = Json.parse(Serialization.write(productDto))
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")
        val expectedMessage: String = s"""{"message":"title not provided"}"""

        val response = controller.cloneProduct(productName)(request)

        status(response) should be(400)
        contentAsString(response).toString() should be(expectedMessage)
      }
    }

    "return a 404 if the existing product cannot be found" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

        val productName = "general-visit-visa"
        val productDto = CloneProductDto("new title", "desc", LocalDate.now)
        val jsValue: JsValue = Json.parse(Serialization.write(productDto))
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")
        val expectedMessage: String = s"""{"message":"product name [$productName] not found"}"""

        when(productService.cloneProduct(productName, productDto)).thenReturn(None)

        val response = controller.cloneProduct(productName)(request)

        status(response) should be(404)
        contentAsString(response).toString() should be(expectedMessage)
      }
    }

    "return a 400 if the name has already been used" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

        val productName = "general-visit-visa"
        val productDto = CloneProductDto("new title", "desc", LocalDate.now)
        val jsValue: JsValue = Json.parse(Serialization.write(productDto))
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")
        val expectedMessage: String = s"""{"message":"title [$productName] has already been used"}"""

        when(productService.cloneProduct(productName, productDto)).thenThrow(new ProductNameAlreadyUsedException(productName))

        val response = controller.cloneProduct(productName)(request)

        status(response) should be(400)
        contentAsString(response).toString() should be(expectedMessage)
      }
    }

    "return a 201 when a clone has been successfully created" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val productName = "general-visit-visa"
        val productDto = CloneProductDto("new title", "desc", LocalDate.now)
        val jsValue: JsValue = Json.parse(Serialization.write(productDto))
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")

        when(productService.cloneProduct(productName, productDto)).thenReturn(Some("new-title"))

        val response = controller.cloneProduct(productName)(request)

        status(response) should be(201)
      }
    }
  }

    "calling PUT /manage/product/general-visit-visa" should {

      "update the exiting product" in {

        running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

          val productName: String = "product-name"
          val productDto = ProductDto(Some(productName), "new title", "description", Map.empty, List.empty, List.empty, Some(1), effectiveFrom = LocalDate.now, productBlockList = List.empty)
          val jsValue: JsValue = Json.parse(Serialization.write(productDto))
          val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")

          val response = controller.update(productName)(request).get

          response.header.status should be(204)
          verify(productService).update(productName, productDto)
        }
      }

    }

    "calling PUT /manage/product/missing" should {

      "return 400" in {
        running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

          val productName: String = "product-name"
          val productDto = ProductDto(Some(productName), "new title", "description", Map.empty, List.empty, List.empty, Some(1), effectiveFrom = LocalDate.now, productBlockList = List.empty)
          val jsValue: JsValue = Json.parse(Serialization.write(productDto))
          val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(jsValue).withHeaders("ContentType" -> "application/json")
          val expectedMessage: String = s"""{"message":"Product [$productName] not found"}"""

          when(productService.update(productName, productDto)).thenThrow(new ProductNotFoundException(productName))

          val response = controller.update(productName)(request)

          status(response) should be(404)
          contentAsString(response).toString() should be(expectedMessage)
        }
      }
    }

    "calling GET /manage/product/tags" should {
      "return all the tags" in {
        running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

          val tags: List[String] = List("t1", "t2")
          when(productService.getTags).thenReturn(tags)

          val response = controller.getTags()(FakeRequest().withHeaders("ContentType" -> "application/json"))

          status(response) should be(200)
          contentAsString(response).toString() should be("""["t1","t2"]""")
        }
      }
    }
}