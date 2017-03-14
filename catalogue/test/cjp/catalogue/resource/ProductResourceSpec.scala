package cjp.catalogue.resource

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import cjp.catalogue.service._
import cjp.catalogue.test.builder.ProductDtoBuilder
import org.joda.time.format.ISODateTimeFormat
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import play.api.GlobalSettings
import play.api.http.{Status, HeaderNames}
import play.api.test._

import org.joda.time.{DateTimeZone, DateTime}


class ProductResourceSpec extends WordSpec with BeforeAndAfter with PlayRunners with ResultExtractors with HeaderNames with Status with Matchers with MockitoSugar {
  implicit val timeout = Timeout(10L, TimeUnit.SECONDS)

  private val productService = mock[ProductManagementService]
  private val attributeService = mock[AttributeService]
  private val controller = new ProductResource(productService, attributeService)
  private val generalVisitVisa = "general-visit-visa"
  private val product = ProductDtoBuilder()

  before {
    reset(productService)
  }

  object TestGlobal extends GlobalSettings {
    override def getControllerInstance[A](controllerClass: Class[A]): A =
      controller.asInstanceOf[A]
  }

  "calling GET /product/general-visit-visa  " should {
    "return general visit visa details" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getCurrent(generalVisitVisa)).thenReturn(Some(product))

        val result = controller.getLatest(generalVisitVisa).apply(FakeRequest())
        status(result) shouldBe  200
        contentType(result) shouldBe Some("application/json")
        val json = contentAsJson(result)
        (json \ "name").as[String] shouldBe product.name.get
        (json \ "title").as[String] shouldBe product.title
        (json \ "description").as[String] shouldBe product.description
      }
    }

    "return a 404 response when no current version is found" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getCurrent(generalVisitVisa)).thenReturn(None)
        val result = controller.getLatest(generalVisitVisa).apply(FakeRequest())
        status(result) shouldBe 404
      }
    }
  }

  "calling GET /product/{name}/draft" should {

    "return general visit visa details draft" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getDraft(generalVisitVisa)).thenReturn(Some(product))

        val result = controller.getDraft(generalVisitVisa).apply(FakeRequest())

        status(result) shouldBe  200
        contentType(result) shouldBe Some("application/json")
        val json = contentAsJson(result)
        (json \ "name").as[String] shouldBe product.name.get
        (json \ "title").as[String] shouldBe product.title
        (json \ "description").as[String] shouldBe product.description
      }
    }

    "return a 404 response when no draft is found" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        when(productService.getDraft(generalVisitVisa)).thenReturn(None)

        val result = controller.getDraft(generalVisitVisa).apply(FakeRequest())

        status(result) shouldBe  404
      }
    }
  }

  "calling GET /product/{name}/effectiveAt/{effectiveAt}" should {

    "return general visit visa details" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val effectiveAt = new DateTime(2015, 3, 31, 16, 0, DateTimeZone.UTC)
        when(productService.getEffectiveAt(generalVisitVisa, effectiveAt)).thenReturn(Some(product))

        val result = controller.getEffectiveAt(generalVisitVisa, "2015-03-31T16:00:00.000Z").apply(FakeRequest())

        status(result) shouldBe  200
      }
    }

    "return a 404 response when no effective product is found" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val effectiveAt = new DateTime(2015, 3, 31, 16, 0, DateTimeZone.UTC)
        when(productService.getEffectiveAt(generalVisitVisa, effectiveAt)).thenReturn(None)

        val result = controller.getEffectiveAt(generalVisitVisa, "2015-03-31T16:00:00.000Z").apply(FakeRequest())

        status(result) shouldBe  404
      }
    }
  }
}
