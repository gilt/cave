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

class GetTeamTokensApiSpec extends PlaySpecification with Results with AbstractTokenApiSpec with UserData {

  "GET /organizations/:name/teams/:team/tokens" should {

    "retrieve the list of tokens for an existing team" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo(s"[$GiltTeamTokenJson]")
    }

    "also work if the user is an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo(s"[$GiltTeamTokenJson]")
    }


    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> BASIC_AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo(s"[$GiltTeamTokenJson]")
    }

    "respond with 404 if the organization does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if the team does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> GiltTeamBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user is neither team member/admin nor org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().getTeamTokens(GiltName, GiltTeamName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/team/$GiltTeamName/tokens")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }
  }
}