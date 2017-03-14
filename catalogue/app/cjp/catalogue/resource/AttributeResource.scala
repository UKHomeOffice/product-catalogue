package cjp.catalogue.resource

import play.api.http.MimeTypes
import play.api.mvc.{Action, Controller}
import cjp.catalogue.service.AttributeService
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Serialization._

class AttributeResource(attributeService: AttributeService) extends Controller {

  implicit val formats = CjpFormats.formats

  def getSummaries = Action {
    Ok(write(attributeService.catalogueAttributes))
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      .as(MimeTypes.JSON)
  }

}