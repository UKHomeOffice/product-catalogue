package cjp.catalogue.service

import cjp.catalogue.model._
import cjp.catalogue.test.builder._
import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}

class CatalogueImportValidatorSpec extends WordSpec with Matchers {


  val catalogueImportValidator = new CatalogueImportValidator()

  "validate" should {
    "pass a valid import" in {
      val importValidation: ImportValidation = catalogueImportValidator.validate(CatalogueExportBuilder())

      importValidation.errors.size shouldBe 0

    }

    "check for non empty attributes, products and serviceAddOns" in {

      val importValidation: ImportValidation = catalogueImportValidator.validate(CatalogueExportBuilder(List(), Map()))

      importValidation.errors.size shouldBe 2
      importValidation.errors should contain("no attribute definitions")
      importValidation.errors should contain("no products")
    }
  }


  "attribute definitions" should {
    "non empty name, facet, label and kind" in {
      val catalogueExport: CatalogueData = CatalogueExportBuilder(
        attributes = List(
          CatalogueAttributeDefinition("", "label", "kind", false, "facet"),
          CatalogueAttributeDefinition("missingLabel", "", "kind", false, "facet"),
          CatalogueAttributeDefinition("missingKind", "label", "", false, "facet"),
          CatalogueAttributeDefinition("missingFacet", "label", "kind", false, "")
        )
      )

      val importValidation: ImportValidation = catalogueImportValidator.validate(catalogueExport)

      importValidation.errors.size shouldBe 4
      importValidation.errors should contain("attribute definition name is missing")
      importValidation.errors should contain("[attributeDefinition:missingLabel][field:label] is empty")
      importValidation.errors should contain("[attributeDefinition:missingKind][field:kind] is empty")
      importValidation.errors should contain("[attributeDefinition:missingFacet][field:facet] is empty")


    }
  }

