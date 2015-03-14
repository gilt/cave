package controllers

import java.util.concurrent.Executors

import com.cave.metrics.data._
import com.cave.metrics.data.evaluator.{Aggregator, ConditionEvaluator}
import org.apache.commons.logging.LogFactory
import org.joda.time.Duration
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait Metrics extends AbstractApiController {
  this: Controller =>

  implicit val defaultContext: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  private val log = LogFactory.getLog("Metrics")

  def deleteOrganizationMetric(org: String, metric: String) = MeteredAction(ServiceName, "delete-organization-metrics") {
    Action.async { request =>
      doDeleteOrganizationMetric(org, metric, request)
    }
  }

  def deleteTeamMetric(orgName: String, teamName: String, metricName: String) = MeteredAction(ServiceName, "delete-team-metrics") {
    Action.async { request =>
      doDeleteTeamMetric(orgName, teamName, metricName, request)
    }
  }

  def deleteMetric(metric: String) = MeteredAction(ServiceName, "delete-metrics") {
    Action.async { request =>
      Metric.extractOrg(request.headers.get(HOST)) match {
        case Success((orgName, None)) => doDeleteOrganizationMetric(orgName, metric, request)
        case Success((orgName, Some(teamName))) => doDeleteTeamMetric(orgName, teamName, metric, request)
        case Failure(e) => Future.successful(BadRequest(e.getMessage))
      }
    }
  }

  /**
   * API for listing organization metrics
   *
   * @param org  the name of the organization
   * @return     list of organization metrics and their tags
   */
  def listOrganizationMetrics(org: String) = MeteredAction(ServiceName, "list-organization-metrics") {
    Action.async { request =>
      getOrganizationMetricsList(org, request)
    }
  }

  /**
   * API for listing team metrics
   *
   * @param org  the name of the organization
   * @param team the name of the team
   * @return     list of team metrics and their tags
   */
  def listTeamMetrics(org: String, team: String) = MeteredAction(ServiceName, "list-team-metrics") {
    Action.async { request =>
      getTeamMetricsList(org, team, request)
    }
  }

  /**
   * API for listing metrics based on the URL
   *
   * @return     list of organization/team metrics and their tags
   */
  def listMetrics() = MeteredAction(ServiceName, "list-metrics") {
    Action.async { request =>
      Metric.extractOrg(request.headers.get(HOST)) match {
        case Success((orgName, None)) =>
          getOrganizationMetricsList(orgName, request)

        case Success((orgName, Some(teamName))) =>
          getTeamMetricsList(orgName, teamName, request)

        case Failure(e) => Future.successful(BadRequest(e.getMessage))
      }
    }
  }

  /**
   * Deletes a metric.
   * @param orgName Name of the organization
   * @param metricName Name of the metric to be deleted
   * @return
   */
  private def doDeleteOrganizationMetric(orgName: String, metricName: String, request: Request[Any]): Future[Result] = {
    withUser(request.headers) { user =>
      withOrganization(orgName) { organization =>
        withOrganizationRole(user, organization, Role.AdminsOnly) {
          deleteMetric(organization.influxCluster, Metric.createDbName(orgName, None), metricName)
        }
      }
    }
  }
  
  private def doDeleteTeamMetric(orgName: String, teamName: String, metricName: String, request: Request[Any]): Future[Result] = {
    withUser(request.headers) { user =>
      withOrganization(orgName) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.AdminsOnly) {
            deleteMetric(organization.influxCluster, Metric.createDbName(orgName, Some(teamName)), metricName)
          }
        }
      }
    }
  }

  /**
   * Extract organization and fetch metrics for it
   *
   * @param orgName   name of organization
   * @param request   the HTTP request
   * @return          list of organization metrics and their tags
   */
  private[this] def getOrganizationMetricsList(orgName: String, request: Request[Any]): Future[Result] =
    withUser(request.headers) { user =>
      withOrganization(orgName) { organization =>
        withOrganizationRole(user, organization, Role.Everyone) {
          getMetricsList(organization.influxCluster, Metric.createDbName(orgName, None))
        }
      }
    }

  /**
   * Extract organization and team, and fetch metrics for it
   *
   * @param orgName   name of organization
   * @param teamName  name of team
   * @param request   the HTTP request
   * @return          list of team metrics and their tags
   */
  private[this] def getTeamMetricsList(orgName: String, teamName: String, request: Request[Any]): Future[Result] =
    withUser(request.headers) { user =>
      withOrganization(orgName) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.Everyone) {
            getMetricsList(team.influxCluster, Metric.createDbName(orgName, Some(teamName)))
          }
        }
      }
    }

  /**
   * Internal method for getting the list for a specific Influx database
   *
   * @param database   the name of database to query
   * @return           list of metrics and their tags
   */
  private[this] def getMetricsList(cluster: Option[String], database: String): Future[Result] = {
    val (client, context) = influxClientFactory.getClient(cluster)
    client.listMetrics(database)(context) map { metrics =>
      Ok(Json.toJson(metrics))
    } recover {
      case NonFatal(e) =>
        log.warn(s"Error during listMetrics: ${e.getMessage}.")
        InternalServerError(InternalErrorMessage)
    }
  }

  /**
   * Deletes a metric on Influx.
   * @param cluster The influx where the metric data resides
   * @param database Name of the database
   * @param metricName Name of the metric to be deleted
   * @return
   */
  private def deleteMetric(cluster: Option[String], database: String, metricName: String): Future[Result] = {
    val (client, context) = influxClientFactory.getClient(cluster)
    client.deleteMetric(database, metricName)(context) map {
      case true => NoContent
      case false => NotFound
    } recover {
      case NonFatal(e) =>
        log.warn(s"Error during deleteMetric: ${e.getMessage}.")
        InternalServerError(InternalErrorMessage)
    }
  }

  /**
   * API for creating metrics for an organization
   *
   * @param orgName   the name of the organization
   * @return          result to render
   */
  def createOrganizationMetrics(orgName: String) = MeteredAction(ServiceName, "create-organization-metrics") {
    Action.async(parse.json) { request =>
      postOrganizationMetrics(orgName, request)
    }
  }

  /**
   * API for creating metrics for a team in an organization
   *
   * @param orgName   the name of the organization
   * @param teamName  the name of the team
   * @return          result to render
   */
  def createTeamMetrics(orgName: String, teamName: String) = MeteredAction(ServiceName, "create-team-metrics") {
    Action.async(parse.json) { request =>
      postTeamMetrics(orgName, teamName, request)
    }
  }

  /**
   * API for creating metrics based on URL
   *
   * @return          result to render
   */
  def createMetrics() = MeteredAction(ServiceName, "create-metrics") {
    Action.async(parse.json) { request =>
      Metric.extractOrg(request.headers.get(HOST)) match {
        case Success((orgName, None)) =>
          postOrganizationMetrics(orgName, request)

        case Success((orgName, Some(teamName))) =>
          postTeamMetrics(orgName, teamName, request)

        case Failure(e) => Future.successful(BadRequest(e.getMessage))
      }
    }
  }

  /**
   * API for evaluation of condition for an organization
   *
   * @param orgName   name of organization
   * @return          result of evaluation
   */
  def checkOrganizationMetrics(orgName: String) = MeteredAction(ServiceName, "check-organization-metrics") {
    Action.async { request =>
      evaluateOrganizationMetrics(orgName, request)
    }
  }

  /**
   * API for evaluation of condition for a team in an organization
   *
   * @param orgName   name of organization
   * @param teamName  name of team
   * @return          result of evaluation
   */
  def checkTeamMetrics(orgName: String, teamName: String) = MeteredAction(ServiceName, "check-team-metrics") {
    Action.async { request =>
      evaluateTeamMetrics(orgName, teamName, request)
    }
  }

  /**
   * API for evaluation of a condition for organization or team
   * Depends on the URL of the request
   *
   * @return          result of evaluation
   */
  def checkMetrics = MeteredAction(ServiceName, "check-metrics") {
    Action.async { request =>
      Metric.extractOrg(request.headers.get(HOST)) match {
        case Success((orgName, None)) =>
          evaluateOrganizationMetrics(orgName, request)

        case Success((orgName, Some(teamName))) =>
          evaluateTeamMetrics(orgName, teamName, request)

        case Failure(e) => Future.successful(BadRequest(e.getMessage))
      }
    }
  }

  /**
   * Internal method for evaluating organization metrics
   *
   * @param orgName    name of organization
   * @param request    the HTTP request
   * @return           result of evaluation
   */
  private[this] def evaluateOrganizationMetrics(orgName: String, request: Request[AnyContent]): Future[Result] =
    withUser(request.headers) { user =>
      withOrganization(orgName) { organization =>
        withOrganizationRole(user, organization, Role.Everyone) {
          evaluateMetrics(organization.influxCluster, Metric.createDbName(organization.name, None), request.queryString)
        }
      }
    }

  /**
   * Internal method for evaluating team metrics
   *
   * @param orgName    name of organization
   * @param teamName   name of team
   * @param request    the HTTP request
   * @return           result of evaluation
   */
  private[this] def evaluateTeamMetrics(orgName: String, teamName: String, request: Request[AnyContent]): Future[Result] =
    withUser(request.headers) { user =>
      withOrganization(orgName) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.Everyone) {
            evaluateMetrics(team.influxCluster, Metric.createDbName(organization.name, Some(team.name)), request.queryString)
          }
        }
      }
    }

  /**
   * Internal method for evaluating metrics
   *
   * @param database   the Influx database to query
   * @param params     the HTTP request parameters
   * @return           result of evaluation
   */
  private[this] def evaluateMetrics(cluster: Option[String], database: String, params: Map[String, Seq[String]]): Future[Result] = {
    MetricCheckRequest.fromQueryString(params) match {
      case Success(request) =>
        try {
          val points = new Duration(request.start, request.end).getStandardMinutes / request.interval

          if (points > 60) {
            Future.successful(BadRequest("The requested data is too large."))
          } else {
            new ConditionEvaluator(cluster, database, request).evaluate(influxClientFactory) map {
              case Some(bulk) =>
                Ok(Json.toJson(bulk))

              case None =>
                BadRequest("Cannot evaluate condition: at least one series does not exist")
            } recover {
              case NonFatal(e) =>
                log.warn("Error during evaluation: " + e.getMessage)
                InternalServerError(InternalErrorMessage)
            }
          }
        } catch {
          case NonFatal(e) =>
            log.warn("Error during parsing: " + e.getMessage)
            Future.successful(BadRequest(e.getMessage))
        }

      case Failure(reason) =>
        Future.successful(BadRequest(reason.getMessage))
    }
  }

  /**
   * API for fetching metrics for an organization
   *
   * @param orgName   the name of the organization
   * @return          result to render (JSON data)
   */
  def getOrganizationMetrics(orgName: String) = MeteredAction(ServiceName, "get-organization-metrics") {
    Action.async { request =>
      fetchOrganizationMetrics(orgName, request)
    }
  }

  /**
   * API for fetching metrics for a team of an organization
   *
   * @param orgName   the name of the organization
   * @param teamName  the name of the team
   * @return          result to render (JSON data)
   */
  def getTeamMetrics(orgName: String, teamName: String) = MeteredAction(ServiceName, "get-team-metrics") {
    Action.async { request =>
      fetchTeamMetrics(orgName, teamName, request)
    }
  }

  /**
   * API for fetching metrics based on given URL
   *
   * @return          result to render (JSON data)
   */
  def getMetrics = MeteredAction(ServiceName, "get-metrics") {
    Action.async { request =>
      Metric.extractOrg(request.headers.get(HOST)) match {
        case Success((orgName, None)) =>
          fetchOrganizationMetrics(orgName, request)

        case Success((orgName, Some(teamName))) =>
          fetchTeamMetrics(orgName, teamName, request)

        case Failure(e) => Future.successful(BadRequest(e.getMessage))
      }
    }
  }

  /**
   * Extract organization, and fetch requested metric data
   *
   * @param orgName    the name of the organization
   * @param request    the HTTP request
   * @return           the result to render
   */
  private[this] def fetchOrganizationMetrics(orgName: String, request: Request[AnyContent]): Future[Result] =
      withUser(request.headers) { user =>
        withOrganization(orgName) { organization =>
          withOrganizationRole(user, organization, Role.Everyone) {
            fetchMetrics(organization.influxCluster, Metric.createDbName(organization.name, None), request.queryString)
          }
        }
      }

  /**
   * Extract organization and team, and fetch requested metric data
   *
   * @param orgName    the name of the organization
   * @param teamName   the name of the team
   * @param request    the HTTP request
   * @return           the result to render
   */
  private[this] def fetchTeamMetrics(orgName: String, teamName: String, request: Request[AnyContent]): Future[Result] =
    withUser(request.headers) { user =>
      withOrganization(orgName) { organization =>
        withTeam(organization, teamName) { team =>
          withTeamRole(user, organization, team, Role.Everyone) {
            fetchMetrics(team.influxCluster, Metric.createDbName(organization.name, Some(team.name)), request.queryString)
          }
        }
      }
    }

  /**
   * Fetch metrics from the backend, for the given organization and team
   *
   * @param database  the name of the database to query
   * @param params    the query string parameters
   * @return          the result to render
   */
  private[this] def fetchMetrics(cluster: Option[String], database: String, params: Map[String, Seq[String]]): Future[Result] = {
    MetricRequest.fromQueryString(params) match {
      case Success(request) =>
        try {
          val (client, context) = influxClientFactory.getClient(cluster)
          client.getAggregatedData(database, Aggregator.toInflux(request.aggregator), request.period,
            request.metric, request.tags, request.start, request.end, request.limit)(context) map {

            case Success(Some(values)) =>
              Ok(Json.toJson(values))
            case Success(None) =>
              BadRequest("Unable to fetch metric data: the metric does not exist.")
            case Failure(reason) =>
              log.warn(s"Error during getMetricData: ${reason.getMessage}")
              InternalServerError(InternalErrorMessage)
          }
        } catch {
          case NonFatal(e) =>
            e.printStackTrace()
            Future.successful(InternalServerError(InternalErrorMessage))
        }

      case Failure(reason) =>
        Future.successful(BadRequest(reason.getMessage))
    }
  }

  /**
   * Extract organization, and create metrics
   *
   * @param orgName    the name of the organization
   * @param request    the HTTP request
   * @return           the result to render
   */
  private[this] def postOrganizationMetrics(orgName: String, request: Request[JsValue]): Future[Result] =
    withOrganization(orgName) { organization =>
      checkAuthorization(request.headers, organization.tokens.getOrElse(Seq())) {
        postMetrics(request.body, Metric.createDbName(organization.name, None), organization.influxCluster)
      }
    }

  /**
   * Extract organization and team, and create metrics
   *
   * @param orgName    the name of the organization
   * @param teamName   the name of the team
   * @param request    the HTTP request
   * @return           the result to render
   */
  private[this] def postTeamMetrics(orgName: String, teamName: String, request: Request[JsValue]): Future[Result] =
    withOrganization(orgName) { organization =>
      withTeam(organization, teamName) { team =>
        checkAuthorization(request.headers, List(organization.tokens, team.tokens).flatten.flatten) {
          postMetrics(request.body, Metric.createDbName(organization.name, Some(team.name)), team.influxCluster)
        }
      }
    }

  /**
   * Add the organization name to all metrics
   *
   * @param metrics  the metrics to modify
   * @param database the name of the database
   * @param cluster  the name of the influx cluster
   * @return a sequence of metrics containing the organization as an extra tag
   */
  private[this] def addDatabaseAndClusterAsTags(metrics: Seq[Metric], database: String, cluster: Option[String]): Seq[Metric] = {
    val extraTags = Seq(
      Some(Metric.Organization -> database),
      cluster.map(Metric.Cluster -> _)
    ).flatten

    metrics map { metric =>
      Metric(metric.name, adjustedTimestamp(metric.timestamp), metric.value, metric.tags ++ extraTags)
    }
  }

  /**
   * If the timestamp is too far into the future, assume it's in the wrong unit of measure
   *
   * @param timestamp  a timestamp value
   * @return           a timestamp, in seconds since the Epoch
   */
  private[this] def adjustedTimestamp(timestamp: Long): Long =
    if (timestamp > 95617584000L) adjustedTimestamp(timestamp / 1000)
    else timestamp

  /**
   * Send the metrics to the data sink, attaching the account name as an extra tag
   *
   * @param content     the content to read metrics from
   * @param database    the database name to add as an extra tag
   * @param cluster     the cluster name to add as an extra tag
   * @return            the response for this request
   */
  private[this] def postMetrics(content: JsValue, database: String, cluster: Option[String]): Future[Result] =
    withMetrics(content) { metrics =>
      dataSink.sendMetrics(addDatabaseAndClusterAsTags(metrics, database, cluster))
      Future.successful(Accepted)
    }

  /**
   * Helper method to extract MetricBulk and execute the function given on it
   *
   * @param json        the json representation of a MetricBulk
   * @param block       the function to execute on the extracted data
   * @return            result of function executed, or BadRequest if bad JSON
   */
  private[this] def withMetrics(json: JsValue)
                               (block: Seq[Metric] => Future[Result]): Future[Result] = {
    json.validate[MetricBulk] match {
      case bulk: JsSuccess[MetricBulk] => {
        val metrics = bulk.get.metrics
        if (metrics forall (_.isSane)) {
          block(metrics)
        } else {
          Future.successful(BadRequest(s"Metric has too many tags or too long name, or too long tag name/value."))
        }
      }
      case error: JsError => Future.successful(BadRequest(s"Cannot parse metrics: ${error.errors}"))
    }
  }
}

object Metrics extends Controller with Metrics