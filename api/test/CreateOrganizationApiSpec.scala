import com.cave.metrics.data.Organization
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class CreateOrganizationApiSpec extends PlaySpecification with Results with AbstractOrganizationApiSpec with UserData {

  "POST /organizations" should {

    "return 201 and the created org object" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createOrganization(Matchers.eq(SOME_USER), any[Organization])).thenReturn(Success(Some(GiltOrg)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.createDatabase(GiltName)(mockContext)).thenReturn(Future(Success(true)))

      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(createOrgJson(GiltName, GiltEmail, GiltNotificationUrl))))

      Await.result(result, 5.second)
      verify(mockAwsWrapper).createOrganizationNotification(GiltOrg)
      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      headers(result).get(LOCATION) must beSome(GiltOrgLocation)
      contentAsString(result) must equalTo(GiltOrgJson)
    }

    "return 401 if not token passed in" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(),
          Json.parse(createOrgJson(GiltName, GiltEmail, GiltNotificationUrl))))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "return 403 if user not recognized" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(createOrgJson(GiltName, GiltEmail, GiltNotificationUrl))))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "return 500 if it fails to create Influx database" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createOrganization(Matchers.eq(SOME_USER), any[Organization])).thenReturn(Success(Some(GiltOrg)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.createDatabase(GiltName)(mockContext)).thenReturn(Future(Failure(new RuntimeException(ErrorMessage))))
      when(mockDataManager.deleteOrganization(GiltName)).thenReturn(Success(true))

      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(createOrgJson(GiltName, GiltEmail, GiltNotificationUrl))))

      result.value
      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }

    "return 400 if invalid JSON" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createOrganization(Matchers.eq(SOME_USER), any[Organization])).thenReturn(Success(Some(GiltOrg)))

      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(InvalidOrgGiltMissingEmail)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(ErrorParsingOrgJson)
    }

    "return 400 if invalid name" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(InvalidOrgBadName)))

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(s"Invalid organization name $BadName.")
    }

    "return 409 if an organization with this name already exists" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createOrganization(Matchers.eq(SOME_USER), any[Organization])).thenReturn(Success(None))

      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(createOrgJson(GiltName, GiltEmail, GiltNotificationUrl))))

      status(result) must equalTo(CONFLICT)
      contentAsString(result) must equalTo(s"Organization '$GiltName' already exists.")
    }

    "return 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(any[String], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.createOrganization(Matchers.eq(SOME_USER), any[Organization])).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().createOrganization()(
        FakeRequest(POST, "/organizations", FakeHeaders(Seq(AUTHORIZATION -> Seq(AUTH_TOKEN))),
          Json.parse(createOrgJson(GiltName, GiltEmail, GiltNotificationUrl))))

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(ErrorMessage)
    }
  }
}
