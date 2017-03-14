package cjp.catalogue.resource

import cjp.catalogue.repository.EffectiveDateRepository
import org.joda.time.DateTime
import org.json4s.native.Serialization._
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller, Request}


class EffectiveDateResource(effectiveDateRepo: EffectiveDateRepository, enableTimemachine: Boolean) extends Controller {

  implicit val formats = CjpFormats.formats

  def getEffectiveDate = Action {
    Ok(Json.toJson(EffectiveDate(DateTime.now))(EffectiveDate.effectiveDateWrites))
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      .as(MimeTypes.JSON)
  }

  def setEffectiveDate = Action { implicit request =>
    val date: String = getEffectiveDate(request)
    val dateTime = DateTime.parse(date)
    if (enableTimemachine) {
      effectiveDateRepo.set(dateTime)
      NoContent
        .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")

    } else Forbidden
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
  }

  def resetEffectiveDate = Action {
    if (enableTimemachine) {
      effectiveDateRepo.reset
      NoContent
        .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    } else Forbidden
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
  }

  private def getEffectiveDate(request: Request[AnyContent]): String = {
    val json: Option[JsValue] = request.body.asJson
    json map {
      case (value: JsValue) => (value \ "effectiveDate").as[String]
    } getOrElse (throw new scala.IllegalArgumentException(s"could not parse request with body ${request.body.asText}"))
  }
}

object EffectiveDate {

  implicit val effectiveDateWrites = new Writes[EffectiveDate] {
    def writes(date: EffectiveDate) = Json.obj("effectiveDate" -> s"${date.effectiveDate.toString()}")
  }

  implicit val effectiveDateTimeReads: Reads[DateTime] = (JsPath \ "effectiveDate").read[String].map(DateTime.parse)
}

case class EffectiveDate(effectiveDate:DateTime)