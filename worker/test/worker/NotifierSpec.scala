package worker

import java.util.concurrent.Executor

import akka.actor._
import akka.testkit.TestKit
import com.cave.metrics.data.{AlertJsonData, Check}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import worker.web.{BadStatus, NotificationSender}

import scala.concurrent.Future

object NotifierSpec extends AlertJsonData {

  object FakeNotificationSender extends NotificationSender {
    def send(check: Check)(implicit exec: Executor): Future[Boolean] =
      if (check.schedule.alert.description == AlertDescription) Future.successful(true)
      else if (check.schedule.alert.description == AlertFiveDescription) Future.successful(false)
      else Future.failed(BadStatus(401))

    def shutdown(): Unit = { }
  }

  def fakeNotifier(n: Check): Props = Props(new Notifier(n) {
    override def client = FakeNotificationSender
  })
}

class NotifierSpec extends TestKit(ActorSystem()) with WordSpecLike with BeforeAndAfterAll {

  import worker.NotifierSpec._

  override def afterAll() = {
    system.shutdown()
  }

  "A notifier" must {
    "send Done(true) when successful" in {
      val notifier = system.actorOf(Props(new StepParent(fakeNotifier(InsufficientOrders), testActor)), "successful")

      expectMsg(Notifier.Done(result = true))
      watch(notifier)
      expectTerminated(notifier)
    }

    "send Done(false) when unsuccessful" in {
      val notifier = system.actorOf(Props(new StepParent(fakeNotifier(InsufficientOrdersFive), testActor)), "unsuccessful")

      expectMsg(Notifier.Done(result = false))
      watch(notifier)
      expectTerminated(notifier)
    }

    "properly finish in case of error" in {
      val notifier = system.actorOf(Props(new StepParent(fakeNotifier(OrdersLessThanPredicted), testActor)), "error")

      watch(notifier)
      expectTerminated(notifier)
    }
  }
}
