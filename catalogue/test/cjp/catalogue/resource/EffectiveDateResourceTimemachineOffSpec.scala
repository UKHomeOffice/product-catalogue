package cjp.catalogue.resource

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.joda.time.DateTime
import org.mockito.Mockito._
import cjp.catalogue.repository.EffectiveDateRepository
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfter, WordSpec}
import play.api.GlobalSettings
import play.api.http.{Status, HeaderNames}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, AnyContentAsText}
import play.api.test._

class EffectiveDateResourceTimemachineOffSpec extends WordSpec with BeforeAndAfter with PlayRunners with ResultExtractors with HeaderNames with Status with Matchers with MockitoSugar {
  implicit val timeout = Timeout(10L, TimeUnit.SECONDS)

  private val effectiveDateRepository = mock[EffectiveDateRepository]
  private val controller = new EffectiveDateResource(effectiveDateRepository, enableTimemachine = false)

  object TestGlobal extends GlobalSettings {
    override def getControllerInstance[A](controllerClass: Class[A]): A =
      controller.asInstanceOf[A]
  }

  before {
    reset(effectiveDateRepository)
  }

  "calling PUT /manage/effectiveDate" should {
    "return 403 if timemachine feature switch is disabled" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val json = Json.parse("""{"effectiveDate":"2015-04-10T14:12:57.159Z"}""")
        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(json).withHeaders("ContentType" -> "application/json")

        val result = controller.setEffectiveDate.apply(request)

        status(result) should be(403)
        verifyZeroInteractions(effectiveDateRepository)
      }
    }
  }

  "calling DELETE /manage/effectiveDate" should {
    "return 403 if timemachine feature switch is disabled" in {
      running(TestServer(18888, FakeApplication(withGlobal = Some(TestGlobal)))) {
        val result = controller.resetEffectiveDate.apply(FakeRequest())

        status(result) should be(403)
        verifyZeroInteractions(effectiveDateRepository)
      }
    }
  }
}
