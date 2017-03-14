package cjp.catalogue.service

import cjp.catalogue.model.{ServiceAddOn, CatalogueAttribute}
import cjp.catalogue.service.ImportValidation._

case class ImportValidation(errors: List[String]) {
  def isValid = errors.isEmpty
}

object ImportValidation {

  val PRODUCT_EMPTY_FIELD = "[productName:%s][version:%s][field:%s] is empty"
  val PRODUCT_ATTRIBUTE_NOT_DEFINED = "[productName:%s][version:%s][attrName :%s] not defined in imported attribute definitions"
  val PRODUCT_ATTRIBUTE_INVALID_KIND = "[productName:%s][version:%s][attrName :%s] kind not same as attribute definition"
  val PRODUCT_SECTION_INVALID_KIND = "[productName:%s][version:%s][sectionName :%s] has invalid section type"
  val PRODUCT_ATTRIBUTE_INVALID_VALUE = "[productName:%s][version:%s][attrName :%s] not valid value as per attribute definition"
  val PRODUCT_ATTRIBUTE_INVALID_FACET = "[productName:%s][version:%s][attrName :%s] facet not same as attribute definition"
  val PRODUCT_ATTRIBUTE_INVALID_EVIDENCE_KINDS = "[productName:%s][version:%s][attrName :%s] evidence kind reference not as per attribute definition"
  val PRODUCT_ATTRIBUTE_NO_RELATED_PRODUCTS = "[productName:%s][version:%s][attrName :%s] requires related products"
  val PRODUCT_ATTRIBUTE_RELATED_PRODUCTS_NOT_REQUIRED = "[productName:%s][version:%s][attrName :%s] does not require related products"
  val PRODUCT_ATTRIBUTE_INVALID_RELATED_PRODUCTS = "[productName:%s][version:%s][attrName :%s] related product not present in the product export"
  val PRODUCT_TIMELINE_INVALID_VERSION = "[TimeLine][productName:%s][version:%s] not defined in product export"
  val PRODUCT_INVALID_SERVICE_ADDON = "[productName:%s][version:%s][serviceAddOn:%s] not defined in Service add on export"
  val ATTRIBUTE_DEF_NAME_EMPTY = "attribute definition name is missing"
  val ATTRIBUTE_DEF_EMPTY_FIELD = "[attributeDefinition:%s][field:%s] is empty"
  val SERVICE_ADDON_EMPTY_FIELD = "[serviceAddOn:%s][version:%s][field:%s] is empty"
  val SERVICE_ADDON_TIMELINE_INVALID_VERSION = "[TimeLine][serviceAddOnName:%s][version:%s] not defined in service addOn export"

}

class CatalogueImportValidator {
  val validators = List(validateCatalogueImportFields _, validateAttributeDefinitionImport _, validateProductImport _)

  val productVersionValidators = List(validateProductFields _, validateProductServiceAddOns _, validateProductAttributes _, validateProductBlockLists _)

  def validate(catalogueImport: CatalogueData) = ImportValidation(
    validators.foldLeft(List.empty[String]) {
      case (er, vf) => er ::: vf(catalogueImport)
    }
  )


  private def validateCatalogueImportFields(catalogueImport: CatalogueData): List[String] = {

    val attributesEmptyError = if (catalogueImport.attributes.isEmpty) List("no attribute definitions") else Nil
    val productsEmptyError = if (catalogueImport.products.isEmpty) List("no products") else Nil

    attributesEmptyError ::: productsEmptyError

  }

  private def validateAttributeDefinitionImport(catalogueImport: CatalogueData): List[String] =
    catalogueImport.attributes.foldLeft(List.empty[String]) {
      case (errs, attr) if attr.name.isEmpty => ATTRIBUTE_DEF_NAME_EMPTY :: errs
      case (errs, attr) if attr.facet.isEmpty => ATTRIBUTE_DEF_EMPTY_FIELD.format(attr.name, "facet") :: errs
      case (errs, attr) if attr.kind.isEmpty => ATTRIBUTE_DEF_EMPTY_FIELD.format(attr.name, "kind") :: errs
      case (errs, attr) if attr.label.isEmpty => ATTRIBUTE_DEF_EMPTY_FIELD.format(attr.name, "label") :: errs
      case (errs, _) => errs
    }

  private def validateProductImport(catalogueImport: CatalogueData): List[String] = catalogueImport.products.flatMap {
    case (n, pe) =>
      validateProductTimeLine(catalogueImport, n, pe) ::: validateProductVersions(catalogueImport, n, pe)
  }.toList


  private def validateProductTimeLine(catalogueImport: CatalogueData, name: String, pe: ProductExport): List[String] =
    pe.timeline.foldLeft(List.empty[String]) {
      case (e, tl) if !pe.versions.exists { case (v, _) => v.toInt == tl.version } =>
        PRODUCT_TIMELINE_INVALID_VERSION.format(name, tl.version.toString) :: e
      case (e, _) => e
    }

  private def validateProductVersions(catalogueImport: CatalogueData, name: String, pe: ProductExport): List[String] =
    pe.versions.flatMap {
      case (version, pve) =>
        productVersionValidators.foldLeft(List.empty[String]) {
          case (errs, vf) => errs ::: vf(catalogueImport, name, version, pve)
        }

    }.toList

  private def validateProductFields(catalogueImport: CatalogueData, name: String, version: String, pve: ProductVersionExport): List[String] =
    if (pve.title.isEmpty)
      PRODUCT_EMPTY_FIELD.format(name, version, "title") :: Nil
    else if (pve.description.isEmpty)
      PRODUCT_EMPTY_FIELD.format(name, version, "description") :: Nil
    else Nil

