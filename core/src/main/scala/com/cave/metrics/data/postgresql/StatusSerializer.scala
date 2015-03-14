package com.cave.metrics.data.postgresql

import com.cave.metrics.data.{CaveIssue, CaveStatus}
import org.joda.time.DateTime

object StatusSerializer {

  def fromPostgresRecord(list: List[Tables.StatusRow]): CaveStatus = {
    val (recent, current) = list.map(record =>
      CaveIssue(record.description, new DateTime(record.since), record.until.map(ts => new DateTime(ts)))
    ).partition(_.until.isDefined)

    CaveStatus(current, recent)
  }
}