  "product" should {

    "not have empty title and description" in {

      val productVersion1: ProductVersionExport = ProductVersionExportBuilder(title = "")
      val productVersion2: ProductVersionExport = ProductVersionExportBuilder(description = "")
      val productExport = ProductExportBuilder(productVersion = List(productVersion1, productVersion2))
      val builder: CatalogueData = CatalogueExportBuilder(products = Map("productName" -> productExport))
      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 2
      importValidation.errors should contain("[productName:productName][version:0][field:title] is empty")
      importValidation.errors should contain("[productName:productName][version:1][field:description] is empty")

    }

    "product block lists should be of valid type" in {

      val productVersion1: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description", productBlockList = List(ProductBlockList("start", List.empty)))
      val productVersion2: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description", productBlockList = List(ProductBlockList("application", List.empty)))
      val productVersion3: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description", productBlockList = List(ProductBlockList("finance", List.empty)))
      val productVersion4: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description", productBlockList = List(ProductBlockList("documents", List.empty)))
      val productVersion5: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description", productBlockList = List(ProductBlockList("declaration", List.empty)))
      val productVersion6: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description", productBlockList = List(ProductBlockList("pay", List.empty)))
      val productVersion7: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description")
      val productVersion8: ProductVersionExport = ProductVersionExportBuilder(title = "product", description = "description", productBlockList = List(ProductBlockList("foobar", List.empty)))
      val productExport = ProductExportBuilder(productVersion = List(productVersion1, productVersion2, productVersion3, productVersion4, productVersion5, productVersion6, productVersion7, productVersion8))
      val builder: CatalogueData = CatalogueExportBuilder(products = Map("productName" -> productExport))
      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors should contain("[productName:productName][version:7][sectionName :foobar] has invalid section type")
    }


    " attribute be defined in the attribute definitions" in {

      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map("attr" -> CatalogueBigDecimalProductAttribute(10.0, "", "facet", "", "")))
      val productExport = ProductExportBuilder(productVersion = List(productVersion))
      val builder: CatalogueData = CatalogueExportBuilder(products = Map("productName" -> productExport))
      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] not defined in imported attribute definitions"

    }

    "attribute have same kind as the definition" in {
      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map("attr" -> CatalogueBigDecimalProductAttribute(10.0, "", "facet", "", "")))
      val productExport = ProductExportBuilder(productVersion = List(productVersion))
      val builder: CatalogueData = CatalogueExportBuilder(
        products = Map("productName" -> productExport),
        attributes = List(CatalogueAttributeDefinition("attr", "someLabel", "Integer", false, "facet"))
      )
      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] kind not same as attribute definition"
    }

    "attribute value should be contained in validValues of attribute definitions if defined" in {

      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map("attr" -> CatalogueIntegerProductAttribute(10, "", "facet", "", "")))
      val productExport = ProductExportBuilder(productVersion = List(productVersion))
      val builder: CatalogueData = CatalogueExportBuilder(
        products = Map("productName" -> productExport),
        attributes = List(CatalogueAttributeDefinition("attr", "someLabel", "Integer", false, "facet", validValues = Map("v1" -> 1, "v1" -> 2, "v1" -> 9)))
      )
      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] not valid value as per attribute definition"
    }

    "attribute facet should be same as attributes definition" in {

      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map("attr" -> CatalogueIntegerProductAttribute(9, "", "wrongFacet", "", "")))
      val productExport = ProductExportBuilder(productVersion = List(productVersion))
      val builder: CatalogueData = CatalogueExportBuilder(
        products = Map("productName" -> productExport),
        attributes = List(CatalogueAttributeDefinition("attr", "someLabel", "Integer", false, "facet", validValues = Map("v1" -> 1, "v1" -> 2, "v1" -> 9)))
      )
      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] facet not same as attribute definition"
    }

    "attribute evidence kind ref should be contained in the definition's evidence kind if defined" in {

      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map(
        "attr" -> CatalogueIntegerProductAttribute(9, "", "facet", "", "", List(EvidenceKind("wrongEvidenceKind", false, false))))
      )
      val productExport = ProductExportBuilder(productVersion = List(productVersion))
      val builder: CatalogueData = CatalogueExportBuilder(
        products = Map("productName" -> productExport),
        attributes = List(CatalogueAttributeDefinition("attr", "someLabel", "Integer", false, "facet", List("evidenceKind")))
      )
      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] evidence kind reference not as per attribute definition"

    }

    //    "have related products if defined in the definition" in {
    //
    //      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map(
    //        "attr" -> CatalogueIntegerProductAttribute(9, "", "facet", "", "", List(EvidenceKind("evidenceKind", false, false))))
    //      )
    //      val productExport = ProductExportBuilder(productVersion = List(productVersion))
    //      val builder: CatalogueExport = CatalogueExportBuilder(
    //        products = Map("productName" -> productExport),
    //        attributes = List(CatalogueAttributeDefinition("attr", "", "Integer", true, "facet", List("evidenceKind")))
    //      )
    //      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)
    //
    //      importValidation.errors.size shouldBe 1
    //      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] requires related products"
    //
    //    }

    //    "not have related products if defined in the definition" in {
    //
    //      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map(
    //        "attr" -> CatalogueIntegerProductAttribute(9, "", "facet", "", "", List(EvidenceKind("evidenceKind", false, false)), List("relatedProduct")))
    //      )
    //      val productExport = ProductExportBuilder(productVersion = List(productVersion))
    //      val builder: CatalogueExport = CatalogueExportBuilder(
    //        products = Map("productName" -> productExport),
    //        attributes = List(CatalogueAttributeDefinition("attr", "", "Integer", false, "facet", List("evidenceKind")))
    //      )
    //      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)
    //
    //      importValidation.errors.size shouldBe 1
    //      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] does not require related products"
    //
    //    }


    "attribute related products should be present in the catalogue product export" in {

      val productVersion: ProductVersionExport = ProductVersionExportBuilder(attributes = Map(
        "attr" -> CatalogueIntegerProductAttribute(9, "", "facet", "", "", List(EvidenceKind("evidenceKind", false, false)), List("someRelatedProduct")))
      )
      val productExport = ProductExportBuilder(productVersion = List(productVersion))
      val builder: CatalogueData = CatalogueExportBuilder(
        products = Map("productName" -> productExport),
        attributes = List(CatalogueAttributeDefinition("attr", "someLabel", "Integer", true, "facet", List("evidenceKind")))
      )

      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors.head shouldBe "[productName:productName][version:0][attrName :attr] related product not present in the product export"
    }

    "timeline versions be defined in the product export" in {
      val productExport = ProductExportBuilder.withTimeLine(TimelineVersion(1, DateTime.now()))
      val builder: CatalogueData = CatalogueExportBuilder(products = Map("productName" -> productExport))

      val importValidation: ImportValidation = catalogueImportValidator.validate(builder)

      importValidation.errors.size shouldBe 1
      importValidation.errors.head shouldBe "[TimeLine][productName:productName][version:1] not defined in product export"

    }
  }
}
