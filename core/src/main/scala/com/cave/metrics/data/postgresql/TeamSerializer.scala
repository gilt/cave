package com.cave.metrics.data.postgresql

import com.cave.metrics.data.{Team, Token}

object TeamSerializer {
  def fromPostgresRecord(list: List[(Tables.TokensRow, Tables.TeamsRow)]) = {
    if (list.nonEmpty) {
      val tokens: List[Token] = TokenSerializer.fromPostgresRecord(list.map(_._1).toList)
      val team: Tables.TeamsRow = list.head._2
      Some(new Team(Some(team.id.toString), team.name, Some(tokens), team.cluster))
    }
    else None
  }
}
