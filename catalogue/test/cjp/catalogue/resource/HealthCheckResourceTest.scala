package cjp.catalogue.resource

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import cjp.catalogue.util.BuildInfo
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import play.api.http.{HeaderNames, Status}
import play.api.test._

import scala.concurrent.Future

class HealthCheckResourceTest extends WordSpec with BeforeAndAfter with PlayRunners with ResultExtractors with HeaderNames with Status with Matchers with MockitoSugar {

  implicit val timeout = Timeout(10L, TimeUnit.SECONDS)

  private val buildInfo: Some[BuildInfo] = Some(BuildInfo(1, "lst-commit"))


  "calling GET /healthcheck" should {

    def healthcheck(reportHealth:Boolean) = {
      val mongoHealthCheck = mock[MongoHealthCheck]
      when(mongoHealthCheck.ping()).thenReturn(Future successful reportHealth)
      val resource = new HealthCheckResource(mongoHealthCheck, buildInfo)
      resource.healthCheck()(FakeRequest())
    }

    "show build info and the string 'healthy' when mongo reports healthy" in {

      val result = healthcheck(reportHealth = true)
      status(result) should be(200)
      contentAsString(result) should be("healthy!\nbuild-number: 1")
    }

    "show build info and the string 'Unhealthy' when mongo reports unhealthy" in {

      val result = healthcheck(reportHealth = false)
      status(result) should be(500)
      contentAsString(result) should be("unhealthy! Mongo reported a negative response to ping!\nbuild-number: 1")
    }
  }

  "calling GET /ping" should {

    "reports pong" in {

      val resource = new HealthCheckResource(null, buildInfo)
      val result = resource.ping()(FakeRequest())
      status(result) should be(200)
      contentAsString(result) should be("pong")
    }
  }
}
