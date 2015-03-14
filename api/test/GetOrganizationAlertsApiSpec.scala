import com.cave.metrics.data.Role
import controllers.Alerts
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

class GetOrganizationAlertsApiSpec extends PlaySpecification with Results with AbstractAlertApiSpec with UserData {

  val AuthRequest = FakeRequest().withHeaders(AUTHORIZATION -> AUTH_TOKEN)

  "GET /organizations/:name/alerts" should {

    "retrieve the list of alerts for an existing organization" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      val limit = 20
      val offset = 0
      when(mockAlertManager.getOrganizationAlerts(GiltOrg, limit + 1, offset)).thenReturn(Success(List(SomeAlert)))

      val result = new TestController().getOrganizationAlerts(GiltName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert))
    }

    "retrieve the list of alerts starting from offset with limit" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      val limit = 1
      val offset = 1
      when(mockAlertManager.getOrganizationAlerts(GiltOrg, limit + 1, offset)).thenReturn(Success(List(SomeAlert)))

      val result = new TestController().getOrganizationAlerts(GiltName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert))
    }

    "return a paging token for the next page if there are more results" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      val limit = 2
      val offset = 0
      when(mockAlertManager.getOrganizationAlerts(GiltOrg, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert, SomeAlert2, SomeAlert3)))

      val result = new TestController().getOrganizationAlerts(GiltName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      val GiltOrgNextAlertLocation = s"""<$BaseUrl/organizations/$GiltName/alerts?limit=$limit&offset=${offset + limit}>; rel="next""""
      headers(result).get(Alerts.LINK) must beSome(GiltOrgNextAlertLocation)
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert, SomeAlert2))
    }

    "return no paging token for the next page if there are no more results" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      val limit = 2
      val offset = 0
      when(mockAlertManager.getOrganizationAlerts(GiltOrg, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert, SomeAlert2)))

      val result = new TestController().getOrganizationAlerts(GiltName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      headers(result).get(Alerts.LINK) must beNone
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert, SomeAlert2))
    }

    "return a paging token for the previous page if on non-first page" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
      (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      val limit = 2
      val offset = 2
      val GiltOrgPrevAlertLocation = s"""<$BaseUrl/organizations/$GiltName/alerts?limit=$limit&offset=0>; rel="prev""""
      when(mockAlertManager.getOrganizationAlerts(GiltOrg, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert, SomeAlert2)))

      val result = new TestController().getOrganizationAlerts(GiltName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      headers(result).get(Alerts.LINK) must beSome(GiltOrgPrevAlertLocation)
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert, SomeAlert2))
    }

    "return paging tokens for both the next and the previous page if on non-first page and there are more results" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      val limit = 2
      val offset = 2
      val GiltOrgNextAlertLocation = s"""<$BaseUrl/organizations/$GiltName/alerts?limit=$limit&offset=4>; rel="next""""
      val GiltOrgPrevAlertLocation = s"""<$BaseUrl/organizations/$GiltName/alerts?limit=$limit&offset=0>; rel="prev""""
      when(mockAlertManager.getOrganizationAlerts(GiltOrg, limit + 1, offset))
        .thenReturn(Success(List(SomeAlert, SomeAlert2, SomeAlert3)))

      val result = new TestController().getOrganizationAlerts(GiltName, limit, offset)(AuthRequest)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")

      headers(result).get(Alerts.LINK) must beSome(s"$GiltOrgNextAlertLocation, $GiltOrgPrevAlertLocation")
      Json.parse(contentAsString(result)) must equalTo(Json.arr(SomeAlert, SomeAlert2))
    }


    "respond with 401 if no credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getOrganizationAlerts(GiltName)(
        FakeRequest(GET, s"/organizations/$GiltName/alerts"))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 401 if unsupported credentials are specified" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)

      val result = new TestController().getOrganizationAlerts(GiltName)(
        FakeRequest(GET, s"/organizations/$GiltName/alerts")
          .withHeaders(AUTHORIZATION -> GiltOrgBadAuth))

      status(result) must equalTo(UNAUTHORIZED)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not found" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = new TestController().getOrganizationAlerts(GiltName)(
        FakeRequest(GET, s"/organizations/$GiltName/alerts")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 403 if user not member or admin" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Viewer)))

      val result = new TestController().getOrganizationAlerts(GiltName)(
        FakeRequest(GET, s"/organizations/$GiltName/alerts")
          .withHeaders(AUTHORIZATION -> AUTH_TOKEN))

      status(result) must equalTo(FORBIDDEN)
      contentAsString(result) must equalTo("")
    }

    "respond with 500 if an error occurs" in running(FakeApplication(withGlobal = mockGlobal)) {

      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])
        (any[ExecutionContext])).thenReturn(Future.successful(Some(SOME_USER)))
      when(mockDataManager.getOrganization(GiltName)).thenReturn(Success(Some(GiltOrg)))
      when(mockDataManager.getOrganizationsForUser(SOME_USER)).thenReturn(Future.successful(List(GiltName -> Role.Member)))
      when(mockAlertManager.getOrganizationAlerts(Matchers.eq(GiltOrg), Matchers.anyInt(), Matchers.anyInt())).thenReturn(Failure(new RuntimeException(ErrorMessage)))

      val result = new TestController().getOrganizationAlerts(GiltName)(AuthRequest)

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsString(result) must equalTo(InternalErrorMessage)
    }
  }
}
