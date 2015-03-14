import controllers.Internal

import scala.util.{Failure, Success}

import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}

class InternalApiSpec extends PlaySpecification with Results with AbstractInternalApiSpec {

  "GET /internal/_healthcheck_" should {

    "respond with 200 and Healthy" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.isHealthy).thenReturn(true)

      val result = new TestController().healthCheck.apply(FakeRequest(GET, "/internal/_healthcheck_"))

      status(result) must equalTo(OK)
      contentAsString(result) must equalTo(Internal.Healthy)
    }

    "respond with 200 and Unhealthy" in running(FakeApplication(withGlobal = mockGlobal)) {
      when(mockDataManager.isHealthy).thenReturn(false)

      val result = new TestController().healthCheck.apply(FakeRequest(GET, "/internal/_healthcheck_"))

      status(result) must equalTo(OK)
      contentAsString(result) must equalTo(Internal.Unhealthy)
    }
  }
}
