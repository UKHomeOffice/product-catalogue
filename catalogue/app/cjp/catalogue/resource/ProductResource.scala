package cjp.catalogue.resource



import cjp.catalogue.service.{AttributeService, ProductDto, ProductManagementService, ProductNotFoundException}
import play.api.http.MimeTypes
import play.api.mvc.{Action, Controller}
import org.joda.time.DateTime
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Serialization._

class ProductResource(productService: ProductManagementService, attributeService: AttributeService) extends Controller {

  implicit val formats = CjpFormats.formats

  def getLatest(name: String) = Action {
    getVersion(productService.getCurrent(name))
  }

  def getDraft(name: String) = Action {
    getVersion(productService.getDraft(name))
  }

  // TODO - respond with 400 if the effectiveAt does not parse as DateTime
  def getEffectiveAt(name: String, effectiveAt: String) = Action {
    getVersion(productService.getEffectiveAt(name, DateTime.parse(effectiveAt)))
  }

  private def getVersion(productOption: => Option[ProductDto]) =
      productOption.fold(NotFound(""))(p =>
        Ok(write(p))
          .withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
          .as(MimeTypes.JSON))
}

