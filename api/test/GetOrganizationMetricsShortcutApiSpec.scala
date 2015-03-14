import com.cave.metrics.data.Role
import data.{GetMetricData, UserData}
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{PlaySpecification, FakeRequest, FakeApplication}

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class GetOrganizationMetricsShortcutApiSpec extends PlaySpecification with Results with AbstractMetricsApiSpec with UserData with GetMetricData {

  "GET /metrics {HOST: $org.$baseUrl}" should {

    "retrieve some metrics for an existing organization" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, Map.empty[String, String], None, None, None)(mockContext))
        .thenReturn(Future.successful(Success(Some(SomeDataOrders))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeDataOrdersJson)
    }

    "return 200 after handling a single tag" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, MetricTagsOrders, None, None, None)(mockContext))
        .thenReturn(Future.successful(Success(Some(SomeDataOrders))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&tags=$MetricTagsOrdersString&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeDataOrdersJson)
    }

    "return 200 after handling multiple tags" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, MetricTagsService, None, None, None)(mockContext))
        .thenReturn(Future.successful(Success(Some(SomeDataOrders))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&tags=$MetricTagsServiceString&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeDataOrdersJson)
    }

    "return 200 after handling a start date" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, Map.empty[String, String], Some(SomeDate), None, None)(mockContext))
        .thenReturn(Future.successful(Success(Some(SomeDataOrders))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&start=$SomeDateString&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeDataOrdersJson)
    }

    "return 200 after handling an end date" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, Map.empty[String, String], None, Some(SomeDate), None)(mockContext))
        .thenReturn(Future.successful(Success(Some(SomeDataOrders))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&end=$SomeDateString&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeDataOrdersJson)
    }

    "return 200 after handling a large limit" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, Map.empty[String, String], None, None, Some(LargeLimit))(mockContext))
        .thenReturn(Future.successful(Success(Some(SomeDataOrders))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&limit=$LargeLimit&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeDataOrdersJson)
    }

    "return 200 after handling a small limit" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, Map.empty[String, String], None, None, Some(SmallLimit))(mockContext))
        .thenReturn(Future.successful(Success(Some(SomeDataOrdersLimited))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&limit=$SmallLimit&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeDataOrdersLimitedJson)
    }

    "return 404 if organization doesn't exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "return 400 if metric not specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?start=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Required parameter 'metric' is missing.")
    }

    "return 400 if aggregator not specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Required parameter 'aggregator' is missing.")
    }

    "return 400 if aggregator not valid" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=collate&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Parameter 'aggregator' has invalid value 'collate'.")
    }

    "return 400 if period not specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Required parameter 'period' is missing.")
    }

    "return 400 if period not valid" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=boom")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Parameter 'period' has invalid value 'boom'.")
    }

    "return 400 if host tag not specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1"))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Hostname must be provided.")
    }

    "return 401 if no token specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 401 if unsupported token specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> GiltOrgBadAuth, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not connected to the organization" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 500 if there is an error" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.getAggregatedData(GiltName, "sum(value)", 1.minutes, MetricNameOrders, Map.empty[String, String], None, None, None)(mockContext))
        .thenReturn(Future.successful(Failure(new RuntimeException(ErrorMessage))))

      val result = new TestController().getMetrics().apply(
        FakeRequest(GET, s"/metrics?metric=$MetricNameOrders&aggregator=sum&period=1")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltName.$BaseUrl"))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
