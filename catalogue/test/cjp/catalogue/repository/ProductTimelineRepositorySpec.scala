package cjp.catalogue.repository

import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import org.joda.time.{DateTimeUtils, DateTime}
import cjp.catalogue.model.TimelineVersion

class ProductTimelineRepositorySpec extends WordSpec with BeforeAndAfter with Matchers with MongoSpecSupport {

  private lazy val repository = new ProductTimelineRepository(mongoConnectorForTest)

  before {
    repository.removeAll
  }

  val productName = "productName"

  "createNewTimeline" should {

    "persist a new timeline" in {

      val persisted = repository.createNewTimeline(productName)

      repository.findByProductName(productName) shouldBe Some(persisted)
    }

    "throw ProductTimelineAlreadyExistsException if the timeline for the product already exists" in {

      repository.createNewTimeline("product")
      intercept[ProductTimelineAlreadyExistsException] {
        repository.createNewTimeline("product")
      }
    }
  }

  "setActiveProduct" should {

    "set the active product on an empty timeline" in {
      val now = DateTime.now

      DateTimeUtils.setCurrentMillisFixed(now.getMillis)

      repository.createNewTimeline(productName)

      repository.setActiveProduct(productName, 2)

      repository.findByProductName(productName).get.findEffectiveVersion(now) shouldBe Some(TimelineVersion(2, now))
    }

    "throw ProductTimelineDoesNotExistException if the timeline for the product does not exist" in {

      intercept[ProductTimelineDoesNotExistException] {
        repository.setActiveProduct(productName, 1)
      }
    }

    "throw IllegalArgumentException if the timeline for the product is not empty" in {

      repository.createNewTimeline(productName)
      repository.setDraftProduct(productName, 1, DateTime.now.plusDays(2))

      intercept[IllegalArgumentException] {
        repository.setActiveProduct(productName, 2)
      }
    }
  }

  "setDraftProduct" should {

    "set the draft" in {
      val now = DateTime.now
      DateTimeUtils.setCurrentMillisFixed(now.getMillis)
      val draftEffectiveDate = now.plusYears(1)
      val draftVersion = 3

      repository.createNewTimeline(productName)

      repository.setActiveProduct(productName, 2)

      repository.setDraftProduct(productName, draftVersion, draftEffectiveDate)

      val timeline = repository.findByProductName(productName).get
      timeline.findDraftVersion() shouldBe Some(TimelineVersion(draftVersion, draftEffectiveDate))
      timeline.findEffectiveVersion(now) shouldBe Some(TimelineVersion(2, now))
    }

    "throw ProductTimelineDoesNotExistException if the timeline for the product does not exist" in {

      intercept[ProductTimelineDoesNotExistException] {
        repository.setDraftProduct(productName, 1, DateTime.now)
      }
    }
  }

  "updateDraftProduct" should {

    "update draft on the timeline without affecting the active version" in {
      val now = DateTime.now
      DateTimeUtils.setCurrentMillisFixed(now.getMillis)
      val newEffectiveDate = now.plusYears(1)
      val newVersion = 4

      repository.createNewTimeline(productName)
      repository.setActiveProduct(productName, 1)
      repository.setDraftProduct(productName, 2, now.plusDays(2))

      repository.updateDraftProduct(productName, newVersion, newEffectiveDate)

      val actualTimeline = repository.findByProductName(productName).get
      actualTimeline.findDraftVersion() shouldBe Some(TimelineVersion(newVersion, newEffectiveDate))
      actualTimeline.findEffectiveVersion(now) shouldBe Some(TimelineVersion(1, now))
    }

    "throw IllegalStateException if draft does not exist on the timeline" in {
      repository.createNewTimeline(productName)

      intercept[IllegalStateException] {
        repository.updateDraftProduct(productName, 1, DateTime.now)
      }
    }

    "throw ProductTimelineDoesNotExistException if the timeline for the product does not exist" in {

      intercept[ProductTimelineDoesNotExistException] {
        repository.updateDraftProduct(productName, 1, DateTime.now)
      }
    }
  }

  "findByProductName" should {

    "return timeline if it exists" in {

      val persisted = repository.createNewTimeline(productName)

      repository.findByProductName(productName) shouldBe Some(persisted)
    }

    "return None if timeline does not exist" in {

      repository.findByProductName("productName") shouldBe None
    }
  }
}
