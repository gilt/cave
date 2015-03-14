package com.cave.metrics.data

import com.cave.metrics.data.influxdb.{InfluxClientFactory, InfluxClient}
import com.cave.metrics.data.postgresql.{AbstractDataManagerSpec, PostgresCacheDataManagerImpl}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.slick.driver.H2Driver.simple._
import scala.util.{Failure, Success}

class AlertManagerSpec extends AbstractDataManagerSpec with MockitoSugar {

  val mockInfluxClientFactory = mock[InfluxClientFactory]
  val mockInfluxClient = mock[InfluxClient]
  val mockExecutionContext = mock[ExecutionContext]
  val dm = new PostgresCacheDataManagerImpl(awsConfig)
  val am = new AlertManager(dm, mockInfluxClientFactory)

  "Alert Manager" should "create Queries based on Alarms" in {
    when(mockInfluxClientFactory.getClient(None)).thenReturn(mockInfluxClient -> mockExecutionContext)
    when(mockInfluxClient.createContinuousQuery(any[String], any[Map[String, String]], any[String], any[FiniteDuration], any[String], any[String])(any[ExecutionContext])).thenReturn(Future(true))

    val org = dm.getOrganization(GiltOrgName).get.get
    val team = dm.getTeam(org, testTeamName).get.get

    am.createOrganizationAlert(org, Alert(None, "alert description", true, "5m", "orders [shipCountry: US].sum.12m <= 5", None, None)) match {
      case Success(Some(alert)) => alert.id.isDefined should be(true)
      case Failure(f) => fail("unable to create an alert" + f.getMessage)
      case _ => fail("unable to create an alert")
    }
    alert2queriesTable.list.size should be(1)
    queriesTable.list.size should be(1)

    am.createOrganizationAlert(org, Alert(None, "alert description", true, "10m", "125 <= orders [shipCountry: US].sum.12m", None, None)) match {
      case Success(Some(alert)) => alert.id.isDefined should be(true)
      case _ => fail("unable to create an alert")
    }
    alert2queriesTable.list.size should be(2)
    queriesTable.list.size should be(1)

    am.createOrganizationAlert(org, Alert(None, "alert description", true, "10m", "orders [shipCountry: EU].sum.12h <= 5", None, None)) match {
      case Success(Some(alert)) => alert.id.isDefined should be(true)
      case _ => fail("unable to create an alert")
    }
    alert2queriesTable.list.size should be(3)
    queriesTable.list.size should be(2)

    am.createOrganizationAlert(org, Alert(None, "alert description", true, "10m", "orders [shipCountry: EU].sum.12h <= 555", None, None)) match {
      case Success(Some(alert)) => alert.id.isDefined should be(true)
      case _ => fail("unable to create an alert")
    }
    alert2queriesTable.list.size should be(4)
    queriesTable.list.size should be(2)

    am.createOrganizationAlert(org, Alert(None, "alert description", true, "10m", "orders [shipCountry: XXX].sum.12h <= orders [shipCountry: WWW].sum.12h", None, None)) match {
      case Success(Some(alert)) => alert.id.isDefined should be(true)
      case _ => fail("unable to create an alert")
    }
    alert2queriesTable.list.size should be(6)
    queriesTable.list.size should be(4)


    am.createTeamAlert(org, team, Alert(None, "alert description", true, "10m", "orders [shipCountry: XXX].sum.12h <= orders [shipCountry: WWW].sum.12h", None, None)) match {
      case Success(Some(alert)) => alert.id.isDefined should be(true)
      case _ => fail("unable to create an alert")
    }
    alert2queriesTable.list.size should be(8)
    queriesTable.list.size should be(4)


    am.createTeamAlert(org, team, Alert(None, "alert description", true, "10m", "orders [][ [shipCountry: XXX].sum.12h <= orders [shipCountry: WWW].sum.12h", None, None)) match {
      case Success(None) => // failed to parse
      case _ => fail("unable to create an alert")
    }

    alert2queriesTable.list.size should be(8)
    queriesTable.list.size should be(4)

    am.createTeamAlert(org, team, Alert(None, "alert description", true, "1m", "heartbeat[svc: magic] missing for 15m", None, None)) match {
      case Success(Some(alert)) => alert.id.isDefined should be(true)
      case _ => fail("expected alert creation to succeed")
    }
    queriesTable.list.foreach(q => println(s"${q.name}  ${q.id}"))

  }

