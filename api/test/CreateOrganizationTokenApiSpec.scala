import com.cave.metrics.data.{Organization, Role, Token}
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

class CreateOrganizationTokenApiSpec extends PlaySpecification with Results with AbstractTokenApiSpec with UserData {

  "POST /organizations/:name/tokens" should {

    "respond with 201 and return the new token" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.addOrganizationToken(any[Organization], any[Token])).thenReturn(Success(GiltOrgToken))

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltOrgTokenDescription))))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltOrgTokenLocation)
      contentAsString(result) must equalTo(GiltOrgTokenJson)
    }

    "also work with Basic authentication" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.addOrganizationToken(any[Organization], any[Token])).thenReturn(Success(GiltOrgToken))

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(BASIC_AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltOrgTokenDescription))))

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltOrgTokenLocation)
      contentAsString(result) must equalTo(GiltOrgTokenJson)
    }

    "respond with 400 if the account already has maximum number of tokens" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(TestName)).thenReturn(Success(Some(TestOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(TestName -> Role.Admin)))

      val result = new TestController().createOrganizationToken(TestName)(
        FakeRequest(POST, s"/organizations/$TestName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(TestOrgTokenDescription))))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Too many tokens for organization.")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))
      when(mockDataManager.addOrganizationToken(any[Organization], any[Token])).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltOrgTokenDescription))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(),
          Json.parse(descriptionJson(GiltOrgTokenDescription))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(GiltOrgBadAuth))),
          Json.parse(descriptionJson(GiltOrgTokenDescription))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }


    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltOrgTokenDescription))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not admin" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(descriptionJson(GiltOrgTokenDescription))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 400 if invalid JSON" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Admin)))

      val result = new TestController().createOrganizationToken(GiltName)(
        FakeRequest(POST, s"/organizations/$GiltName/tokens",
          FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(InvalidOrgGiltMissingTokens)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Cannot parse request body: description is missing.")
    }
  }
}
