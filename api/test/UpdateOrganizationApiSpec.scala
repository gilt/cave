import com.cave.metrics.data._
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class UpdateOrganizationApiSpec extends PlaySpecification with Results with AbstractOrganizationApiSpec with UserData {

  "PUT /organizations/:name" should {

    "respond with 200 and the updated account object" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.updateOrganization(any[Organization], any[OrganizationPatch])).thenReturn(Success(Some(GiltUpdatedOrg)))
      when(mockAwsWrapper.updateOrganizationNotification(GiltUpdatedOrg)).thenReturn(Future.successful())

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo(GiltUpdatedOrgJson)
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.updateOrganization(any[Organization], any[OrganizationPatch])).thenReturn(Success(Some(GiltUpdatedOrg)))
      when(mockAwsWrapper.updateOrganizationNotification(GiltUpdatedOrg)).thenReturn(Future.successful())

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(BASIC_AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo(GiltUpdatedOrgJson)
    }

    "does not send notification when URL remains the same" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.updateOrganization(any[Organization], any[OrganizationPatch])).thenReturn(Success(Some(GiltSameUrlOrg)))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), None)))

      result.value
      verify(mockAwsWrapper, never()).updateOrganizationNotification(any[Organization])
      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo(GiltSameUrlOrgJson)
    }

    "respond with 401 if no authentication provided" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }


    "respond with 403 if authorization token is invalid" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if the user is not an admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 400 if invalid JSON" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          Json.parse(InvalidOrgGiltMissingEmail)))

      verify(mockAwsWrapper, never()).updateOrganizationNotification(any[Organization])
      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("No fields specified for update.")
    }

    "respond with 404 if the organization does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      verify(mockAwsWrapper, never()).updateOrganizationNotification(any[Organization])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if the account is deleted before it can be updated" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.updateOrganization(any[Organization], any[OrganizationPatch])).thenReturn(Success(None))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      verify(mockAwsWrapper, never()).updateOrganizationNotification(any[Organization])
      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "return 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      verify(mockAwsWrapper, never()).updateOrganizationNotification(any[Organization])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during update" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))

      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.updateOrganization(any[Organization], any[OrganizationPatch])).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().updateOrganization(GiltName)(
        FakeRequest(PUT, s"/organizations/$GiltName",
          FakeHeaders(Seq((AUTHORIZATION, Seq(AUTH_TOKEN)))),
          updateOrgJson(Some(GiltUpdatedEmail), Some(GiltUpdatedNotificationUrl))))

      verify(mockAwsWrapper, never()).updateOrganizationNotification(any[Organization])
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

  }
}
