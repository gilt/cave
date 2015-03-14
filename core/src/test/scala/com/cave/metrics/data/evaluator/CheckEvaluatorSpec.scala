package com.cave.metrics.data.evaluator

import com.cave.metrics.data._
import com.cave.metrics.data.influxdb.{InfluxClientFactory, InfluxClient}
import org.joda.time.DateTime
import org.mockito.Matchers.{any => mockAny, eq => mockEq}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class CheckEvaluatorSpec extends FlatSpec with MockitoSugar with Matchers with BeforeAndAfter with AlertJsonData with ScalaFutures {

  "An evaluator" should "evaluate condition with two value sources" in {

    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod, "3 > 2", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)
    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    val result = evaluator.evaluate(fetcher)

    Mockito.verifyZeroInteractions(mockClient)
    whenReady(result) { value =>
      value should be (Success(true))
    }
  }

  it should "evaluate condition with two value sources and greaterThanOrEqual()" in {

    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod, "3 >= 3", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)
    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    val result = evaluator.evaluate(fetcher)

    Mockito.verifyZeroInteractions(mockClient)
    whenReady(result) { value =>
      value should be (Success(true))
    }
  }


  it should "evaluate condition with two value sources and equal()" in {

    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod, "3 == 2", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)
    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    val result = evaluator.evaluate(fetcher)

    Mockito.verifyZeroInteractions(mockClient)
    whenReady(result) { value =>
      value should be (Success(false))
    }
  }

  it should "evaluate condition with two value sources and notEqual()" in {

    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod, "3 != 2", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)
    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    val result = evaluator.evaluate(fetcher)

    Mockito.verifyZeroInteractions(mockClient)
    whenReady(result) { value =>
      value should be (Success(true))
    }
  }

  it should "evaluate condition with a metric source and a value" in {

    val evaluator = new CheckEvaluator(InsufficientOrders)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockAny[String], mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(MetricData(Timestamp, 1.1)))))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be (Success(true))
    }

  }

  it should "return false if not enough data points available for metric source" in {
    val evaluator = new CheckEvaluator(InsufficientOrdersFive)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockAny[String], mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(MetricData(Timestamp, 11.0)))))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be (Success(false))
    }

  }

  it should "evaluate condition with metric source and multiple data points" in {
    val evaluator = new CheckEvaluator(InsufficientOrdersFive)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockAny[String], mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(
        MetricData(Timestamp0, 2.0),
        MetricData(Timestamp1, 9.9),
        MetricData(Timestamp2, 9.0),
        MetricData(Timestamp3, 6.0),
        MetricData(Timestamp4, 10.0)
      ))))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be (Success(true))
    }
  }

  it should "evaluate condition with two metric sources and multiple data points" in {
    val evaluator = new CheckEvaluator(OrdersLessThanPredicted)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(AlternativeClusterName)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockEq("orders"), mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(
      MetricData(Timestamp0, 2.0),
      MetricData(Timestamp1, 9.9),
      MetricData(Timestamp2, 9.0),
      MetricData(Timestamp3, 6.0),
      MetricData(Timestamp4, 10.0)
    ))))))

    when(mockClient.getMetricData(mockAny[String], mockEq("ordersLO"), mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(
      MetricData(Timestamp0,  3.0),
      MetricData(Timestamp1, 10.0),
      MetricData(Timestamp2, 11.0),
      MetricData(Timestamp3, 25.0),
      MetricData(Timestamp4, 30.0)
    ))))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be (Success(true))
    }
  }

  it should "return false if a metric sources has insufficient data points" in {
    val evaluator = new CheckEvaluator(OrdersLessThanPredicted)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(AlternativeClusterName)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockEq("orders"), mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(
      MetricData(Timestamp0, 2.0),
      MetricData(Timestamp1, 9.9),
      MetricData(Timestamp2, 9.0),
      MetricData(Timestamp3, 6.0),
      MetricData(Timestamp4, 10.0)
    ))))))

    when(mockClient.getMetricData(mockAny[String], mockEq("ordersLO"), mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(
      MetricData(Timestamp0, 0.0),
      MetricData(Timestamp1, 0.0)
    ))))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be (Success(false))
    }
  }

  it should "evaluate conditions with aggregate metrics" in {
    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod,
      "responseTime[svc: important, env: production].p99.5m > 5000 at least 5 times", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)

    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockAny[String], mockEq(Map.empty[String, String]),
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(
      MetricData(Timestamp0, 2.0),
      MetricData(Timestamp1, 9.9),
      MetricData(Timestamp2, 9.0),
      MetricData(Timestamp3, 6.0),
      MetricData(Timestamp4, 10.0)
    ))))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be (Success(false))
    }
  }

  it should "evaluate missing data condition to false when data exists" in {
    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod,
      "heartbeat[svc: important] missing for 15m", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)

    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockEq("heartbeat"), mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq(
      MetricData(Timestamp0, 0.0),
      MetricData(Timestamp1, 0.0)
    ))))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be(Success(false))
    }
  }

  it should "evaluate missing data condition to true when no data exists" in {
    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod,
      "heartbeat[svc: important] missing for 15m", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)

    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockEq("heartbeat"), mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq.empty[MetricData])))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be(Success(true))
    }
  }

  it should "fail to evaluate missing data condition when the series does not exist" in {
    val check = Check(Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, AlertPeriod,
      "heartbeat[svc: important] missing for 15m", Some(AlertHandbookUrl), Some(AlertRouting))), Timestamp)

    val evaluator = new CheckEvaluator(check)
    val mockClient = mock[InfluxClient]
    val mockContext = mock[ExecutionContext]
    val mockClientFactory = mock[InfluxClientFactory]
    val fetcher = new DataFetcher(mockClientFactory)

    when(mockClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
    when(mockClient.getMetricData(mockAny[String], mockEq("heartbeat"), mockAny[Map[String, String]],
      mockAny[Option[DateTime]], mockAny[Option[DateTime]], mockAny[Option[Int]])(mockAny[ExecutionContext])).
      thenReturn(Future.successful(Success(Some(MetricDataBulk(Seq.empty[MetricData])))))

    val result = evaluator.evaluate(fetcher)

    whenReady(result) { value =>
      value should be(Success(true))
    }
  }


}
