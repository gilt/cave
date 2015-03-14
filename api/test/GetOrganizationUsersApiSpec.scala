import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{FakeRequest, FakeApplication, PlaySpecification}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

class GetOrganizationUsersApiSpec extends PlaySpecification with Results with AbstractOrganizationApiSpec with UserData {

  "GET /organizations/:org/users" should {

    "retrieve the list of users for an existing organization" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      when(mockDataManager.getUsersForOrganization(GiltOrg)).thenReturn(Future.successful(SOME_USERS))

      val result = new TestController().getUsers(GiltName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/users")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SOME_USERS_JSON)
    }

    "also works with basic authorization" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      when(mockDataManager.getUsersForOrganization(GiltOrg)).thenReturn(Future.successful(SOME_USERS))

      val result = new TestController().getUsers(GiltName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/users")
          .withHeaders(AUTHORIZATION -> BASIC_AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsJson(result) must equalTo(SOME_USERS_JSON)
    }

    "return 200 and an empty JSON if no users found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      when(mockDataManager.getUsersForOrganization(GiltOrg)).thenReturn(Future.successful(List()))

      val result = new TestController().getUsers(GiltName).apply(
        FakeRequest(GET, s"/organizations/$GiltName/users")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      contentAsString(result) must equalTo("[]")
    }
  }

  "return 404 if the organization does not exist" in running(FakeApplication(withGlobal = mockGlobal)) {
    Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
    when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
      (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
    when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(None))

    val result = new TestController().getUsers(GiltName).apply(
      FakeRequest(GET, s"/organizations/$GiltName/users")
        .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

    status(result) must equalTo(NOT_FOUND)
    contentAsString(result) must equalTo("")
  }

  "return 401 if no authorization token provided" in running(FakeApplication(withGlobal = mockGlobal)) {
    Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

    val result = new TestController().getUsers(GiltName).apply(
      FakeRequest(GET, s"/organizations/$GiltName/users"))

    status(result) must equalTo(UNAUTHORIZED)
    contentAsString(result) must equalTo("")
  }

  "return 401 if unsupported authorization token provided" in running(FakeApplication(withGlobal = mockGlobal)) {
    Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

    val result = new TestController().getUsers(GiltName).apply(
      FakeRequest(GET, s"/organizations/$GiltName/users")
        .withHeaders(AUTHORIZATION -> GiltOrgBadAuth))

    status(result) must equalTo(UNAUTHORIZED)
    contentAsString(result) must equalTo("")
  }

  "return 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
    Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
    when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
      (any[ExecutionContext])).thenReturn(Future.successful(None))

    val result = new TestController().getUsers(GiltName).apply(
      FakeRequest(GET, s"/organizations/$GiltName/users")
        .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

    status(result) must equalTo(FORBIDDEN)
    contentAsString(result) must equalTo("")
  }

  "return 403 if user not member or admin" in running(FakeApplication(withGlobal = mockGlobal)) {
    Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
    when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
      (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
    when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
    when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))

    val result = new TestController().getUsers(GiltName).apply(
      FakeRequest(GET, s"/organizations/$GiltName/users")
        .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

    status(result) must equalTo(FORBIDDEN)
    contentAsString(result) must equalTo("")
  }

  "return 500 if there is an error" in running(FakeApplication(withGlobal = mockGlobal)) {
    Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
    when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
      (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
    when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
    when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
    when(mockDataManager.getUsersForOrganization(GiltOrg)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

    val result = new TestController().getUsers(GiltName).apply(
      FakeRequest(GET, s"/organizations/$GiltName/users")
        .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

    status(result) must equalTo(INTERNAL_SERVER_ERROR)
    contentAsString(result) must equalTo(InternalErrorMessage)
  }

}
