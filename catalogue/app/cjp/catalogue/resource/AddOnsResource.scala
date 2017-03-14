package cjp.catalogue.resource

import cjp.catalogue.service.{AddOnService, AttributeService}
import org.json4s.Formats
import org.json4s.native.Serialization._
import play.api.http.MimeTypes
import play.api.mvc.{Action, Controller}

class AddOnsResource(addOnService: AddOnService) extends Controller {

  implicit val formats: Formats = CjpFormats.formats

  def getAddOns = Action {
    Ok(write(addOnService.getAllAddOnNames))
      .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
      .as(MimeTypes.JSON)
  }

}