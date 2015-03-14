import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.{FakeHeaders, FakeRequest, PlaySpecification}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

class AddTeamUserApiSpec extends PlaySpecification with Results with AbstractTeamApiSpec with UserData {

  "POST /organizations/$name/teams/$team/users" should {

    "return 202 after adding an existing user to a team" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.addUserToTeam(USER1, GiltTeam, Role.Member)).thenReturn(Future.successful(true))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(ACCEPTED)
      contentAsString(result) must equalTo("")
    }

    "also work if the user is an org admin" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.addUserToTeam(USER1, GiltTeam, Role.Member)).thenReturn(Future.successful(true))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(ACCEPTED)
      contentAsString(result) must equalTo("")
    }

    "return 202 after adding a non existing user to a team" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))
      when(mockMailService.sendAttemptedTeamAdd(SOME_EMAIL, GiltOrg, GiltTeam, SOME_USER)).thenReturn(Future.successful())

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(ACCEPTED)
      contentAsString(result) must equalTo("")
    }

    "return 409 if the user already is in the team" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.addUserToTeam(USER1, GiltTeam, Role.Member)).thenReturn(Future.successful(false))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(CONFLICT)
      contentAsString(result) must equalTo("")
    }

    "return 400 if the input data does not contain an email" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("mail" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Unable to parse request body: must have both 'email' and 'role'.")
    }

    "return 400 if the input data does not contain a role" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "mole" -> "admin")))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Unable to parse request body: must have both 'email' and 'role'.")
    }

    "return 400 if the input data does not contain a valid role" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> "leader")))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Unable to parse request body: must have both 'email' and 'role'.")
    }

    "return 401 if no authorization token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(),
          Json.obj("email" -> SOME_EMAIL, "role" -> "leader")))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 401 if unsupported authorization token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(GiltOrgBadAuth))),
          Json.obj("email" -> SOME_EMAIL, "role" -> "leader")))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not found" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 403 if the request user is neither a team admin nor an org admin" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 500 if there's an error during getOrganization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if there's an error during getTeam" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if there's an error during getOrganizationsForUser" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if there's an error during getUserByEmail" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if there's an error during addUserToOrganization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.addUserToTeam(USER1, GiltTeam, Role.Member)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().addUser(GiltName, GiltTeamName)(
        FakeRequest(POST, s"/organizations/$GiltName/team/$GiltTeamName/users",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("email" -> SOME_EMAIL, "role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }

}

