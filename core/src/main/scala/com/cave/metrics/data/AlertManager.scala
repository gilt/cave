package com.cave.metrics.data

import com.cave.metrics.data.evaluator.{Aggregator, AlertParser}
import com.cave.metrics.data.influxdb.{InfluxClient, InfluxClientFactory}
import org.apache.commons.logging.LogFactory

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

/**
 * Manager for creating and fetching alerts.
 * As well as persisting and retrieving the alert metadata, it parses alert conditions and generates associated
 * queries in InfluxDB.
 *
 * @param dataManager - manager used for persisting and retrieving the raw alert metadata
 * @param influxClientFactory - factory for getting hold of an InfluxDB client
 */
class AlertManager(dataManager: DataManager, influxClientFactory: InfluxClientFactory) extends AlertParser {

  private val log = LogFactory.getLog(classOf[AlertManager])
  private val Timeout: Duration = 5.seconds

  private def addRelatedMetrics(alerts: Seq[Alert]): Seq[Alert] = alerts map addRelatedMetrics
  private def addRelatedMetrics(alert: Option[Alert]): Option[Alert] = alert map addRelatedMetrics
  private def addRelatedMetrics(alert: Alert): Alert = alert.copy(relatedMetrics = Some(parseMetrics(alert)))

  // Parse the alert condition for related metrics
  private def parseMetrics(alert: Alert): Set[AlertMetric] = {
    def metricInfoFromSource(source: Source): Option[AlertMetric] = source match {
      case MetricSource(metric, tags) => Some(AlertMetric(metric, tags, None, None))
      case AggregatedSource(MetricSource(metric, tags), aggregator, duration) => Some(AlertMetric(metric, tags, Some(aggregator.toString), Some(duration.toSeconds)))
      case FactoredSource(s, _) => metricInfoFromSource(s) // This shouldn't be infinite!
      case _ => None
    }

    parseAll(anyAlert, alert.condition) match {
      case Success(SimpleAlert(left, _, right, _, _), _) =>
        Set(metricInfoFromSource(left), metricInfoFromSource(right)).flatten
      case Success(MissingDataAlert(metricSource, _), _) =>
        metricInfoFromSource(metricSource).toSet
      case Failure(message, _) =>
        Set.empty[AlertMetric]
    }
  }

  def getOrganizationAlerts(organization: Organization, limit: Int = 20, offset: Int = 0): Try[Seq[Alert]] = {
    dataManager.getOrganizationAlerts(organization, limit, offset) map addRelatedMetrics
  }

  def getTeamAlerts(organization: Organization, team: Team, limit: Int = 20, offset: Int = 0): Try[Seq[Alert]] = {
    dataManager.getTeamAlerts(organization, team, limit, offset) map addRelatedMetrics
  }

  def getAlert(alertId: String): Try[Option[Alert]] = {
    dataManager.getAlert(alertId) map addRelatedMetrics
  }

  def updateAlert(alert: Alert, alertPatch: AlertPatch): Try[Option[Alert]] = {
    dataManager.updateAlert(alert, alertPatch) map addRelatedMetrics
  }

  def deleteAlert(alertId: String) = dataManager.deleteAlert(alertId)

  def createTeamAlert(org: Organization, team: Team, alert: Alert): Try[Option[Alert]] = createAlert(org, Some(team), alert)

  def createOrganizationAlert(org: Organization, alert: Alert): Try[Option[Alert]] = createAlert(org, None, alert)

  private def createAlert(organization: Organization, team: Option[Team], alert: Alert): Try[Option[Alert]] = {
    implicit val dbName = team.map(_.name + ".").getOrElse("") + organization.name
    val clusterName = team map(_.influxCluster) getOrElse organization.influxCluster
    implicit val (client, context) = influxClientFactory.getClient(clusterName)

    parseAll(duration, alert.period) match {
      case Success(_, _) =>
        parseAll(anyAlert, alert.condition) match {
          case Success(SimpleAlert(left, _, right, _, _), _) =>
            val requiredQueries = List(left, right) flatMap getQueryName
            val queriesToBeCreated = requiredQueries.toMap.filterKeys(queryDoesNotExistInDb)

            if (queriesToBeCreated.map(createContinuousQuery).forall(x => x)) {
              createDbEntities(organization, team, alert, requiredQueries)
            } else {
              scala.util.Failure(new RuntimeException("Unable to create Continuous Queries in Influx."))
            }

          case Success(MissingDataAlert(metricSource, duration), _) =>
            createDbEntities(organization, team, alert, List.empty[(String, AggregatedSource)])

          case Failure(message, _) =>
            scala.util.Success(None)
        }

      case NoSuccess(msg, _) =>
        scala.util.Failure(new RuntimeException(s"Cannot parse alert period: ${alert.period}"))
    }
  }

  def createDbEntities(organization: Organization, teamOpt: Option[Team],
                       alert: Alert, requiredQueries: List[(String, AggregatedSource)]): Try[Option[Alert]] =
    teamOpt match {
      case Some(team) => dataManager.createTeamAlert(organization, team, alert, requiredQueries.toMap.keySet)
      case None => dataManager.createOrganizationAlert(organization, alert, requiredQueries.toMap.keySet)
    }

  private def getQueryName(src: Source): Option[(String, AggregatedSource)] = src match {
      case a@AggregatedSource(_, _, _) => Some(a.toString -> a)
      case FactoredSource(source, _) => source match {
        case a@AggregatedSource(_, _, _) => Some(a.toString -> a)
        case _ => None
      }
      case _ => None
    }

  private def queryDoesNotExistInDb(query: String): Boolean = dataManager.queryDoesNotExist(query)

  private def createContinuousQuery(source: (String, AggregatedSource))
                                   (implicit database: String, client: InfluxClient, context: ExecutionContext): Boolean = source match {
    case (queryName, src) =>
      val aggregator = Aggregator.toInflux(src.aggregator)
      val metricSource = src.metricSource.metric
      val period = src.duration
      val tags = src.metricSource.tags
      try {
        Await.result(client.createContinuousQuery(metricSource, tags, aggregator, period, queryName, database)(context), Timeout)
      } catch {
        case e: TimeoutException =>
          log.warn(s"Unable to create Continuous Query: ${e.getMessage}", e)
          false
      }
  }
}
