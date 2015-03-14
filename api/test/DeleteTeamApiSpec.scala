import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

class DeleteTeamApiSpec extends PlaySpecification with Results with AbstractTeamApiSpec with UserData {

  "DELETE /organizations/:name/teams/:team" should {

    "respond with 204 when the team is deleted" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.deleteTeam(GiltOrg, GiltTeamName)).thenReturn(Success(true))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.deleteTeam(GiltOrg, GiltTeamName)).thenReturn(Success(true))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> BASIC_AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "also work if user is org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.deleteTeam(GiltOrg, GiltTeamName)).thenReturn(Success(true))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if account not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if team not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if the team is deleted quickly twice" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.deleteTeam(GiltOrg, GiltTeamName)).thenReturn(Success(false))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> GiltTeamBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user neother team admin nor org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs during deleteTeam" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.deleteTeam(GiltOrg, GiltTeamName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().deleteTeam(GiltName, GiltTeamName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}