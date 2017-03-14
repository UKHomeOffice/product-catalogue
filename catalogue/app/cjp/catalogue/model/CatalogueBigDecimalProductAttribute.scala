package cjp.catalogue.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
case class CatalogueBigDecimalProductAttribute(value: BigDecimal,
                                               label: String = "",
                                               facet: String,
                                               referenceText: String,
                                               referenceUrl: String,
                                               evidenceKinds: List[EvidenceKind] = Nil,
                                               relatedProducts: List[String] = Nil) extends CatalogueAttribute {
  def kind = "Decimal"
}
