package com.cave.metrics.data.influxdb

import java.util.concurrent.TimeUnit

import com.cave.metrics.data.{MetricInfo, MetricData, MetricDataBulk, Metric}
import com.ning.http.client._
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpStatus
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class InfluxClusterConfig(url: String, user: String, pass: String) {
  override def toString = s"{ url: '$url', user: '$user', pass: '$pass' }"
}

class InfluxClient(config: InfluxClusterConfig) {

  private val client = new AsyncHttpClient

  private val log = LogFactory.getLog(classOf[InfluxClient])

  private val MaxAttempts = 2
  private val Timeout: FiniteDuration = 60.seconds
  private val RetryDelay: FiniteDuration = 2.seconds
  private val MaxDataPointsPerCall: Int = 1440

  log.info(s"Created a new Influx Client for ${config.url}, using ${config.user}")

  private[this] def prepareRequest(method: String, path: String): RequestBuilder = {
    val rb = new RequestBuilder(method)
      .setUrl(config.url + path)
      .addQueryParameter("u", config.user)
      .addQueryParameter("p", config.pass)
      .addQueryParameter("time_precision", "s")

    rb
  }

  private[this] def attemptRequest(request: Request, attempt: Int = 1): Response = {
    log.info(s"[${Thread.currentThread().getName}] Request: ${request.getUrl}")
    val result = client.executeRequest(request).get(Timeout.toSeconds, TimeUnit.SECONDS)
    val responseMessage = s"[${Thread.currentThread().getName}] Response[${result.getStatusCode}]: ${result.getResponseBody}"
    if (result.getStatusCode >= HttpStatus.SC_BAD_REQUEST) {
      log.warn(responseMessage)
      if (attempt < MaxAttempts) {
        Thread.sleep(RetryDelay.toMillis)
        attemptRequest(request, attempt + 1)
      } else result
    } else {
      log.info(responseMessage)
      result
    }
  }

  private[this] def executeRequest(request: Request)(implicit ec: ExecutionContext): Future[Response] = {
    future {
      attemptRequest(request)
    }
  }

  private[this] def POST(path: String, data: JsValue)
                        (implicit ec: ExecutionContext): Future[Response] =
    executeRequest(
      prepareRequest("POST", path)
        .setBody(Json.stringify(data))
        .addHeader(CONTENT_TYPE, "application/json")
        .build())

  private[this] def GET(path: String, args: Map[String, String] = Map())
                       (implicit ec: ExecutionContext): Future[Response] = {
    val requestBuilder = prepareRequest("GET", path)
    args foreach { case (key, value) =>
      requestBuilder.addQueryParameter(key, value)
    }

    executeRequest(requestBuilder.build())
  }

  private[this] def DELETE(path: String)
                          (implicit ec: ExecutionContext): Future[Response] = {
    executeRequest(prepareRequest("DELETE", path).build())
  }

  private[this] def error(response: Response) = Failure(new RuntimeException(response.getStatusCode match {
    case 401 => "Unauthorized"
    case 403 => "Forbidden"
    case _ => response.getResponseBody
  }))

  /**
   * Must call this to release any outstanding connections
   */
  def close(): Unit = client.close()

  import Models._

  /**
   * Create a database with the given name
   *
   * @param name  the name for the database to create
   * @return      true, if created; false if conflict; failure if error
   */
  def createDatabase(name: String)(implicit ec: ExecutionContext): Future[Try[Boolean]] = {
    log.info(s"Creating Influx Db '$name'...")

    // must pass the cluster admin user/pass from config
    POST("/db", Json.toJson(CreateDatabase(name))) map {
      case r if r.getStatusCode == 201 => Success(true)
      case r if r.getStatusCode == 409 => Success(false)
      case r => error(r)
    }
  }

  /**
   * Delete the database with the given name
   *
   * @param name  the name of the database to delete
   * @return      true if deleted; false if not found; failure if error
   */
  def deleteDatabase(name: String)(implicit ec: ExecutionContext): Future[Try[Boolean]] = {
    log.info(s"Deleting Influx Db '$name'...")

    // must pass the cluster admin user/pass from config
    DELETE(s"/db/$name") map {
      case r if r.getStatusCode == 204 => Success(true)
      case r if r.getStatusCode == 400 => Success(false)
      case r => error(r)
    }
  }

  /**
   * Fetch data from specified database, for given metric, tags and time
   * The number of data points returned is limited with either
   * a limit parameter or a timeRange parameter
   *
   * @param database   the database to query
   * @param metric     the metric name
   * @param tags       the metric tags to match
   * @param start      start of the time range (optional)
   * @param end        end of the time range (optional)
   * @param limit      a number of data points to return
   * @return           the values and their timestamps
   */
  def getMetricData(database: String, metric: String, tags: Map[String, String],
                    start: Option[DateTime], end: Option[DateTime],
                    limit: Option[Int])
                   (implicit ec: ExecutionContext): Future[Try[Option[MetricDataBulk]]] = {
    val query: String = buildQuery(metric, tags, start, end, limit)
    log.info("Query: " + query)

    GET(s"/db/$database/series", Map("q" -> query)) map {
      case r if r.getStatusCode == 200 =>
        Success(Some(createResponse(r.getResponseBody)))

      case r if r.getStatusCode == 400 && r.getResponseBody.contains("Couldn't find series") =>
        Success(None)

      case r => error(r)
    }
  }

