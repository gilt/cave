import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import play.api.mvc.Results
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DeleteOrganizationTokenApiSpec extends PlaySpecification with Results with AbstractTokenApiSpec with UserData {

  "DELETE /organizations/:name/tokens/:id" should {

    "respond with 204 and no content" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestOrgToken.id.get)).thenReturn(Success(true))

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestOrgToken.id.get)).thenReturn(Success(true))

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> BASIC_AUTH_TOKEN))

      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "respond with 400 if organization has only one token" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().deleteOrganizationToken(GiltName, GiltOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$GiltName/tokens/${GiltOrgToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot delete the last organization token.")
    }

    "respond with 404 if organization not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(None))

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 if token not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestOrgToken.id.get)).thenReturn(Success(false))

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(NOT_FOUND)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> TestOrgBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Member)))

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Admin)))
      when(mockDataManager.deleteToken(TestOrgToken.id.get)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().deleteOrganizationToken(TestName, TestOrgToken.id.get).apply(
        FakeRequest(DELETE, s"/organizations/$TestName/tokens/${TestOrgToken.id}")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
