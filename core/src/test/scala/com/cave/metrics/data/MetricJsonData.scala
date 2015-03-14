package com.cave.metrics.data

trait MetricJsonData {

  final val ValidMetricOrdersUS =
    """{
      |  "name": "orders",
      |  "timestamp": 1395915330,
      |  "value": 10,
      |  "tags": { "shipCountry": "US" }
      |}
    """.stripMargin

  final val ValidMetricOrdersUSBulk =
    """{ "metrics": [{
      |  "name": "orders",
      |  "timestamp": 1395915330,
      |  "value": 10,
      |  "tags": { "shipCountry": "US" }}
      |  ]
      |}
    """.stripMargin

  final val ValidMetricOrdersCA =
    """{
      |  "name": "orders",
      |  "timestamp": 1395915330,
      |  "value": 2,
      |  "tags": { "shipCountry": "CA" }
      |}
    """.stripMargin

  final val BadMetricJson = """{ no json here }"""
  final val IncompleteJson = """{ "name": "orders" }"""

  final val OrdersUSMissingValue =
    """{
      |  "name": "orders without value",
      |  "timestamp": 1395915330,
      |  "tags": { "shipCountry": "US" }
      |}
      | """.stripMargin

  final val OrdersUSMissingTimestamp =
    """{
      |  "name": "orders without value",
      |  "value": 10,
      |  "tags": { "shipCountry": "US" }
      |}
      | """.stripMargin

  final val OrdersCAMissingValue =
    """{
      |  "name": "orders without value",
      |  "timestamp": 1395915330,
      |  "tags": { "shipCountry": "CA" }
      |}
      | """.stripMargin

  final val OrdersCAMissingTimestamp =
    """{
      |  "name": "orders without value",
      |  "value": 10,
      |  "tags": { "shipCountry": "CA" }
      |}
      | """.stripMargin

  final val ErrorMissingValue     = "Cannot parse metrics: List((/metrics,List(ValidationError(error.path.missing,WrappedArray()))))"
  final val ErrorMissingTimestamp = "JsResultException(errors:List((/timestamp,List(ValidationError(error.path.missing,WrappedArray())))))"

  final val ValidMetrics = s"""{"metrics": [$ValidMetricOrdersUS,$ValidMetricOrdersCA]}"""
  final val InvalidMetrics = s"[$OrdersUSMissingValue,$OrdersCAMissingTimestamp]"
  final val ValidAndInvalidMetrics = s"[$ValidMetricOrdersUS,$OrdersUSMissingValue,$OrdersCAMissingTimestamp,$ValidMetricOrdersCA]"
}
