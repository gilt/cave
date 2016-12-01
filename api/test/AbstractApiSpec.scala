import com.cave.metrics.data._
import com.cave.metrics.data.influxdb.{InfluxClient, InfluxClientFactory}
import controllers._
import data.TestJsonData
import init.{AwsWrapper, MailService}
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.joda.time.DateTime
import org.scalatest.mockito.MockitoSugar
import org.specs2.mutable.Specification
import play.api.GlobalSettings
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

trait AbstractApiSpec extends MockitoSugar with TestJsonData {

  class Global extends GlobalSettings

  final val ErrorMessage = "Boom!"
  final val InternalErrorMessage = "Oops! We've experienced an internal error. Please try your request again later."

  val mockDataManager = mock[DataManager]
  val mockDataSink = mock[DataSink]
  val mockAwsWrapper = mock[AwsWrapper]
  val mockInfluxClientFactory = mock[InfluxClientFactory]
  val mockClient = mock[InfluxClient]
  val mockContext = mock[ExecutionContext]
  val mockGlobal = Some(new Global)
  val mockAlertManager = mock[AlertManager]
  val mockMailService = mock[MailService]
  val mockPasswordHelper = mock[PasswordHelper]
}

trait AbstractInternalApiSpec extends AbstractApiSpec {
  class TestController extends Controller with Internal {
    override lazy val dataManager = mockDataManager
    override lazy val awsWrapper = mockAwsWrapper
    override lazy val baseUrl = BaseUrl
    override lazy val influxClientFactory = mockInfluxClientFactory
    override lazy val dataSink = mockDataSink
    override lazy val mailService = mockMailService
    override lazy val passwordHelper = mockPasswordHelper
    override lazy val maxTokens = 3
  }
}

trait AbstractOrganizationApiSpec extends AbstractApiSpec {
  class TestController extends Controller with Organizations {
    override lazy val dataManager = mockDataManager
    override lazy val awsWrapper = mockAwsWrapper
    override lazy val baseUrl = BaseUrl
    override lazy val influxClientFactory = mockInfluxClientFactory
    override lazy val dataSink = mockDataSink
    override lazy val maxTokens = 3
    override lazy val alertManager = mockAlertManager
    override lazy val mailService = mockMailService
    override lazy val passwordHelper = mockPasswordHelper
  }
}

trait AbstractTeamApiSpec extends AbstractApiSpec {
  class TestController extends Controller with Teams {
    override lazy val dataManager = mockDataManager
    override lazy val awsWrapper = mockAwsWrapper
    override lazy val baseUrl = BaseUrl
    override lazy val influxClientFactory = mockInfluxClientFactory
    override lazy val dataSink = mockDataSink
    override lazy val maxTokens = 3
    override lazy val alertManager = mockAlertManager
    override lazy val mailService = mockMailService
    override lazy val passwordHelper = mockPasswordHelper
  }
}

trait AbstractTokenApiSpec extends AbstractApiSpec {
  class TestController extends Controller with Tokens {
    override lazy val dataManager = mockDataManager
    override lazy val awsWrapper = mockAwsWrapper
    override lazy val baseUrl = BaseUrl
    override lazy val influxClientFactory = mockInfluxClientFactory
    override lazy val dataSink = mockDataSink
    override lazy val maxTokens = 3
    override lazy val mailService = mockMailService
    override lazy val passwordHelper = mockPasswordHelper
  }
}

trait AbstractAlertApiSpec extends AbstractApiSpec {
  class TestController extends Controller with Alerts {
    override lazy val dataManager = mockDataManager
    override lazy val awsWrapper = mockAwsWrapper
    override lazy val baseUrl = BaseUrl
    override lazy val influxClientFactory = mockInfluxClientFactory
    override lazy val dataSink = mockDataSink
    override lazy val maxTokens = 3
    override lazy val alertManager = mockAlertManager
    override lazy val mailService = mockMailService
    override lazy val passwordHelper = mockPasswordHelper
  }
}

trait AbstractUsersApiSpec extends AbstractApiSpec {
  class TestController(current: DateTime) extends Controller with Users {
    override lazy val dataManager = mockDataManager
    override lazy val awsWrapper = mockAwsWrapper
    override lazy val baseUrl = BaseUrl
    override lazy val influxClientFactory = mockInfluxClientFactory
    override lazy val dataSink = mockDataSink
    override lazy val maxTokens = 3
    override lazy val alertManager = mockAlertManager
    override lazy val mailService = mockMailService
    override lazy val passwordHelper = mockPasswordHelper

    override def now(): DateTime = current
  }
}

trait AbstractMetricsApiSpec extends Specification with AbstractApiSpec with MetricJsonData {

  class TestController extends Controller with Metrics {
    override lazy val dataManager = mockDataManager
    override lazy val awsWrapper = mockAwsWrapper
    override lazy val baseUrl = BaseUrl
    override lazy val influxClientFactory = mockInfluxClientFactory
    override lazy val dataSink = mockDataSink
    override lazy val mailService = mockMailService
    override lazy val passwordHelper = mockPasswordHelper
    override lazy val maxTokens = 3
    override implicit val metricsEnabled = false
  }

  protected def verifyMetricOrdersUS(metrics: Seq[Metric], orgName: String) {
    metrics.size must equalTo(1)
    val usOrders = metrics.head

    usOrders.name must equalTo("orders")
    usOrders.timestamp must equalTo(1395915330)
    usOrders.value must equalTo(10)
    usOrders.tags.size must equalTo(2)
    usOrders.tags.get("shipCountry") must beSome("US")
    usOrders.tags.get(Metric.Organization) must beSome(orgName)
  }

  protected def hostMap(orgName: String) =
    Seq((HttpHeaders.Names.HOST, Seq(s"$orgName.example.com")))

  protected def hostMap(orgName: String, teamName: String) =
    Map(HttpHeaders.Names.HOST -> s"$teamName.$orgName.example.com")
}