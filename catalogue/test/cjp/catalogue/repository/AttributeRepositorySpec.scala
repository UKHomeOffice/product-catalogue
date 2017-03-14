package cjp.catalogue.repository

import cjp.catalogue.model._
import org.scalatest.{Matchers, BeforeAndAfter, WordSpec}

class AttributeRepositorySpec extends WordSpec with BeforeAndAfter with Matchers with MongoSpecSupport {

  private lazy val repository = new AttributeRepository(mongoConnectorForTest)

  before {
    repository.removeAll
  }

  "save" should {
    "persist attribute definition with valid values" in {

      repository.save(CatalogueAttributeDefinition("some","someLabel" ,"oneKind",false,"",Nil,Map("A1" -> 1,"A2" -> 2)))
      val ob = repository.findAllAttributes.head
      ob should be(CatalogueAttributeDefinition("some","someLabel" ,"oneKind",false,"",Nil,Map("A1" -> 1,"A2" -> 2)))


    }
  }


}
