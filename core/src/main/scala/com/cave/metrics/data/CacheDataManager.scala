package com.cave.metrics.data

import scala.util.Try

trait CacheDataManager extends DataManager {


  /**
   * Retrieve all configured alerts
   *
   * @return               a list of organization names and their enabled alerts (with associated information)
   */
  def getEnabledAlerts(): Try[Map[String, List[Schedule]]]

}
