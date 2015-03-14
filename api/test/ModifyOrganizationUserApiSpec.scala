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

class ModifyOrganizationUserApiSpec extends PlaySpecification with Results with AbstractOrganizationApiSpec with UserData {

  "PATCH /organizations/$name/users/$email" should {

    "return 202 after changing the role of a user within an organization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.changeOrganizationRole(USER1, GiltOrg, Role.Member)).thenReturn(Future.successful())

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(ACCEPTED)
      contentAsString(result) must equalTo("")
    }

    "return 202 even if the email does not exist in the CAVE database" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(None))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(ACCEPTED)
      contentAsString(result) must equalTo("")
    }

    "return 400 if the request attempts to modify the authenticated user" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().modifyUser(GiltName, SOME_USER_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_USER_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot modify self.")
    }

    "return 400 if no role was passed inside the request body" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("mole" -> Role.Member)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot parse request body: 'role' is missing.")
    }

    "return 401 if no auth token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 401 if unsupported auth token specified" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(GiltOrgBadAuth))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not found" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not admin" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 500 if an error occurs during findUserByToken" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getOrganization" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during getOrganizationsForUser" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

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

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 500 if an error occurs during changeOrganizationRole" in {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), Matchers.any[DateTime])(Matchers.any[ExecutionContext]))
        .thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.getUserByEmail(SOME_EMAIL)).thenReturn(Future.successful(Some(USER1)))
      when(mockDataManager.changeOrganizationRole(USER1, GiltOrg, Role.Member)).thenReturn(Future.failed(new RuntimeException(ErrorMessage)))

      val result = new TestController().modifyUser(GiltName, SOME_EMAIL)(
        FakeRequest("PATCH", s"/organizations/$GiltName/users/$SOME_EMAIL",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.obj("role" -> Role.Member)))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }


  }
}
