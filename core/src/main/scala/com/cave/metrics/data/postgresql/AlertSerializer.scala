package com.cave.metrics.data.postgresql

import com.cave.metrics.data.Alert
import com.cave.metrics.data.postgresql.Tables.AlertsRow

object AlertSerializer {
  def fromPostgresRecord(alert:AlertsRow): Alert = {
    new Alert(Some(alert.id.toString), alert.description, alert.status.get, alert.period, alert.condition, alert.handbookUrl, Alert.routingFromString(alert.routing))
  }
}
