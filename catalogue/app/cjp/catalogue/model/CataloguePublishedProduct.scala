package cjp.catalogue.model

import org.bson.types.ObjectId
import org.joda.time.DateTime

case class CataloguePublishedProduct(name: String,
                                     title: String,
                                     description: String,
                                     attributes: Map[String, CatalogueAttribute] = Map.empty,
                                     serviceAddOns: List[ServiceAddOn] = Nil,
                                     version: Int,
                                     updateId: Option[ObjectId] = None,
                                     effectiveFrom: DateTime,
                                     productBlockList: List[ProductBlockList] = Nil)
