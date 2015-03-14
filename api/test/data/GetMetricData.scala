package data

import com.cave.metrics.data.{MetricInfo, MetricData, MetricDataBulk}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json

trait GetMetricData {

  final val MetricNameOrders = "orders"
  final val MetricTagsOrders = Map("shipTo" -> "US")
  final val MetricTagsOrdersString = "shipTo:US"

  final val MetricTagsService = Map("env" -> "production", "svc" -> "svc-important", "host" -> "svc18")
  final val MetricTagsServiceString = "env:production,svc:svc-important,host:svc18"

  final val DT = ISODateTimeFormat.dateTime()
  final val DateTime0 = DT.parseDateTime("2014-10-17T10:00:00.000Z")
  final val DateTime1 = DT.parseDateTime("2014-10-17T10:01:00.000Z")
  final val DateTime2 = DT.parseDateTime("2014-10-17T10:02:00.000Z")
  final val DateTime3 = DT.parseDateTime("2014-10-17T10:03:00.000Z")
  final val DateTime4 = DT.parseDateTime("2014-10-17T10:04:00.000Z")
  final val DateTime5 = DT.parseDateTime("2014-10-17T10:05:00.000Z")
  final val DateTime6 = DT.parseDateTime("2014-10-17T10:06:00.000Z")
  final val DateTime7 = DT.parseDateTime("2014-10-17T10:07:00.000Z")

  final val SomeDateString = "2014-10-17T10:00:00.000Z"
  final val SomeDate = DT.parseDateTime(SomeDateString)

  final val SomeDataOrders = MetricDataBulk(Seq(
    MetricData(DateTime0, 2),
    MetricData(DateTime1, 3),
    MetricData(DateTime2, 4),
    MetricData(DateTime3, 10),
    MetricData(DateTime4, 7),
    MetricData(DateTime5, 4),
    MetricData(DateTime6, 3),
    MetricData(DateTime7, 1)
  ))

  final val SomeDataOrdersJson = Json.toJson(SomeDataOrders)

  final val LargeLimit: Int = 9999
  final val SmallLimit: Int = 3

  final val SomeDataOrdersLimited = MetricDataBulk(SomeDataOrders.metrics.take(SmallLimit))
  final val SomeDataOrdersLimitedJson = Json.toJson(SomeDataOrdersLimited)

  final val SomeMetrics = Seq(
    MetricInfo("svc-sku-pricing-publisher", List("host", "service", "environment")),
    MetricInfo("alertsHistory", List("alert"))
  )
  final val SomeMetricsJson = Json.toJson(SomeMetrics)
}
