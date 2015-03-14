import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import com.cave.metrics.data.Metric
import org.mockito.{Mockito, ArgumentCaptor}
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test._

class CreateTeamMetricsShortcutApiSpec  extends PlaySpecification with Results with AbstractMetricsApiSpec {

  "POST /metrics {HOST: $team.$org.$baseUrl}" should {

    "respond with 201 and no content" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockDataSink)
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(ACCEPTED)

      val argCaptor = ArgumentCaptor.forClass(classOf[Seq[Metric]])
      verify(mockDataSink).sendMetrics(argCaptor.capture())
      verifyMetricOrdersUS(argCaptor.getValue, s"$GiltTeamName.$GiltName")

      contentAsString(result) must equalTo("")
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockDataSink)
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamBasicAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(ACCEPTED)

      val argCaptor = ArgumentCaptor.forClass(classOf[Seq[Metric]])
      verify(mockDataSink).sendMetrics(argCaptor.capture())
      verifyMetricOrdersUS(argCaptor.getValue, s"$GiltTeamName.$GiltName")

      contentAsString(result) must equalTo("")
    }

    "respond with 201 and no content when using an organization token" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockDataSink)
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltOrgAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(ACCEPTED)

      val argCaptor = ArgumentCaptor.forClass(classOf[Seq[Metric]])
      verify(mockDataSink).sendMetrics(argCaptor.capture())
      verifyMetricOrdersUS(argCaptor.getValue, s"$GiltTeamName.$GiltName")

      contentAsString(result) must equalTo("")
    }

    "also work with an organization token and Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockDataSink)
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltOrgBasicAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(ACCEPTED)

      val argCaptor = ArgumentCaptor.forClass(classOf[Seq[Metric]])
      verify(mockDataSink).sendMetrics(argCaptor.capture())
      verifyMetricOrdersUS(argCaptor.getValue, s"$GiltTeamName.$GiltName")

      contentAsString(result) must equalTo("")
    }

    "respond with 401 if no authentication specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported authentication specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamBadAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUS)))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }


    "respond with 403 if wrong authentication specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(TestTeamAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if organization not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if team not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 400 if incomplete JSON" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createMetrics()(
        FakeRequest(POST, s"/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)), (HOST, Seq(s"$GiltTeamName.$GiltName.$BaseUrl")))),
          Json.parse(InvalidMetrics)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot parse metrics: List((/metrics,List(ValidationError(error.path.missing,WrappedArray()))))")
    }
  }
}