  def getAggregatedData(database: String, aggregator: String, period: FiniteDuration,
                        metric: String, tags: Map[String, String],
                        start: Option[DateTime], end: Option[DateTime],
                        limit: Option[Int])(implicit ec: ExecutionContext): Future[Try[Option[MetricDataBulk]]] = {
    val query: String = buildAggregatedQuery(aggregator, period, metric, tags, start, end, limit)
    log.info(s"Query: $query")

    GET(s"/db/$database/series", Map("q" -> query)) map {
      case r if r.getStatusCode == 200 =>
        Success(Some(createResponse(r.getResponseBody)))

      case r if r.getStatusCode == 400 && r.getResponseBody.contains("Couldn't find series") =>
        Success(None)

      case r => error(r)
    }
  }

  def listMetrics(database: String)(implicit ec: ExecutionContext): Future[Seq[MetricInfo]] = {
    val query = "select * from /.*/ limit 1"
    log.info(s"About to query all metrics for $database...")

    GET(s"/db/$database/series", Map("q" -> query)) map {
      case r if r.getStatusCode == 200 =>
        createMetricInfoResponse(r.getResponseBody)

      case r => sys.error(r.getResponseBody)
    }
  }

  def deleteMetric(database: String, metricName: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val query = s"drop series $metricName"
    log.info(s"About to delete metric $metricName on database $database...")

    GET(s"/db/$database/series", Map("q" -> query)).map {
      case r if r.getStatusCode == 200 => true
      case r if r.getStatusCode == 404 => false
      case r => sys.error(r.getResponseBody)
    }
  }

  private[this] def limitCeiling(limit: Option[Int]): Int = limit match {
    case Some(num) if num > MaxDataPointsPerCall => MaxDataPointsPerCall
    case Some(num)             => num
    case None                  => MaxDataPointsPerCall
  }

  private[influxdb] def buildAggregatedQuery(aggregator: String, period: FiniteDuration,
                                             metric: String, tags: Map[String, String],
                                             start: Option[DateTime], end: Option[DateTime], limit: Option[Int]): String = {
    val where = buildWhereClause(getTagsClauses(tags) ++ getTimeClauses(start, end))
    s"""select $aggregator from "$metric"$where group by time(${period.toSeconds}s) limit ${limitCeiling(limit)}"""
  }

  private[influxdb] def buildQuery(metric: String, tags: Map[String, String], start: Option[DateTime], end: Option[DateTime], limit: Option[Int]): String = {
    val whereClause = buildWhereClause(getTagsClauses(tags) ++ getTimeClauses(start, end))
    s"""select value from "$metric"$whereClause limit ${limitCeiling(limit)}"""
  }

  private[influxdb] def buildWhereClause(items: List[String]) =
    if (items.length > 0) s" where ${items.mkString(" and ")}" else ""

  /**
   * Creates Continuous Query in Influx
   *
   * @param metric      the metric name to query
   * @param tags        the metric tags for the query
   * @param aggregator  the aggregating function to apply
   * @param period      the period of aggregation
   * @param database    the database holding the data
   * @return            true if successful, false otherwise
   */
  def createContinuousQuery(metric: String, tags: Map[String, String], aggregator: String, period: FiniteDuration, queryName: String, database: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    GET(s"/db/$database/series", Map("q" -> createSQL(metric, tags, aggregator, period, queryName))) map {
      case r if r.getStatusCode == 200 => true
      case r =>
        log.warn("Failed to create Continuous Query. Code: " + r.getStatusCode + ", message: " + r.getResponseBody)
        false
    }
  }

  private[influxdb] def createSQL(metric: String, tagsMap: Map[String, String], aggregator: String, period: FiniteDuration, queryName: String): String =
    s"""select $aggregator as value from "$metric"${buildWhereClause(getTagsClauses(tagsMap))} group by time(${period.toSeconds}s) into "$queryName""""


  /**
   * Persists metric data to the appropriate databases
   *
   * @param metrics  a sequence of metrics data points
   * @return         for each database:
   *                 true if persisted, failure if error
   */
  def putMetricData(metrics: Seq[Metric])(implicit ec: ExecutionContext): Seq[Future[Try[Boolean]]] = {
    val results = InfluxMetric.prepareRequests(metrics) map { case (database, json) =>
      log.info(s"Database: $database, JSON: $json")

      POST(s"/db/$database/series", Json.parse(json)) map {
        case r if r.getStatusCode == 200 => Success(true)
        case r => error(r)
      }
    }

    results.toSeq
  }

