import com.cave.metrics.data.Metric
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Mockito}
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test._

import scala.util.{Failure, Success}

class CreateTeamMetricsApiSpec  extends PlaySpecification with Results with AbstractMetricsApiSpec {

  "POST /organizations/:name/team/:team/metrics" should {

    "respond with 201 and no content" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockDataSink)
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)))),
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

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamBasicAuth)))),
          Json.parse(ValidMetricOrdersUSBulk)))


      status(result) must equalTo(ACCEPTED)

      val argCaptor = ArgumentCaptor.forClass(classOf[Seq[Metric]])
      verify(mockDataSink).sendMetrics(argCaptor.capture())
      verifyMetricOrdersUS(argCaptor.getValue, s"$GiltTeamName.$GiltName")

      contentAsString(result) must equalTo("")
    }

    "work with an organization token as well" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockDataSink)
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltOrgAuth)))),
          Json.parse(ValidMetricOrdersUSBulk)))


      status(result) must equalTo(ACCEPTED)

      val argCaptor = ArgumentCaptor.forClass(classOf[Seq[Metric]])
      verify(mockDataSink).sendMetrics(argCaptor.capture())
      verifyMetricOrdersUS(argCaptor.getValue, s"$GiltTeamName.$GiltName")

      contentAsString(result) must equalTo("")
    }

    "work with an organization token and Basic authentication too" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockDataSink)
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltOrgBasicAuth)))),
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

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(),
          Json.parse(ValidMetricOrdersUSBulk)))


      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported authentication specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamBadAuth)))),
          Json.parse(ValidMetricOrdersUSBulk)))


      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if wrong authentication specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(TestTeamAuth)))),
          Json.parse(ValidMetricOrdersUSBulk)))


      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if organization not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if team not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)))),
          Json.parse(ValidMetricOrdersUSBulk)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 400 if incomplete JSON" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))

      val result = new TestController().createTeamMetrics(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/metrics",
          FakeHeaders(Seq((AUTHORIZATION, Seq(GiltTeamAuth)))),
          Json.parse(InvalidMetrics)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(ErrorMissingValue)
    }
  }
}
