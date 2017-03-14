package cjp.catalogue.repository

import org.scalatest._
import org.joda.time.DateTime

class EffectiveDateRepositorySpec extends WordSpec with BeforeAndAfter with Matchers with MongoSpecSupport {

  private lazy val repository = new EffectiveDateRepository(mongoConnectorForTest)

  before {
    repository.removeAll
  }

  "EffectiveDateRepository" should {
    "set and find datetime" in {
      val datetime = DateTime.now
      repository.set(datetime)

      repository.find should be(Some(datetime))
    }

    "remove effective date time" in {
      repository.set(DateTime.now)
      repository.reset
      repository.find should be (None)
    }
  }

}
