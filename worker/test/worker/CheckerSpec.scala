package worker

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import com.cave.metrics.data.evaluator.DataFetcher
import com.cave.metrics.data.influxdb.InfluxClientFactory
import com.cave.metrics.data.{AlertJsonData, Check}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success}

class CheckerSpec extends TestKit(ActorSystem()) with WordSpecLike with BeforeAndAfterAll with AlertJsonData with MockitoSugar {

  override def afterAll() = {
    system.shutdown()
  }

  final val SomeReason = "BOOM!"
  val mockClientFactory = mock[InfluxClientFactory]

  def fakeChecker(check: Check): Props = Props(new Checker(check) {
    override def fetcher = new DataFetcher(mockClientFactory)

    override def run(check: Check)(implicit ec: ExecutionContext): Future[Try[Boolean]] = {
      if (check.schedule.alert.description == AlertDescription) Future.successful(Success(true))
      else if (check.schedule.alert.description == AlertFiveDescription) Future.successful(Success(false))
      else Future.failed(new RuntimeException(SomeReason))
    }
  })


  "A checker" must {
    "send Done(true) if an alarm condition has been detected" in {
      val checker = system.actorOf(Props(new StepParent(fakeChecker(InsufficientOrders), testActor)), "alarm")

      expectMsg(Checker.Done(alarm = Success(true)))
      watch(checker)
      expectTerminated(checker)
    }

    "send Done(false) if no alarm condition has been detected" in {
      val checker = system.actorOf(Props(new StepParent(fakeChecker(InsufficientOrdersFive), testActor)), "notAlarm")

      expectMsg(Checker.Done(alarm = Success(false)))
      watch(checker)
      expectTerminated(checker)
    }

    "properly finish in case of error" in {
      val checker = system.actorOf(Props(new StepParent(fakeChecker(OrdersLessThanPredicted), testActor)), "error")

      expectMsg(Checker.Aborted(SomeReason))
      watch(checker)
      expectTerminated(checker)
    }
  }
}
