package cjp.catalogue.repository

import cjp.catalogue.model.{CatalogueBooleanProductAttribute, EvidenceKind, _}
import cjp.catalogue.test.builder.CatalogueProductBuilder
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

class ProductRepositorySpec extends WordSpec with BeforeAndAfter with Matchers with MongoSpecSupport {

  private lazy val repository = new ProductRepository(mongoConnectorForTest)

  before {
    repository.removeAll
  }

  def newProduct(name: String) = NewCatalogueProduct(
    name = name,
    title = "title1",
    description = "description1",
    attributes = Map(
      "boolAttr" -> CatalogueBooleanProductAttribute(true, "", "ref text", "ref/url", "identity", List(EvidenceKind("docRef", true, false))),
      "intAttr" -> CatalogueIntegerProductAttribute(1, "", "ref text", "ref/url", "identity", List(EvidenceKind("docRef", true, false))),
      "bigDecimalAttr" -> CatalogueBigDecimalProductAttribute(99.9, "", "ref text", "ref/url", "identity", List(EvidenceKind("docRef", true, false)))
    ),
    created = DateTime.now,
    lastModified = DateTime.now,
    serviceAddOns = Nil
  )

  "createNewProduct" should {
    "persist all attribute types" in {

      val productName: String = "productName"

      val product = newProduct(productName)

      val persisted = repository.createNewProduct(product)

      val actualProduct = repository.findLatestVersion(productName).get
      actualProduct shouldBe persisted
    }

    "throw ProductNameAlreadyUsedException if the name is already used" in {
      val p1 = CatalogueProductBuilder(name = "p1")
      val p2 = CatalogueProductBuilder(name = "p1")

      repository.createNewProduct(p1)
      intercept[ProductNameAlreadyUsedException] {
        repository.createNewProduct(p2)
      }
    }
  }

  "addVersion" should {

    "make the last inserted version the current latest version" in {
      val productName: String = "productName"

      val product = newProduct(productName).copy(title = "title1", description = "description1")

      val version2 = newProduct(productName).copy(title = "title2", description = "description2")

      repository.createNewProduct(product)

      val newVersion = repository.addVersion(productName, version2)

      val actualProduct = repository.findLatestVersion(productName).get
      actualProduct shouldBe newVersion.get
    }
  }

  "update" should {

    "update the product with the given id" in {
      val productName: String = "productName"

      val version = newProduct(productName).copy(title = "title1", description = "description1")

      val persisted = repository.createNewProduct(version)

      val draft: PersistedCatalogueProduct = repository.update(persisted._id, persisted.version, version.copy(title = "new title"))

      val actualProduct = repository.findByNameAndVersion(persisted.name, persisted.version)
      actualProduct.get shouldBe draft
    }
  }

  "delete" should {
    "delete the product" in {
      val productName: String = "productName"

      val version = newProduct(productName).copy(title = "title1", description = "description1")

      val persisted = repository.createNewProduct(version)

      repository.delete(persisted)

      repository.findByNameAndVersion(persisted.name, persisted.version) shouldBe None
    }
  }

  "findByNameAndVersion" should {
    "return the specified product" in {
      val productName: String = "generalVisitVisa"
      val product = CatalogueProductBuilder(name = productName)
      val persisted = repository.createNewProduct(product)
      repository.createNewProduct(CatalogueProductBuilder(name = "anotherVisa"))
      repository.findByNameAndVersion(persisted.name, persisted.version).get shouldBe persisted
    }

    "return None if there isnt a product with the given name" in {
      assert(repository.findByNameAndVersion("some name", 1) === None)
    }

    "return None if there is a product with the given name, but not the version" in {
      val productName: String = "generalVisitVisa"
      val product = CatalogueProductBuilder(name = productName)
      repository.createNewProduct(product)

      assert(repository.findByNameAndVersion(productName, 101) === None)
    }
  }

  "findAllTags" should {
    "return the distinct set of tags from all products" in {
      val p1 = CatalogueProductBuilder(name = "p1", tags = List("tag1", "tag2", "tag3"))
      val p2 = CatalogueProductBuilder(name = "p2", tags = List("tag1", "tag4"))

      repository.createNewProduct(p1)
      repository.createNewProduct(p2)

      assert(repository.findAllTags === List("tag1", "tag2", "tag3", "tag4"))
    }

    "return the distinct set of tags and ignore the empty tags" in {
      val p1 = CatalogueProductBuilder(name = "p1", tags = List())
      val p2 = CatalogueProductBuilder(name = "p2", tags = List("tag1", "tag4"))

      repository.createNewProduct(p1)
      repository.createNewProduct(p2)

      assert(repository.findAllTags === List("tag1", "tag4"))
    }

  }
}
