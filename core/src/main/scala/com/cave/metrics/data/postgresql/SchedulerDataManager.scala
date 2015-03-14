package com.cave.metrics.data.postgresql

import java.sql.Timestamp

import com.cave.metrics.data.AwsConfig
import com.cave.metrics.data.postgresql.Tables._
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.driver.PostgresDriver.simple._

class SchedulerDataManager(awsConfig: AwsConfig) extends DatabaseConnection(awsConfig) {

  def leadershipTermTimeoutSeconds = awsConfig.leadershipTermTimeoutSeconds
  def leadershipTermLengthSeconds = awsConfig.leadershipTermLengthSeconds

  def DBDateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss Z")

  implicit val getSchedulersResult = GetResult(r => SchedulersRow(r.<<, r.<<, r.<<))

  /**
   * Try to extend leadership for given hostname
   *
   * @param hostname the hostname
   * @return true if successful, false otherwise
   */
  def extendLeadership(hostname: String): Boolean = {
    db.withTransaction { implicit session =>
      val sql = s"BEGIN; SELECT * FROM schedulers WHERE name = '$hostname' FOR UPDATE"
      val query = Q.queryNA[SchedulersRow](sql)

      def updateTimestampAndHostname(): Boolean = Schedulers.filter(_.name === hostname).map(_.createdAt).update(new Timestamp(System.currentTimeMillis())) == 1

      try {
        query.list.length == 1 && (updateTimestampAndHostname || {
          session.rollback()
          false
        })
      } catch {
        case e: Exception =>
          log.error(e)
          session.rollback()
          false
      }
    }
  }

  /**
   * Try to obtain leadership
   *
   * @param hostname the hostname
   * @return true if successful, false otherwise
   */
  def takeLeadership(hostname: String): Boolean = {
    db.withTransaction { implicit session =>
      val termTimeout = new DateTime().minusSeconds(leadershipTermTimeoutSeconds)
      val timeoutSql = DBDateTimeFormatter.print(termTimeout)
      val sql = s"BEGIN; SELECT * FROM schedulers WHERE created_at < '$timeoutSql' FOR UPDATE"
      val query = Q.queryNA[SchedulersRow](sql)

      def updateTimestamp(): Boolean = Schedulers.filter(_.createdAt < new Timestamp(termTimeout.getMillis))
        .map(s => (s.name, s.createdAt)).update(hostname, new Timestamp(System.currentTimeMillis())) == 1

      try {
        query.list.length == 1 &&
          (updateTimestamp() || {
            session.rollback()
            false
          })
      } catch {
        case e: Exception =>
          log.error(e)
          session.rollback()
          false
      }
    }
  }
}
