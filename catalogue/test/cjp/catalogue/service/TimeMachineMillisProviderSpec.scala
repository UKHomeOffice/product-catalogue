package cjp.catalogue.service

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mock.MockitoSugar
import cjp.catalogue.repository.EffectiveDateRepository
import org.mockito.Mockito.when
import org.joda.time.DateTime

class TimeMachineMillisProviderSpec extends WordSpec with Matchers with MockitoSugar {

  "getMillis" should {
    "return system time if effective date not set in db" in {
      val effectiveDateRepo = mock[EffectiveDateRepository]
      when(effectiveDateRepo.find).thenReturn(None)

      new TimeMachineMillisProvider(effectiveDateRepo).getMillis should be (System.currentTimeMillis() +- 10)
    }

    "return persisted datetime" in {
      val effectiveDateRepo = mock[EffectiveDateRepository]
      val effectiveDate = DateTime.now.plusDays(1)
      when(effectiveDateRepo.find).thenReturn(Some(effectiveDate))

      new TimeMachineMillisProvider(effectiveDateRepo).getMillis should be (effectiveDate.getMillis)
    }
  }
}
