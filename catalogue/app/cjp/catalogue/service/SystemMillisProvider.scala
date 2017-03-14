package cjp.catalogue.service

import org.joda.time.DateTimeUtils.MillisProvider

class SystemMillisProvider extends MillisProvider {
  /**
   * Gets the current time.
   * @return the current time in millis
   */
  def getMillis: Long = {
    return System.currentTimeMillis
  }
}