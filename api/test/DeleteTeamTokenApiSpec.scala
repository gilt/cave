import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}

class DeleteTeamTokenApiSpec extends PlaySpecification with Results with AbstractTokenApiSpec with UserData {

  "DELETE /organizations/:name/teams/:team/tokens/:id" should {

    "respond with 204 and no content" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(Some(TestTeam)))
      when(mockDataManager.getTeamsForUser(TestOrg, SOME_USER)).thenReturn(Future.successful(List(TestTeamName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestTeamToken.id.get)).thenReturn(Success(true))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "also work if user is an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(Some(TestTeam)))
      when(mockDataManager.getTeamsForUser(TestOrg, SOME_USER)).thenReturn(Future.successful(List(TestTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestTeamToken.id.get)).thenReturn(Success(true))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(Some(TestTeam)))
      when(mockDataManager.getTeamsForUser(TestOrg, SOME_USER)).thenReturn(Future.successful(List(TestTeamName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestTeamToken.id.get)).thenReturn(Success(true))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> BASIC_AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "respond with 400 if team has only one token" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.deleteToken(GiltTeamToken.id.get)).thenReturn(Success(true))

      val result = new TestController().deleteTeamToken(GiltName, GiltTeamName, GiltTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/tokens/${GiltTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot delete the last team token.")
    }

    "respond with 404 if organization not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(None))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if team not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(None))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if token not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(Some(TestTeam)))
      when(mockDataManager.getTeamsForUser(TestOrg, SOME_USER)).thenReturn(Future.successful(List(TestTeamName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestTeamToken.id.get)).thenReturn(Success(false))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> TestTeamBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user neither team admin nor org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(Some(TestTeam)))
      when(mockDataManager.getTeamsForUser(TestOrg, SOME_USER)).thenReturn(Future.successful(List(TestTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Member)))
      when(mockDataManager.deleteToken(TestTeamToken.id.get)).thenReturn(Success(true))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(Some(TestTeam)))
      when(mockDataManager.getTeamsForUser(TestOrg, SOME_USER)).thenReturn(Future.successful(List(TestTeamName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestTeamToken.id.get)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().deleteTeamToken(TestName, TestTeamName, TestTeamToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/teams/$TestTeamName/tokens/${TestTeamToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
