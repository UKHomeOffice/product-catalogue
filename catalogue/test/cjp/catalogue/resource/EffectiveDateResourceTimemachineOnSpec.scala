package cjp.catalogue.resource

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.joda.time.format.ISODateTimeFormat
import org.json4s.JsonAST.JString
import org.mockito.Mockito._
import cjp.catalogue.repository.EffectiveDateRepository
import org.joda.time.{DateTimeZone, DateTime}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfter, WordSpec}
import play.api.GlobalSettings
import play.api.http.{Status, HeaderNames}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test._

class EffectiveDateResourceTimemachineOnSpec extends WordSpec with BeforeAndAfter with PlayRunners with ResultExtractors with HeaderNames with Status with Matchers with MockitoSugar {
  implicit val timeout = Timeout(10L, TimeUnit.SECONDS)

  private val effectiveDateRepository = mock[EffectiveDateRepository]
  private val controller = new EffectiveDateResource(effectiveDateRepository, enableTimemachine = true)

  object TestGlobal extends GlobalSettings {
    override def getControllerInstance[A](controllerClass: Class[A]): A =
      controller.asInstanceOf[A]
  }

  before {
    reset(effectiveDateRepository)
  }

  "calling PUT /manage/effectiveDate" should {
    "set the effective date and return 204 if timemachine feature switch enabled" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {

        val date = "2015-04-10T14:12:57.159Z"
        val json = Json.parse(s"""{"effectiveDate":"${date}"}""")
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(json).withHeaders("ContentType" -> "application/json")

        val expectedDate = DateTime.parse(date)

        val controller = new EffectiveDateResource(effectiveDateRepository, enableTimemachine = true)
        val result = controller.setEffectiveDate.apply(request)

        status(result) should be(204)
        verify(effectiveDateRepository).set(expectedDate)
      }
    }
   }

  "calling DELETE /manage/effectiveDate" should {
    "reset the effective date and return 204 if timemachine feature switch enabled" in {

      val result = controller.resetEffectiveDate.apply(FakeRequest())
      status(result) should be(204)

      verify(effectiveDateRepository).reset
    }
  }
}
