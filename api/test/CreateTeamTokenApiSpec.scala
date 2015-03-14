import data.UserData
import org.joda.time.DateTime
import org.mockito.{Matchers, Mockito}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

import com.cave.metrics.data.{Role, Organization, Team, Token}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test._

class CreateTeamTokenApiSpec extends PlaySpecification with Results with AbstractTokenApiSpec with UserData {

  "POST /organizations/:name/teams/:team/tokens" should {

    "respond with 201 and return the new token" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.addTeamToken(any[Organization], any[Team], any[Token])).thenReturn(Success(GiltTeamToken))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltTeamTokenLocation)
      contentAsString(result) must equalTo(GiltTeamTokenJson)
    }

    "also work if user is an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.addTeamToken(any[Organization], any[Team], any[Token])).thenReturn(Success(GiltTeamToken))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltTeamTokenLocation)
      contentAsString(result) must equalTo(GiltTeamTokenJson)
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.addTeamToken(any[Organization], any[Team], any[Token])).thenReturn(Success(GiltTeamToken))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(BASIC_AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltTeamTokenLocation)
      contentAsString(result) must equalTo(GiltTeamTokenJson)
    }

    "respond with 400 if the team already has maximum number of tokens" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getTeam(TestOrg, TestTeamName)).thenReturn(Success(Some(TestTeam)))
      when(mockDataManager.getTeamsForUser(TestOrg, SOME_USER)).thenReturn(Future.successful(List(TestTeamName -> Role.Admin)))

      val result = new TestController().createTeamToken(TestName, TestTeamName)(
        FakeRequest(POST, s"/organizations/$TestName/teams/$TestTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Too many tokens for team.")
    }

    "respond with 404 if the account does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if the team does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(None))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.addTeamToken(any[Organization], any[Team], any[Token])).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(GiltTeamBadAuth))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user neither a team admin nor an org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamTokenDescription))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 400 if invalid JSON" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))

      val result = new TestController().createTeamToken(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/teams/$GiltTeamName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(InvalidOrgGiltMissingTokens)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot parse request body: description is missing.")
    }
  }
}
