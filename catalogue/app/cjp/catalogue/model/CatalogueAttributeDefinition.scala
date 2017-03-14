package cjp.catalogue.model

case class CatalogueAttributeDefinition (name: String, label: String, kind: String, hasRelatedProducts: Boolean, facet: String, evidenceKinds: List[String] = Nil , validValues : Map[String,Int] = Map())
