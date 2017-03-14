package cjp.catalogue.resource

import cjp.catalogue.repository.ProductNameAlreadyUsedException
import cjp.catalogue.service.{CloneProductDto, ProductDto, ProductManagementService, ProductNotFoundException}
import org.json4s.native.Serialization._
import play.api.http.MimeTypes
import play.api.mvc._


class ManageProductResource(productService: ProductManagementService) extends Controller {

  implicit val formats = CjpFormats.formats

  private def OkResponse(body: String) = Ok(body).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*").as(MimeTypes.JSON)

  private val NoContentResponse = NoContent.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*")

  def getSummaries = Action {
    OkResponse(write(productService.getSummaries))
  }

  def getTags = Action {
    OkResponse(write(productService.getTags))
  }


  def getCurrent(name: String) = Action {
    productService.getCurrent(name).fold(NotFound(s"product name $name not found")) { product => OkResponse(write(product)) }
  }

  def getDraft(name: String) = Action {
    productService.getDraft(name).fold(NoContentResponse) { draft => OkResponse(write(draft)) }
  }

  def deleteDraft(name: String) = Action {
    try {
      productService.deleteDraft(name)
      NoContentResponse
    } catch {
      case e: IllegalArgumentException =>
        createNotFoundResponse(Map("message" -> e.getMessage))
    }
  }

  def create = Action { implicit request =>

    try {
      val json = getBodyAsJson(request)
      val product: ProductDto = read[ProductDto](json)
      val id = productService.create(product)
      val redirect = routes.ManageProductResource.getCurrent(id)
      Created.withHeaders("Location" -> redirect.url)
    }
    catch {
      case e: ProductNameAlreadyUsedException =>
        createBadRequest(Map("message" -> s"title [${e.name}] has already been used"))
    }
  }

  private def createBadRequest(errors: Map[String, String]) = BadRequest(write(errors)).as(MimeTypes.JSON)

  private def createNotFoundResponse(messages: Map[String, String]) = NotFound(write(messages)).as(MimeTypes.JSON)

  def cloneProduct(name: String) = Action { implicit request =>
    try {
      val json = getBodyAsJson(request)
      val cloneProductDto: CloneProductDto = read[CloneProductDto](json)
      if (cloneProductDto.title.trim.isEmpty) {
        createBadRequest(Map("message" -> "title not provided"))
      } else {
        productService.cloneProduct(name, cloneProductDto) map { cloned =>
          val redirect = routes.ManageProductResource.getCurrent(cloned)
          Created.withHeaders("Location" -> redirect.url)
        } getOrElse {
          createNotFoundResponse(Map("message" -> s"product name [$name] not found"))
        }
      }
    } catch {
      case e: ProductNameAlreadyUsedException =>
        createBadRequest(Map("message" -> s"title [${e.name}] has already been used"))
    }
  }

  private def getBodyAsJson(request: Request[AnyContent]): String = {
    request.body.asJson map (_.toString()) getOrElse (throw new scala.IllegalArgumentException(s"could not parse request with body ${request.body.asText}"))
  }

  def update(name: String) = Action { implicit request =>
    try {
      val json = getBodyAsJson(request)
      val productDto: ProductDto = read[ProductDto](json).copy(name = Some(name))
      productService.update(name, productDto)
      NoContentResponse

    } catch {
      case e: IllegalArgumentException =>
        createBadRequest(Map("message" ->  e.getMessage))
      case e: ProductNotFoundException =>
        createNotFoundResponse(Map("message" -> e.getMessage))
    }
  }
}

