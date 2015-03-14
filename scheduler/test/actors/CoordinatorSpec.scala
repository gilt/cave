package actors

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.cave.metrics.data._
import init.AwsWrapper
import init.AwsWrapper.WorkItem
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.specs2.matcher.ShouldMatchers

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

class CoordinatorSpec extends TestKit(ActorSystem()) with WordSpecLike with ShouldMatchers with ImplicitSender with BeforeAndAfterAll with AlertJsonData with MockitoSugar {

  val mockAwsWrapper = mock[AwsWrapper]
  val mockDataManager = mock[CacheDataManager]

  override def afterAll() = {
    system.shutdown()
  }

  "A coordinator" must {

    "create schedulers for all enabled alerts" in {

      val SomeId = "1234"
      val AnotherId = "4321"
      val OtherId = "12345"

      val alerts = List(
        Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting))),
        Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(AnotherId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting))))

      val moreAlerts = List(
        Schedule(TeamName, None, None, NotificationUrl, Alert(Some(OtherId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
      )

      when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> alerts, TeamName -> moreAlerts)))
      when(mockAwsWrapper.receiveMessages()(any[ExecutionContext])).thenReturn(Future.successful(List.empty[WorkItem]))
      val coordinator = TestActorRef(Props(new Coordinator(mockAwsWrapper, mockDataManager) {
        override def createScheduler(schedule: Schedule) = {}
      }))

      coordinator ! Coordinator.StatusRequest

      expectMsgPF() {
        case Coordinator.StatusResponse(cache, schedules) =>
          cache.schedulesByOrganization should haveSize(2)
          val forOrgName = cache.schedulesByOrganization(OrgName)
          forOrgName should haveSize(2)
          val forTeamName = cache.schedulesByOrganization(TeamName)
          forTeamName should haveSize(1)

          schedules should haveSize(3)

        case _ => fail("Unexpected message received.")
      }


      coordinator ! PoisonPill
      watch(coordinator)
      expectTerminated(coordinator)
    }
  }
}
