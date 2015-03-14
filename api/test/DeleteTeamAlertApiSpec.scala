import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import play.api.mvc.Results
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DeleteTeamAlertApiSpec extends PlaySpecification with Results with AbstractAlertApiSpec with UserData {

  "DELETE /organizations/:name/teams/:team/alerts/:id" should {

    "respond with 204 when deletion succeeds" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockAlertManager.deleteAlert(AlertId)).thenReturn(Success(true))
      when(mockAwsWrapper.deleteAlertNotification(AlertId, GiltName)).thenReturn(Future.successful())

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "also works if the user is an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      when(mockAlertManager.deleteAlert(AlertId)).thenReturn(Success(true))
      when(mockAwsWrapper.deleteAlertNotification(AlertId, GiltName)).thenReturn(Future.successful())

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if organization not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if team not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if alert not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockAlertManager.deleteAlert(AlertId)).thenReturn(Success(false))

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId"))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> GiltTeamBadAuth))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user is neither team member/admin nor org admin" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Viewer)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
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
      when(mockAlertManager.deleteAlert(AlertId)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().deleteTeamAlert(GiltName, GiltTeamName, AlertId)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteAlertNotification(any[String], any[String])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
