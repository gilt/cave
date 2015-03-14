package com.cave.metrics.data.postgresql

import com.cave.metrics.data.{Token, Organization}

object OrganizationSerializer {
  def fromPostgresRecord(list: List[(Tables.TokensRow, Tables.OrganizationsRow)]) = {
    if (list.nonEmpty) {
      val tokens: List[Token] = TokenSerializer.fromPostgresRecord(list.map(_._1).toList)
      val org: Tables.OrganizationsRow = list.head._2
      Some(new Organization(Some(org.id.toString), org.name, org.email, org.notificationUrl, Some(tokens), org.cluster))
    }
    else None
  }
}
