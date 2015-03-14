import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DeleteOrganizationApiSpec extends PlaySpecification with Results with AbstractOrganizationApiSpec with UserData {

  "DELETE /organizations/:name" should {

    "respond with 204 when the organization is deleted" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getTeams(GiltOrg)).thenReturn(Success(List()))
      when(mockDataManager.deleteOrganization(GiltName)).thenReturn(Success(true))
      when(mockAwsWrapper.deleteOrganizationNotification(GiltName)).thenReturn(Future.successful())

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getTeams(GiltOrg)).thenReturn(Success(List()))
      when(mockDataManager.deleteOrganization(GiltName)).thenReturn(Success(true))
      when(mockAwsWrapper.deleteOrganizationNotification(GiltName)).thenReturn(Future.successful())

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> BASIC_AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "respond with 400 when the organization has teams" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getTeams(GiltOrg)).thenReturn(Success(List(GiltTeam)))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot delete account with teams attached. Delete teams first.")
    }

    "respond with 404 if organization does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 when the organization is deleted twice" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getTeams(GiltOrg)).thenReturn(Success(List()))
      when(mockDataManager.deleteOrganization(GiltName)).thenReturn(Success(false))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if no credentials specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName"))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> GiltOrgBadAuth))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if wrong credentials specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user is not org admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs during findUserByToken" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during getOrganization" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during getOrganizationsForUser" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))


      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during getTeams" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getTeams(GiltOrg)).thenReturn(Failure(new RuntimeException(ErrorMessage)))


      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 500 if an error occurs during deleteOrganization" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getTeams(GiltOrg)).thenReturn(Success(List()))
      when(mockDataManager.deleteOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().deleteOrganization(GiltName).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      verify(mockAwsWrapper, never()).deleteOrganizationNotification(any[String])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