  it should "fail to parse an alert with bad period" in {
    val org = dm.getOrganization(GiltOrgName).get.get

    am.createOrganizationAlert(org, Alert(None, "alert description", true, "5y", "orders [shipCountry: US].sum.12m <= 5", None, None)) match {
      case Success(_) => fail("Expected to fail")
      case Failure(e) => e.getMessage should be("Cannot parse alert period: 5y")
    }
  }

  it should "add metrics to the alerts when fetching" in {
    val org = dm.getOrganization(GiltOrgName).get.get
    val team = dm.getTeam(org, testTeamName).get.get

    val controlData = createSampleAlerts(org, team)

    am.getOrganizationAlerts(org) match {
      case Success(alerts) =>
        validateAlerts(alerts, controlData)
      case _ => fail("expected alert retrieval to succeed")
    }

    am.getTeamAlerts(org, team) match {
      case Success(alerts) =>
        validateAlerts(alerts, controlData)
      case _ => fail("expected alert retrieval to succeed")
    }

    controlData.foreach {
      case (id, controlMetrics) =>
        am.getAlert(id) match {
          case Success(alert) =>
            alert.isDefined should be(true)
            alert.foreach {
              _.relatedMetrics should be(Some(controlMetrics))
            }
          case _ => fail("expected alert retrieval to succeed")
        }
    }
  }

  // Creates a metric for each type of Source. Returns a map of alert id to associated metrics
  private def createSampleAlerts(org: Organization, team: Team): Map[String, Set[AlertMetric]] = {
    val singleSource = am.createTeamAlert(org, team, Alert(None, "alert description", true, "5m", "metric1 [shipCountry: US].sum.12m <= 5", None, None)) match {
      case Success(Some(alert)) => alert.id.get
      case _ => fail("expected alert creation to succeed")
    }

    val doubleSource = am.createTeamAlert(org, team, Alert(None, "alert description", true, "5m", "metric1 [shipCountry: US, shipState: NY].sum.5m <= metric2 [shipCountry: EU].sum.12m", None, None)) match {
      case Success(Some(alert)) => alert.id.get
      case _ => fail("expected alert creation to succeed")
    }

    val missingDataSource = am.createTeamAlert(org, team, Alert(None, "alert description", true, "1m", "missing_data_metric[svc: magic] missing for 10m", None, None)) match {
      case Success(Some(alert)) => alert.id.get
      case _ => fail("expected alert creation to succeed")
    }

    Map(
      singleSource -> Set(AlertMetric("metric1", Map("shipCountry" -> "US"), Some("sum"), Some(720l))),
      doubleSource -> Set(AlertMetric("metric1", Map("shipCountry" -> "US", "shipState" -> "NY"), Some("sum"), Some(300l)), AlertMetric("metric2", Map("shipCountry" -> "EU"), Some("sum"), Some(720l))),
      missingDataSource -> Set(AlertMetric("missing_data_metric", Map("svc" -> "magic"), None, None))
    )
  }

  private def validateAlerts(alerts: Seq[Alert], controlData: Map[String, Set[AlertMetric]]) {
    def inControl(alert: Alert) = alert.id.exists(controlData.contains)

    val alertsToTest = alerts.filter { inControl }
    alertsToTest.foreach { alert =>
      alert.relatedMetrics should be(controlData.get(alert.id.get))
    }
  }
}
