package cjp.catalogue.service

import cjp.catalogue.model.{CatalogueAttribute, EvidenceKind}

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
case class CatalogueAttributeDto(value: Any,
                                 label: String,
                                 facet: String,
                                 referenceText: String,
                                 referenceUrl: String,
                                 evidenceKinds: List[EvidenceKind] = Nil,
                                 relatedProducts: List[RelatedProductSummary] = Nil,
                                 validValues : Map[String,Int] = Map())


case class RelatedProductSummary(name: String, title: String)
