package com.cave.metrics.data.postgresql

import com.cave.metrics.data._

import scala.slick.driver.PostgresDriver.simple._
import scala.util.Try

class PostgresCacheDataManagerImpl(awsConfig: AwsConfig) extends PostgresDataManagerImpl(awsConfig) with CacheDataManager {

  /**
   * Retrieve all organizations and their enabled alerts
   *
   * @return   a list of enabled alerts and their associated information, grouped by org
   */
  override def getEnabledAlerts(): Try[Map[String, List[Schedule]]] = {
    Try {
      val data = db.withTransaction { implicit session =>

        val result = for {
          ((org, alert), team) <- organizationsTable.filter(_.deletedAt.isEmpty) leftJoin alertsTable.filter( a => a.deletedAt.isEmpty && a.status) on (_.id === _.organizationId) leftJoin teamsTable.filter(_.deletedAt.isEmpty) on (_._2.teamId === _.id)
        } yield (org, team.?, alert.?)

        result.list.map { case (organization, maybeTeam, maybeAlert) =>
          val alert = maybeAlert map { row =>
              Alert(Some(row.id.toString), row.description, row.status.getOrElse(false), row.period, row.condition, row.handbookUrl, Alert.routingFromString(row.routing))
          }

          organization.name -> alert.map(model => Schedule(organization.name, maybeTeam.map(_.name), maybeTeam.map(_.cluster) getOrElse(organization.cluster), organization.notificationUrl, model))
        }
      }

      data.groupBy(_._1).mapValues(_.map(_._2).flatten)
    }
  }
}
