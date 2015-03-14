package com.cave.metrics.data.postgresql

import com.cave.metrics.data.Token
import org.joda.time.DateTime

object TokenSerializer {
  def fromPostgresRecord(rows: List[Tables.TokensRow]): List[Token] = {
    rows.map(r => new Token(Some(r.id.toString), r.description, r.value, new DateTime(r.createdAt))).toList
  }
}
