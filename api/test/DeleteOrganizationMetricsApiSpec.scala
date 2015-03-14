import com.cave.metrics.data.Role
import data.UserData
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{FakeRequest, FakeApplication, PlaySpecification}
import scala.concurrent.{Future, ExecutionContext}
import scala.Some
import scala.util.Success

class DeleteOrganizationMetricsApiSpec extends PlaySpecification with Results with AbstractMetricsApiSpec with UserData {

  val MetricName = "metric"
  val organization = GiltOrg
  val user = SOME_USER

  "DELETE /organizations/:name/metric-names/:metric" should {
    "respond with 204 when deletion succeeds" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(user)))
      when(mockDataManager.getOrganization(organization.name)).thenReturn(Success(Some(organization)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(organization.name -> Role.Admin)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.deleteMetric(s"${organization.name}", MetricName)(mockContext)).thenReturn(Future.successful(true))

      val result = new TestController().deleteOrganizationMetric(organization.name, MetricName)(FakeRequest(DELETE, s"/organizations/${organization.name}/metrics-names/$MetricName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))
      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 when metric doesn't exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(user)))
      when(mockDataManager.getOrganization(organization.name)).thenReturn(Success(Some(organization)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(organization.name -> Role.Admin)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.deleteMetric(s"${organization.name}", MetricName)(mockContext)).thenReturn(Future.successful(false))

      val result = new TestController().deleteOrganizationMetric(organization.name, MetricName)(FakeRequest(DELETE, s"/organizations/${organization.name}/metrics-names/$MetricName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))
      status(result) must equalTo(NOT_FOUND)
    }

    "respond with 403 when it's not an admin user" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(user)))
      when(mockDataManager.getOrganization(organization.name)).thenReturn(Success(Some(organization)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(organization.name -> Role.Member)))

      val result = new TestController().deleteOrganizationMetric(organization.name, MetricName)(FakeRequest(DELETE, s"/organizations/${organization.name}/metrics-names/$MetricName").withHeaders(AUTHORIZATION -> AUTH_TOKEN))
      status(result) must equalTo(FORBIDDEN)
    }
  }
}
