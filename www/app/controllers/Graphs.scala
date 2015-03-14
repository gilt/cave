package controllers

import com.gilt.cavellc.Client
import com.gilt.cavellc.errors.FailedRequest
import com.gilt.cavellc.models.{Role, Aggregator, GraphDatapoint, MetricDataBulk}
import controllers.helpers.{CreateAlert, CaveForms}
import org.joda.time._
import play.api.Logger
import play.api.libs.json._

import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class GraphViewData(aggregator: Aggregator, period: String, startDate: DateTime, endDate: DateTime,
                         intervalForTest: String, condition: String, tags: Map[String, String])

object GraphViewData {
  def create(aggregator: Option[Aggregator],
             period: Option[String],
             startDate: Option[DateTime],
             endDate: Option[DateTime],
             evaluateAlertPeriod: Option[String],
             condition: Option[String],
             tags: Option[String]) =
    GraphViewData(
      aggregator.getOrElse(Aggregator.Count),
      period.getOrElse("5m"),
      startDate.getOrElse(DateTime.now().minusHours(1)),
      endDate.getOrElse(DateTime.now()),
      evaluateAlertPeriod.getOrElse("5m"),
      condition.getOrElse(""),
      buildTagMap(tags))

  private def buildTagMap(tags: Option[String]): Map[String, String] = tags.getOrElse("").split(",").map(_.split(":")).filter(_.length == 2).map(tag => tag(0) -> tag(1)).toMap
}


class Graphs extends AbstractCaveController {

  val MaximumDataPointsPerCall = 256
  val MaximumDataPointsPerGraph = 4096

  private type MetricFetcher = (DateTime, DateTime) => Future[Option[MetricDataBulk]]

  def graphForOrganization(organizationName: String, metric: String,
                           aggregator: Option[Aggregator] = None,
                           period: Option[String] = None,
                           startDate: Option[DateTime] = None,
                           endDate: Option[DateTime] = None,
                           intervalForTest: Option[String] = None,
                           condition: Option[String] = None,
                           tags: Option[String] = None) = caveAsyncAction { implicit request =>

    val selections = GraphViewData.create(aggregator, period, startDate, endDate, intervalForTest, condition, tags)

    val breadcrumb = ListMap("Home" -> routes.Application.index,
      organizationName -> routes.Organizations.organization(organizationName),
      "metrics" -> routes.Metrics.organizationMetrics(organizationName),
      metric -> routes.Graphs.graphForOrganization(organizationName, metric))
    val createAlertForm = CaveForms.createAlertForm.fill(CreateAlert(organizationName, EMPTY_STRING, false, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, None))
    withCaveClient { client =>
      for {
        metricInfo <- client.Metrics.getOrganizationsAndMetricNamesByOrganization(organizationName) if metricInfo.exists(_.name == metric)
        tags <- Future.successful(metricInfo.filter(_.name == metric).map(_.tags).flatten.sorted)
        role <- client.Users.getOrganizations().map(_.filter(_.name == organizationName).map(_.role).headOption.getOrElse(Role.Viewer))
      } yield Ok(views.html.graphs.graph(organizationName, None, metric, breadcrumb, tags, role, createAlertForm, selections))
    }
  }

  def graphForTeam(organizationName: String, teamName: String, metric: String,
                   aggregator: Option[Aggregator] = None,
                   period: Option[String] = None,
                   startDate: Option[DateTime] = None,
                   endDate: Option[DateTime] = None,
                   intervalForTest: Option[String] = None,
                   condition: Option[String] = None,
                   tags: Option[String] = None) = caveAsyncAction { implicit request =>

    val selections = GraphViewData.create(aggregator, period, startDate, endDate, intervalForTest, condition, tags)

    withCaveClient { client =>
      val createAlertForm = CaveForms.createAlertForm.fill(CreateAlert(organizationName, teamName, false, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, None))
      for {
        organizations <- client.Users.getOrganizations()
        metricInfo <- client.Metrics.getOrganizationsAndTeamsAndMetricNamesByOrganizationAndTeam(organizationName, teamName) if metricInfo.exists(_.name == metric)
        tags <- Future.successful(metricInfo.filter(_.name == metric).map(_.tags).flatten.sorted)
        roleInOrganization <- Future.successful(organizations.filter(_.name == organizationName).head.role)
        userTeams <- getUserTeams(organizations, client)
        role <- getRoleInTeam(organizationName, teamName, userTeams, roleInOrganization)
        breadcrumb <- Future.successful(ListMap(
          "Home" -> routes.Application.index,
          organizationName -> buildOrganizationUrlForBreadcrumb(organizationName, organizations),
          teamName -> routes.Teams.team(organizationName, teamName),
          "metricInfo" -> routes.Metrics.teamMetrics(organizationName, teamName),
          metric -> routes.Graphs.graphForTeam(organizationName, teamName, metric)))
      } yield Ok(views.html.graphs.graph(organizationName, Some(teamName), metric, breadcrumb, tags, role, createAlertForm, selections))
    }
  }