  private def validateProductServiceAddOns(catalogueImport: CatalogueData, name: String, version: String, pve: ProductVersionExport): List[String] =
    pve.serviceAddOns.foldLeft(List.empty[String]) {
      case (errs, se) if se.title.isEmpty => SERVICE_ADDON_EMPTY_FIELD.format(name, version, "title") :: errs
      case (errs, se) if se.description.isEmpty => SERVICE_ADDON_EMPTY_FIELD.format(name, version, "description") :: errs
      case (errs, _) => errs
    }

  val sections = List("start", "application", "finance", "documents", "declaration", "pay")
  private def validateProductBlockLists(catalogueImport: CatalogueData, name: String, version: String, pve: ProductVersionExport): List[String] =
    pve.productBlockList.foldLeft(List.empty[String]) {
      case (errs, pbl) if !sections.contains(pbl.section) => List(PRODUCT_SECTION_INVALID_KIND.format(name, version, pbl.section))
      case (errs, pbl) => errs
    }


  private def validateProductAttributes(catalogueImport: CatalogueData, name: String, version: String, pve: ProductVersionExport): List[String] =
    pve.attributes.foldLeft(List.empty[String]) {
      case (er, (attrName, _)) if !catalogueImport.attributes.exists(_.name == attrName) =>
        PRODUCT_ATTRIBUTE_NOT_DEFINED.format(name, version, attrName) :: er

      case (er, (attrName, attr)) if hasInvalidKind(catalogueImport, attrName, attr) =>
        PRODUCT_ATTRIBUTE_INVALID_KIND.format(name, version, attrName) :: er

      case (er, (attrName, attr)) if hasInvalidValue(catalogueImport, attrName, attr) =>
        PRODUCT_ATTRIBUTE_INVALID_VALUE.format(name, version, attrName) :: er

      case (er, (attrName, attr)) if hasInvalidFacet(catalogueImport, attrName, attr) =>
        PRODUCT_ATTRIBUTE_INVALID_FACET.format(name, version, attrName) :: er

      case (er, (attrName, attr)) if hasInvalidEvidenceKinds(catalogueImport, attrName, attr) =>
        PRODUCT_ATTRIBUTE_INVALID_EVIDENCE_KINDS.format(name, version, attrName) :: er
      //the data in the catalogue db somehow not confirm to these rules? will need to fix the attribute defs
//      case (er, (attrName, attr)) if hasNoRequiredRelatedProducts(catalogueImport, attrName, attr) =>
//        PRODUCT_ATTRIBUTE_NO_RELATED_PRODUCTS.format(name, version, attrName) :: er
//
//      case (er, (attrName, attr)) if hasRelatedProductsWhenNotRequired(catalogueImport, attrName, attr) =>
//        PRODUCT_ATTRIBUTE_RELATED_PRODUCTS_NOT_REQUIRED.format(name, version, attrName) :: er

      case (er, (attrName, attr)) if hasInvalidRelatedProducts(catalogueImport, attrName, attr) =>
        PRODUCT_ATTRIBUTE_INVALID_RELATED_PRODUCTS.format(name, version, attrName) :: er

      case (er, _) => er
    }


  private def hasInvalidFacet(catalogueImport: CatalogueData, attrName: String, attr: CatalogueAttribute): Boolean = {
    catalogueImport.attributes.exists(x => x.name == attrName && x.facet != attr.facet)
  }

  private def hasInvalidValue(catalogueImport: CatalogueData, attrName: String, attr: CatalogueAttribute): Boolean = {
    catalogueImport.attributes.exists(x => x.name == attrName && x.validValues.nonEmpty && !x.validValues.values.exists(_ == attr.value))
  }

  private def hasInvalidKind(catalogueImport: CatalogueData, attrName: String, attr: CatalogueAttribute): Boolean = {
    catalogueImport.attributes.exists(x => x.name == attrName && x.kind != attr.kind)
  }

  private def hasInvalidEvidenceKinds(catalogueImport: CatalogueData, attrName: String, attr: CatalogueAttribute): Boolean = {
    catalogueImport.attributes.exists(x => x.name == attrName && x.evidenceKinds.nonEmpty && !attr.evidenceKinds.forall(ek => x.evidenceKinds.contains(ek.reference)))
  }

  //TODO the data in the catalogue db somehow not confirm to these rules? will need to fix the attribute defs
  private def hasNoRequiredRelatedProducts(catalogueImport: CatalogueData, attrName: String, attr: CatalogueAttribute): Boolean = {
    catalogueImport.attributes.exists(x => x.name == attrName && (x.hasRelatedProducts && attr.relatedProducts.isEmpty))
  }

  //TODO the data in the catalogue db somehow not confirm to these rules? will need to fix the attribute defs
  private def hasRelatedProductsWhenNotRequired(catalogueImport: CatalogueData, attrName: String, attr: CatalogueAttribute): Boolean = {
    catalogueImport.attributes.exists(x => x.name == attrName && (!x.hasRelatedProducts && attr.relatedProducts.nonEmpty))
  }

  private def hasInvalidRelatedProducts(catalogueImport: CatalogueData, attrName: String, attr: CatalogueAttribute): Boolean = {
    val allProducts = catalogueImport.products.keys.toList
    catalogueImport.attributes.exists {
      x =>
        x.name == attrName && !attr.relatedProducts.forall(r => allProducts.contains(r))
    }
  }


}