package worker

import akka.actor._
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.cave.metrics.data._
import init.AwsWrapper
import init.AwsWrapper.WorkItem
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class CoordinatorSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll with AlertJsonData with MockitoSugar {

  def fakeCoordinator(awsWrapper: AwsWrapper, mockCheckers: mutable.Map[ActorRef, WorkItem]): Props = Props(new Coordinator(awsWrapper, shouldSendHistory = false) {

    override val checkers = mockCheckers
    override def createNotifier(item: WorkItem): Unit = { }
  })

  def fakeChecker(parentCoordinator: ActorRef): Props = Props(new Actor {
    def receive = {
      case "abort" =>
        parentCoordinator ! Checker.Aborted("Boom!")
        context stop self
      case "true" =>
        parentCoordinator ! Checker.Done(Success(true))
        context stop self
      case "false" =>
        parentCoordinator ! Checker.Done(Success(false))
        context stop self
    }
  })

  val mockAwsWrapper = mock[AwsWrapper]
  val mockDataManager = mock[CacheDataManager]

  override def afterAll() = {
    system.shutdown()
  }

  "A coordinator" must {

    "return its status" in {
      when(mockAwsWrapper.receiveMessages()(any[ExecutionContext])).thenReturn(Future.successful(List.empty[WorkItem]))

      val checkers = mutable.Map.empty[ActorRef, WorkItem]
      val mockItem = mock[WorkItem]

      val coordinator = TestActorRef(fakeCoordinator(mockAwsWrapper, checkers))

      val checker1 = TestActorRef(fakeChecker(coordinator))
      val checker2 = TestActorRef(fakeChecker(coordinator))
      val checker3 = TestActorRef(fakeChecker(coordinator))
      val checker4 = TestActorRef(fakeChecker(coordinator))
      val checker5 = TestActorRef(fakeChecker(coordinator))
      val checker6 = TestActorRef(fakeChecker(coordinator))

      checkers ++= mutable.Map(checker1 -> mockItem, checker2 -> mockItem, checker3 -> mockItem,
        checker4 -> mockItem, checker5 -> mockItem, checker6 -> mockItem)

      checker1 ! "abort"
      checker2 ! "abort"
      checker3 ! "false"
      checker4 ! "false"
      checker5 ! "false"
      checker6 ! "true"

      coordinator ! Coordinator.StatusRequest

      expectMsgPF() {
        case Coordinator.StatusResponse(currentlyActive, aborted, totalProcessed, noOfAlarmsTriggered) =>
          currentlyActive should be(0)
          aborted should be(2)
          noOfAlarmsTriggered should be(1)
          totalProcessed should be(4)
        case _ => fail("Unexpected message received.")
      }

      coordinator ! PoisonPill
      watch(coordinator)
      expectTerminated(coordinator)
    }
  }
}