  def checkAlertConditionForOrganization(orgName: String, period: Int, condition: String, metric: String,
                                         start: DateTime, end: Option[DateTime]) = caveAsyncActionJson { implicit request =>
    def getData(start: DateTime, end: DateTime)(implicit client: Client) =
      client.Metrics.getOrganizationsAndCheckMetricsByOrganization(orgName, condition, start, Some(end), Some(period))

    withCaveClient { implicit client =>
      getMetricData(period, start, end.getOrElse(new DateTime()))(getAllDataInOneApiCall(getData))
    }
  }

  def checkAlertConditionForTeam(orgName: String, teamName: String, period: Int, condition: String, metric: String,
                                 start: DateTime, end: Option[DateTime]) = caveAsyncActionJson { implicit request =>
    def getData(start: DateTime, end: DateTime)(implicit client: Client) =
      client.Metrics.getOrganizationsAndTeamsAndCheckMetricsByOrganizationAndTeam(orgName, teamName, condition, start, Some(end), Some(period))

    withCaveClient { implicit client =>
      getMetricData(period, start, end.getOrElse(new DateTime()))(getAllDataInOneApiCall(getData))
    }
  }

  def dataForTeam(orgName: String, teamName: String, period: Int, aggregator: Aggregator, metric: String,
                  tags: Option[String], start: DateTime,
                  end: DateTime) = caveAsyncActionJson { implicit request =>

    def getData(start: DateTime, end: DateTime)(implicit client: Client) =
      client.Metrics.getOrganizationsAndTeamsByOrganizationAndTeam(orgName, teamName, metric, tags, aggregator, period, Some(start), Some(end), Some(MaximumDataPointsPerCall))

    if (isValidRequest(start, end, period)) {
      withCaveClient { implicit client =>
        getMetricData(period, start, end)(getDataInMultipleApiCalls(getData))
      }
    } else Future.successful(BadRequest("Too much data requested."))
  }


  def dataForOrganization(orgName: String, period: Int, aggregator: Aggregator, metric: String, tags: Option[String],
                          start: DateTime,
                          end: DateTime) = caveAsyncActionJson { implicit request =>

    def getData(start: DateTime, end: DateTime)(implicit client: Client) =
      client.Metrics.getOrganizationsByOrganization(orgName, metric, tags, aggregator, period, Some(start), Some(end), Some(MaximumDataPointsPerCall))

    if (isValidRequest(start, end, period)) {
      withCaveClient { implicit client =>
        getMetricData(period, start, end)(getDataInMultipleApiCalls(getData))
      }
    } else Future.successful(BadRequest("Too much data requested."))
  }

  private[controllers] def isValidRequest(start: DateTime, end: DateTime, periodInMinutes: Int): Boolean = {
    val requested = new Duration(start, end).getStandardMinutes / periodInMinutes
    (requested <= MaximumDataPointsPerGraph)
  }

  private[controllers] def getAllDataInOneApiCall(getData: MetricFetcher)(timestamps: Seq[DateTime]) = {
    val sections = Seq(timestamps)
    sections.map { section =>
      getData(section.head, section.last)
    }
  }

  private[controllers] def getDataInMultipleApiCalls(getData: MetricFetcher)(timestamps: Seq[DateTime]) = {
    val sections = timestamps.sliding(MaximumDataPointsPerCall, MaximumDataPointsPerCall - 1).toSeq
    sections.map { section =>
      getData(section.head, section.last)
    }
  }

  private[controllers] def getMetricData(periodInMinutes: Int, start: DateTime, end: DateTime)
                                        (slicedResults: Seq[DateTime] => Seq[Future[Option[MetricDataBulk]]]) = {
    val period = Period.minutes(periodInMinutes)
    val startOfAggregation =
      if (periodInMinutes == 1) // for aggregation evert 1 min
        start.withMillisOfSecond(0).withSecondOfMinute(0)
      else if (periodInMinutes > 1 && periodInMinutes < 60) // for aggregation evert (1 to 60) min - range exclusive
        start.withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0)
      else // for aggregation every 1 h and more
        start.withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0).withHourOfDay(0)


    val endOfAggregation = end.minusSeconds(1)
    val datapointTimestamps = dateRange(startOfAggregation, endOfAggregation, period).filter(_.isAfter(start.minusSeconds(1))).toSeq

    Future.sequence(slicedResults(datapointTimestamps)).map(_.flatten) map { metricDataBulks =>
      val datapointsFound = metricDataBulks.map(_.metrics).flatten
      val datapoints = datapointTimestamps.map(ts =>
        GraphDatapoint(ts, datapointsFound.find(_.time.equals(ts)).map(_.value))
      )

      Ok(Json.toJson(datapoints))
    } recover {
      case e: FailedRequest =>
        // FIXME: this error happens due to issues with Influx when requested multiple data points
        Logger.error(s"get Metrics call failed\nAPI CALL: ", e)
        InternalServerError(e.message)
    }
  }

  @tailrec
  private[controllers] final def findStartDate(startResultDate: DateTime, expectedStartDate: DateTime, period: Period): DateTime = {
    if (startResultDate.isAfter(expectedStartDate))
      findStartDate(startResultDate.minus(period), expectedStartDate, period)
    else
      startResultDate
  }

  private def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = {
    Iterator.iterate(from.withSecondOfMinute(0).withMillisOfSecond(0))(_.plus(step)).takeWhile(!_.isAfter(to.plusSeconds(1))).take(MaximumDataPointsPerGraph)
  }
}

