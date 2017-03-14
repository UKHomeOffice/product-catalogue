package cjp.catalogue.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
case class CatalogueIntegerProductAttribute(value: Int,
                                            label: String = "",
                                            facet: String,
                                            referenceText: String = "",
                                            referenceUrl: String = "",
                                            evidenceKinds: List[EvidenceKind] = Nil,
                                            relatedProducts: List[String] = Nil) extends CatalogueAttribute {
  def kind = "Integer"
}
