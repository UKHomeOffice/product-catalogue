package cjp.catalogue.filter

import java.util.UUID.randomUUID

import org.slf4j.{LoggerFactory, MDC}
import play.api.mvc.{Result, Filter, RequestHeader, SimpleResult}

import scala.collection.JavaConverters._
import scala.concurrent.Future

object AccessLogFilter extends Filter {

  val log = LoggerFactory.getLogger("ACCESS_LOG")

  import scala.concurrent.ExecutionContext.Implicits.global

  def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (rh.path.startsWith("/assets/")) {
      f(rh)
    } else {
      val start = System.currentTimeMillis()
      val requestId = randomUUID().toString
      val applicationId = rh.session.get("applicationId").getOrElse("(none)")
      MDC.put("request_id", requestId)
      MDC.put("application_id", applicationId)

      log.debug("InReq {} {}", rh.method, rh.path, Map(
        "request_method" -> rh.method,
        "request_url" -> rh.path,
        "request_source" -> rh.headers.get("x-forwarded-for").getOrElse(rh.remoteAddress),
        "request_referrer" -> rh.headers.get("referer").orElse(rh.headers.get("referrer")).getOrElse("(none)"),
        "request_userAgent" -> rh.headers.get("user-agent").getOrElse("(none)")
      ).asJava)

      f(rh).map { sr =>
        log.debug("InRes {} {} {}", sr.header.status.toString, rh.method, rh.path, Map(
          "request_method" -> rh.method,
          "request_url" -> rh.path,
          "response_status" -> sr.header.status,
          "response_contentType" -> sr.header.headers.collectFirst { case (k, v) if k.equalsIgnoreCase("content-type") => v }.getOrElse("(none)"),
          "response_location" -> sr.header.headers.collectFirst { case (k, v) if k.equalsIgnoreCase("location") => v }.getOrElse("(none)"),
          "duration_ms" -> (System.currentTimeMillis() - start)
        ).asJava)
        MDC.clear()
        sr
      }
    }
  }
}
