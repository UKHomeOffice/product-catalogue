package cjp.catalogue.model

import org.scalatest.{Matchers, WordSpec}
import org.joda.time.DateTime

class ProductTimelineSpec extends WordSpec with Matchers {

  "findEffectiveVersion" should {

    "returning the product version with the latest from date" in {

      val oldestProductVersion = TimelineVersion(1, DateTime.now.minusDays(4))
      val currentProductVersion = TimelineVersion(2, DateTime.now)
      val futureProductVersion = TimelineVersion(3, DateTime.now.plusDays(2))
      val productTimeLine = ProductTimeline(productName = "prodA",
        versions = List(oldestProductVersion, currentProductVersion, futureProductVersion))

      productTimeLine.findEffectiveVersion(DateTime.now) should be(Some(currentProductVersion))
    }

    "return the current version even if versions are not in chronological order" in {

      val currentProductVersion = TimelineVersion(1, DateTime.now)
      val oldestProductVersion = TimelineVersion(2, DateTime.now.minusDays(4))
      val futureProductVersion = TimelineVersion(3, DateTime.now.plusDays(2))

      val productTimeLine = ProductTimeline(productName = "prodA",
        versions = List(oldestProductVersion, futureProductVersion, currentProductVersion))

      productTimeLine.findEffectiveVersion(DateTime.now) should be(Some(currentProductVersion))

    }

    "return None when there is no product version for a given effective date" in {
      val expectedProductVersion = TimelineVersion(1, DateTime.now.minusDays(4))
      val productTimeLine = ProductTimeline(productName = "prodA",
        versions = List(expectedProductVersion))

      productTimeLine.findEffectiveVersion(DateTime.now.minusDays(5)) should be(None)

    }

  }

  "findDraftVersion" should {

    "return a draft version" in {

      val oldestProductVersion = TimelineVersion(1, DateTime.now.minusDays(4))
      val currentProductVersion = TimelineVersion(2, DateTime.now)
      val futureProductVersion = TimelineVersion(3, DateTime.now.plusDays(2))

      val productTimeLine = ProductTimeline(productName = "prodA",
        versions = List(oldestProductVersion, currentProductVersion, futureProductVersion))

      productTimeLine.findDraftVersion should be(Some(futureProductVersion))

    }

    "return None if there are no draft versions" in {
      val oldestProductVersion = TimelineVersion(1, DateTime.now.minusDays(4))
      val currentProductVersion = TimelineVersion(2, DateTime.now)

      val productTimeLine = ProductTimeline(productName = "prodA",
        versions = List(oldestProductVersion, currentProductVersion))

      productTimeLine.findDraftVersion should be(None)

    }

    "return a draft version even if versions are not in chronological order" in {

      val oldestProductVersion = TimelineVersion(1, DateTime.now.minusDays(4))
      val currentProductVersion = TimelineVersion(2, DateTime.now)
      val futureProductVersion = TimelineVersion(3, DateTime.now.plusDays(2))

      val productTimeLine = ProductTimeline(productName = "prodA",
        versions = List(currentProductVersion, futureProductVersion, oldestProductVersion))

      productTimeLine.findDraftVersion should be(Some(futureProductVersion))

    }
  }
}
