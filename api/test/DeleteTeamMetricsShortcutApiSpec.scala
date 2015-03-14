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

class DeleteTeamMetricsShortcutApiSpec extends PlaySpecification with Results with AbstractMetricsApiSpec with UserData {

  val MetricName = "metric"
  val organization = GiltOrg
  val team = GiltTeam
  val user = SOME_USER

  "DELETE /metric-names/:metric" should {
    "respond with 204 when metric exists" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(user)))
      when(mockDataManager.getOrganization(organization.name)).thenReturn(Success(Some(organization)))
      when(mockDataManager.getTeam(organization, team.name)).thenReturn(Success(Some(team)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(organization), Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(team.name -> Role.Admin)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.deleteMetric(s"${team.name}.${organization.name}", MetricName)(mockContext)).thenReturn(Future.successful(true))

      val result = new TestController().deleteMetric(MetricName)(FakeRequest(DELETE, s"/metrics-names/$MetricName").
        withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"${team.name}.${organization.name}.$BaseUrl"))
      status(result) must equalTo(NO_CONTENT)
      contentAsString(result) must equalTo("")
    }

    "respond with 404 metric doesn't exist" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(user)))
      when(mockDataManager.getOrganization(organization.name)).thenReturn(Success(Some(organization)))
      when(mockDataManager.getTeam(organization, team.name)).thenReturn(Success(Some(team)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(organization), Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(team.name -> Role.Admin)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.deleteMetric(s"${team.name}.${organization.name}", MetricName)(mockContext)).thenReturn(Future.successful(false))

      val result = new TestController().deleteMetric(MetricName)(FakeRequest(DELETE, s"/metrics-names/$MetricName").
        withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"${team.name}.${organization.name}.$BaseUrl"))
      status(result) must equalTo(NOT_FOUND)
    }

    "work for org admins" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(user)))
      when(mockDataManager.getOrganization(organization.name)).thenReturn(Success(Some(organization)))
      when(mockDataManager.getTeam(organization, team.name)).thenReturn(Success(Some(team)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(organization), Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(team.name -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(organization.name -> Role.Admin)))
      when(mockInfluxClientFactory.getClient(None)).thenReturn(mockClient -> mockContext)
      when(mockClient.deleteMetric(s"${team.name}.${organization.name}", MetricName)(mockContext)).thenReturn(Future.successful(true))

      val result = new TestController().deleteMetric(MetricName)(FakeRequest(DELETE, s"/metrics-names/$MetricName").
        withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"${team.name}.${organization.name}.$BaseUrl"))
      status(result) must equalTo(NO_CONTENT)
    }

    "respond with 403 when it's not an admin user" in running(FakeApplication(withGlobal = mockGlobal)) {
      Mockito.reset(mockAwsWrapper, mockDataManager, mockInfluxClientFactory)
      when(mockDataManager.findUserByToken(Matchers.eq(SOME_TOKEN), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(Some(user)))
      when(mockDataManager.getOrganization(organization.name)).thenReturn(Success(Some(organization)))
      when(mockDataManager.getTeam(organization, team.name)).thenReturn(Success(Some(team)))
      when(mockDataManager.getTeamsForUser(Matchers.eq(organization), Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(team.name -> Role.Member)))
      when(mockDataManager.getOrganizationsForUser(Matchers.eq(user))(Matchers.any[ExecutionContext])).thenReturn(Future.successful(List(team.name -> Role.Member)))

      val result = new TestController().deleteMetric(MetricName)(FakeRequest(DELETE, s"/metrics-names/$MetricName").
        withHeaders(AUTHORIZATION -> AUTH_TOKEN, HOST -> s"${team.name}.${organization.name}.$BaseUrl"))
      status(result) must equalTo(FORBIDDEN)
    }
  }
}
