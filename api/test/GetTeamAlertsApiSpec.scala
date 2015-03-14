import com.cave.metrics.data.Role
import controllers.Alerts
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

class GetTeamAlertsApiSpec extends PlaySpecification with Results with AbstractAlertApiSpec with UserData {

  val AuthRequest = FakeRequest().withHeaders(AUTHORIZATION -> AUTH_TOKEN)

  "GET /organizations/:name/teams/:team/alerts" should {

    "retrieve the list of alerts for an existing team" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      val limit = 20
      val offset = 0
      when(mockAlertManager.getTeamAlerts(GiltOrg, GiltTeam, limit + 1, offset)).thenReturn(Success(List(SomeAlert)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert))
    }

    "also works if user is an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      val limit = 20
      val offset = 0
      when(mockAlertManager.getTeamAlerts(GiltOrg, GiltTeam, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert))
    }

    "retrieve the list of alerts starting from offset up to a given limit" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      val limit = 2
      val offset = 2
      when(mockAlertManager.getTeamAlerts(GiltOrg, GiltTeam, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert, SomeAlert2)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert, SomeAlert2))
    }

    "return a paging token for the next page if there are more results" in running(FakeApplication(withGlobal = mockGlobal)) {
      val GiltTeamNextAlertLocation = s"""<$BaseUrl/organizations/$GiltName/teams/$GiltTeamName/alerts?limit=2&offset=2>; rel="next""""

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      val limit = 2
      val offset = 0
      when(mockAlertManager.getTeamAlerts(GiltOrg, GiltTeam, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert, SomeAlert2, SomeAlert3)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      headers(result).get(Alerts.LINK) must beSome(GiltTeamNextAlertLocation)
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert, SomeAlert2))
    }

    "return a paging token for the previous page if there are more results" in running(FakeApplication(withGlobal = mockGlobal)) {
      val GiltTeamPrevAlertLocation = s"""<$BaseUrl/organizations/$GiltName/teams/$GiltTeamName/alerts?limit=2&offset=0>; rel="prev""""

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      val limit = 2
      val offset = 2
      when(mockAlertManager.getTeamAlerts(GiltOrg, GiltTeam, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      headers(result).get(Alerts.LINK) must beSome(GiltTeamPrevAlertLocation)
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert))
    }

    "return a paging token for the next page if there are more results" in running(FakeApplication(withGlobal = mockGlobal)) {
      val GiltTeamNextAlertLocation = s"""<$BaseUrl/organizations/$GiltName/teams/$GiltTeamName/alerts?limit=2&offset=4>; rel="next""""
      val GiltTeamPrevAlertLocation = s"""<$BaseUrl/organizations/$GiltName/teams/$GiltTeamName/alerts?limit=2&offset=0>; rel="prev""""

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
      (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      val limit = 2
      val offset = 2
      when(mockAlertManager.getTeamAlerts(GiltOrg, GiltTeam, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert, SomeAlert2, SomeAlert3)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      headers(result).get(Alerts.LINK) must beSome(s"$GiltTeamNextAlertLocation, $GiltTeamPrevAlertLocation")
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert, SomeAlert2))
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName)(
        FakeRequest(GET, s"/organizations/$GiltName/teams/$GiltTeamName/alerts"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName)(
        FakeRequest(GET, s"/organizations/$GiltName/teams/$GiltTeamName/alerts")
          .withHeaders(AUTHORIZATION -> GiltTeamBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName)(AuthRequest)

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user is neither a team member/admin nor an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName)(AuthRequest)

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if organization not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName)(AuthRequest)

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if team not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName)(AuthRequest)

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockAlertManager.getTeamAlerts(Matchers.eq(GiltOrg), Matchers.eq(GiltTeam), Matchers.anyInt(), Matchers.anyInt()))
        .thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().getTeamAlerts(GiltName, GiltTeamName)(AuthRequest)

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
