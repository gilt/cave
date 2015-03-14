import com.cave.metrics.data._
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CreateTeamAlertApiSpec extends PlaySpecification with Results with AbstractAlertApiSpec with UserData {

  "POST /organizations/:name/teams/:team/alerts" should {

    "respond with 201 and the new alert" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockAlertManager.createTeamAlert(any[Organization], any[Team], any[Alert])).thenReturn(Success(Some(SomeAlert)))
      when(mockAwsWrapper.createAlertNotification(any[Schedule])).thenReturn(Future.successful())

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltTeamAlertLocation)
      contentAsString(result) must equalTo(Json.stringify(SomeOldAlertJson))
    }

    "also works if user is an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockAlertManager.createTeamAlert(any[Organization], any[Team], any[Alert])).thenReturn(Success(Some(SomeAlert)))
      when(mockAwsWrapper.createAlertNotification(any[Schedule])).thenReturn(Future.successful())

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltTeamAlertLocation)
      contentAsString(result) must equalTo(Json.stringify(SomeOldAlertJson))
    }

    "respond with 400 if alert already has an ID" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeOldAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("To update an existing alert, use the PATCH verb.")
    }

    "respond with 400 if incomplete json" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeIncompleteAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(ErrorIncompleteAlertJson)
    }

    "respond with 400 if alert cannot be parsed" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockAlertManager.createTeamAlert(any[Organization], any[Team], any[Alert])).thenReturn(Success(None))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(s"Unable to parse Alert condition: $AlertCondition.")
    }

    "respond with 404 if team not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if organization not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(),
          SomeNewAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockAlertManager.createTeamAlert(any[Organization], any[Team], any[Alert])).thenReturn(Success(Some(SomeAlert)))
      when(mockAwsWrapper.createAlertNotification(any[Schedule])).thenReturn(Future.successful())

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(GiltTeamBadAuth))),
          SomeNewAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user neither a team member/admin nor an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))


      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockAlertManager.createTeamAlert(any[Organization], any[Team], any[Alert])).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createTeamAlert(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/alerts",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          SomeNewAlertJson))

      verify(mockAwsWrapper, never()).createAlertNotification(any[Schedule])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
