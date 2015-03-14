import com.cave.metrics.data.{MetricInfo, Role}
import data.{GetMetricData, UserData}
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{FakeRequest, FakeApplication, PlaySpecification}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

class ListTeamMetricsShortcutApiSpec extends PlaySpecification with Results with AbstractMetricsApiSpec with UserData with GetMetricData {

  "GET /metrics-list {HOST: $team.$org.$baseUrl}" should {

    "retrieve some metrics for an existing organization" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(GiltOrg), Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.listMetrics(s"$GiltTeamName.$GiltName")(mockContext)).thenReturn(Future.successful(SomeMetrics))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeMetricsJson)
    }

    "also works for org admins" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(GiltOrg), Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List()))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.listMetrics(s"$GiltTeamName.$GiltName")(mockContext)).thenReturn(Future.successful(SomeMetrics))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SomeMetricsJson)
    }

    "return an empty Json if no metrics exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(GiltOrg), Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.listMetrics(s"$GiltTeamName.$GiltName")(mockContext)).thenReturn(Future.successful(List.empty[MetricInfo]))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo("[]")
    }

    "return 400 if host tag is missing" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Hostname must be provided.")
    }

    "return 404 if organization does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "return 404 if team does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }


    "return 401 if no auth token specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 401 if unsupported auth token specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> GiltOrgBadAuth, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not in the team and not an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(GiltOrg), Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List()))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 500 if there's an error" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(GiltOrg), Matchers.eq(SOME_USER))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.listMetrics(s"$GiltTeamName.$GiltName")(mockContext)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().listMetrics().apply(
        FakeRequest(GET, s"/metrics-list")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"$GiltTeamName.$GiltName.$BaseUrl"))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}