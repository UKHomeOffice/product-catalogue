package cjp.catalogue.resource

import cjp.catalogue.model.PersistedCatalogueProduct
import cjp.catalogue.service._
import cjp.catalogue.utils.Logging
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.ignore
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import play.api.http.MimeTypes
import play.api.mvc.{Action, Controller}


class ExportImportResource(exportImportService: ExportImportService, catalogueImportValidator: CatalogueImportValidator) extends Controller with Logging {

  val productSerializer = FieldSerializer[PersistedCatalogueProduct](
    ignore("_id") orElse ignore("name") orElse ignore("version") orElse ignore("created") orElse ignore("lastModified")
  )

  implicit val formats = CjpFormats.formats + productSerializer


  def export = Action {
    Ok(write(exportImportService.export)).as(MimeTypes.JSON)
  }

  def importCatalogue = Action(parse.tolerantText(parse.UNLIMITED)) {
    implicit request =>
      try {
        val catalogueExport: CatalogueData = Serialization.read[CatalogueData](request.body)
        val importValidation = catalogueImportValidator.validate(catalogueExport)
        if (importValidation.isValid) {
          exportImportService.importCatalogue(catalogueExport)
          NoContent
        } else {
          BadRequest(write(importValidation))
        }
      } catch {
        case e => {
          error("error importing catalogue", e)
          InternalServerError(e.getMessage)
        }
      }
  }
}

