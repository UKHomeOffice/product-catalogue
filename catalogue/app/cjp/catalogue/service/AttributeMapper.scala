package cjp.catalogue.service

import cjp.catalogue.model.CatalogueAttribute

trait AttributeMapper {

  def mapAttributesToDto(mapOfAttributes: Map[String, CatalogueAttribute], relatedProductsMapper: List[String] => List[RelatedProductSummary], labels: Map[String, String]): Map[String, CatalogueAttributeDto] = {
    mapOfAttributes.map{ tuple =>
      val attribute: CatalogueAttribute = tuple._2
      val relatedProducts = relatedProductsMapper(attribute.relatedProducts)

      (tuple._1, CatalogueAttributeDto(
        value = attribute.value,
        label = labels.getOrElse(tuple._1, tuple._1),
        facet = attribute.facet,
        referenceText = attribute.referenceText,
        referenceUrl = attribute.referenceUrl,
        evidenceKinds =  attribute.evidenceKinds,
        relatedProducts = relatedProducts
      ))
    }
  }

}
