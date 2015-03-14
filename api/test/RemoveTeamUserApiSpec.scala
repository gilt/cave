import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import play.api.mvc.Results
import play.api.test.{FakeRequest, PlaySpecification}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RemoveTeamUserApiSpec extends PlaySpecification with Results with AbstractTeamApiSpec with UserData {

  "DELETE /organizations/$name/teams/$team/users/$email" should {

    "return 204 after removing a user from a team" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.deleteUserFromTeam(USER1, GiltTeam)).thenReturn(Future.successful())

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
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
      when(mockDataManager.deleteUserFromTeam(USER1, GiltTeam)).thenReturn(Future.successful())

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "return 204 even if the user does not exist within CAVE" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "return 401 if no auth token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 401 if unsupported auth token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> GiltOrgBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 403 if the request user not found" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

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

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 500 if an error occurs during findUserByToken" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getOrganization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getTeam" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getTeamsForUser" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getUserByEmail" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during deleteUserFromTeam" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getTeam(GiltOrg, GiltTeamName)).thenReturn(Success(Some(GiltTeam)))
      when(mockDataManager.getTeamsForUser(GiltOrg, SOME_USER)).thenReturn(Future.successful(List(GiltTeamName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.deleteUserFromTeam(USER1, GiltTeam)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, GiltTeamName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/teams/$GiltTeamName/users/$SOME_EMAIL")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }


  }
}
