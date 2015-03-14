import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import play.api.mvc._
import play.api.test._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RemoveOrganizationUserApiSpec extends PlaySpecification with Results with AbstractOrganizationApiSpec with UserData {

  "DELETE /organizations/$name/users/$email" should {

    "return 204 after removing a user from an organization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.deleteUserFromOrganization(USER1, GiltOrg)).thenReturn(Future.successful())

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "return 204 even if the user does not exist within the organization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "return 400 if the request attempts to remove the authenticated user from the organization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().removeUser(GiltName, SOME_USER_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_USER_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot remove self.")
    }

    "return 401 if no auth token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 401 if unsupported auth token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> GiltOrgBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 403 if the request user not found" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 403 if the request user not admin" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 500 if an error occurs during findUserByToken" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getOrganization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getOrganizationsForUser" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getUserByEmail" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during deleteUserFromOrganization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.deleteUserFromOrganization(USER1, GiltOrg)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().removeUser(GiltName, SOME_EMAIL)(
        FakeRequest(DELETE, s"/organizations/$GiltName/users/$SOME_EMAIL").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }


  }
}
