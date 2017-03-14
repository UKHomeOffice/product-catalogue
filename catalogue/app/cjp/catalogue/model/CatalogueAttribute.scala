package cjp.catalogue.model

import com.novus.salat.annotations.raw.Salat

@Salat
trait CatalogueAttribute {
  def kind: String
  val label: String
  val value: Any
  val facet: String
  val referenceText: String
  val referenceUrl: String
  val evidenceKinds: List[EvidenceKind]
  val relatedProducts: List[String]
}
