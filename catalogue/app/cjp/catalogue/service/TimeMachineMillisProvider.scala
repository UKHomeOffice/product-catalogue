package cjp.catalogue.service

import org.joda.time.DateTimeUtils.MillisProvider
import cjp.catalogue.repository.EffectiveDateRepository

class TimeMachineMillisProvider(effectDateRepository: EffectiveDateRepository) extends MillisProvider {
  override def getMillis: Long = effectDateRepository.find.fold(System.currentTimeMillis())(_.getMillis)
}