  /**
   * Create a SQL where clause to match all given tags
   *
   * @param tags  the tags as a map of key-value pairs
   * @return      a properly constructed WHERE clause
   */
  private[influxdb] def getTagsClauses(tags: Map[String, String]): List[String] =
    (tags map { case (k, v) => k + "='" + v + "'"}).toList

  private[influxdb] def createResponse(responseBody: String): MetricDataBulk =
    MetricDataBulk((Json.parse(responseBody).as[GetMetricDataResponse].points map { case (date, value) =>
      new MetricData(date, value)
    }).toSeq)

  /**
   * Turn the response into a list of MetricInfo entities
   *
   * @param responseBody  the response for the 'list series' query
   * @return              list of metric names and their tags
   */
  private[influxdb] def createMetricInfoResponse(responseBody: String): Seq[MetricInfo] =
    Json.parse(responseBody) match {
      case JsArray(sequence) if sequence.length > 0 =>
        val metrics = sequence map { value =>
          MetricInfo(
            name = (value \ "name").as[String],
            tags = (value \ "columns").as[List[String]].filterNot(tag => List("time", "sequence_number", "value").contains(tag))
          )
        }
        // remove the generated metrics for continuous queries alerts
        metrics.filterNot(_.name.size == 128)

      case _ => Seq.empty[MetricInfo]
    }

  private final val DTFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  /**
   * Create a SQL clause to match the given time range
   *
   * @param start     start of the time range (optional)
   * @param end       end of the time range (optional)
   * @return           the condition for a SQL statement to match the range
   */
  private[influxdb] def getTimeClauses(start: Option[DateTime], end: Option[DateTime]): List[String] = {

    def print(date: DateTime): String = DTFormat.print(date.withZone(DateTimeZone.UTC))

    List(
      start map (startDate => s"time > '${print(startDate)}'"),
      end map (endDate => s"time < '${print(endDate)}'")
    ).flatten
  }

  object Models {

    case class CreateDatabase(name: String, replicationFactor: Int)

    case class DatabaseUser(name: String, password: String)

    case class GetMetricDataResponse(name: String, points: Map[DateTime, Double])

    object CreateDatabase {

      def apply(name: String) =
        new CreateDatabase(name, 1)

      implicit val reads: Reads[CreateDatabase] = new Reads[CreateDatabase] {
        val impl = { json: JsValue =>
          new CreateDatabase(
            (json \ "name").as[String],
            (json \ "replicationFactor").asOpt[Int].getOrElse(1))
        }

        def reads(json: JsValue) = {
          try {
            JsSuccess(impl(json))
          } catch {
            case e: Exception => JsError(e.getMessage)
          }
        }
      }

      implicit val writes: Writes[CreateDatabase] = new Writes[CreateDatabase] {
        def writes(value: CreateDatabase) = Json.obj(
          "name" -> JsString(value.name),
          "replicationFactor" -> JsNumber(value.replicationFactor)
        )
      }
    }

    object DatabaseUser {

      implicit val reads: Reads[DatabaseUser] = new Reads[DatabaseUser] {
        val impl = { json: JsValue =>
          new DatabaseUser(
            (json \ "name").as[String],
            (json \ "password").as[String])
        }

        def reads(json: JsValue) = {
          try {
            JsSuccess(impl(json))
          } catch {
            case e: Exception => JsError(e.getMessage)
          }
        }
      }

      implicit val writes: Writes[DatabaseUser] = new Writes[DatabaseUser] {
        def writes(value: DatabaseUser) = Json.obj(
          "name" -> JsString(value.name),
          "password" -> JsString(value.password)
        )
      }
    }

    object GetMetricDataResponse {

      implicit val reads: Reads[GetMetricDataResponse] = new Reads[GetMetricDataResponse] {
        val impl = { json: JsValue =>
          json match {
            case JsArray(sequence) if sequence.length > 0 =>
              val struct = sequence(0)

              new GetMetricDataResponse(
                name = (struct \ "name").as[String],

                points = ({ json: JsValue =>
                  json match {
                    case _@(JsNull | _: JsUndefined) =>
                      Map.empty[DateTime, Double]

                    case JsArray(values) =>
                      values.toList.map {
                        case JsArray(pairs) =>
                          val n = pairs.length
                          new DateTime(1000 * pairs(0).as[Long]) -> pairs(n-1).as[Double]

                        case _ => sys.error("Unexpected data encountered inside the 'points' array.")
                      }.toMap

                    case x => sys.error(s"Cannot parse list from ${x.getClass}")
                  }
                })(struct \ "points")
              )
            case _ => new GetMetricDataResponse("", Map.empty[DateTime, Double])
          }
        }

        def reads(json: JsValue) = {
          try {
            JsSuccess(impl(json))
          } catch {
            case e: Exception => JsError(e.getMessage)
          }
        }
      }
    }

  }

}
