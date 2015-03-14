import com.cave.metrics.data.{Role, Organization, Team}
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

class CreateTeamApiSpec extends PlaySpecification with Results with AbstractTeamApiSpec with UserData {

  "POST /organizations/:name/teams" should {

    "return 201 and the created team object" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.createTeam(any[Organization], any[Team])).thenReturn(Success(Some(GiltTeam)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.createDatabase(anyString())(any[ExecutionContext])).thenReturn(Future.successful(Success(true)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltTeamLocation)
      contentAsString(result) must equalTo(GiltTeamJson)
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.createTeam(any[Organization], any[Team])).thenReturn(Success(Some(GiltTeam)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.createDatabase(anyString())(any[ExecutionContext])).thenReturn(Future.successful(Success(true)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(BASIC_AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltTeamLocation)
      contentAsString(result) must equalTo(GiltTeamJson)
    }

    "respond with 400 if no team name was specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltTeamName))))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(ErrorParsingTeamJson)
    }

    "respond with 400 if team name is not suitable" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(InvalidTeamBadName)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(s"Invalid team name $BadName.")
    }

    "respond with 409 if a team with the same name exists" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.createTeam(any[Organization], any[Team])).thenReturn(Success(None))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(CONFLICT)
      contentAsString(result) must equalTo(s"Team '$GiltTeamName' already exists.")
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(GiltOrgBadAuth))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if wrong credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs during findUserByToken" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during getOrganization" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during getOrganizationsForUser" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during createTeam" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.createTeam(any[Organization], any[Team])).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during createDatabase" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.createTeam(any[Organization], any[Team])).thenReturn(Success(Some(GiltTeam)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.createDatabase(anyString())(any[ExecutionContext])).thenReturn(Future.successful(Failure(new RuntimeException(ErrorMessage))))
      when(mockDataManager.deleteTeam(any[Organization], anyString)).thenReturn(Success(true))

      val result = new TestController().createTeam(GiltName).apply(
        FakeRequest(POST, s"/organizations/$GiltName/teams",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(teamNameJson(GiltTeamName))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
