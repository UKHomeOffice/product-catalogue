package cjp.catalogue.service

import cjp.catalogue.model.CatalogueAttributeDefinition
import cjp.catalogue.repository.AttributeRepository
import java.lang.String
import scala.Predef.String

class AttributeService(attributeRepository: AttributeRepository) {

  def catalogueAttributes = attributeRepository.findAllAttributes

  def getDefinition(facet: String, name: String): Option[CatalogueAttributeDefinition] = {
    catalogueAttributes.find(ca => ca.facet == facet && ca.name == name)
  }

  def finder:(String, String) => Option[CatalogueAttributeDefinition] = {
    val attrs = catalogueAttributes

    {(facet: String, name: String) =>
      attrs.find(ca => ca.facet == facet && ca.name == name)
    }
  }

  def importAttributes(attributes: List[CatalogueAttributeDefinition]): Unit = {
    attributeRepository.stageImport(attributes)
    attributeRepository.renameStage
  }
}
