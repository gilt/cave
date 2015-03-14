package actors

import actors.Scheduler.NotificationUrlChange
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestEvent.Mute
import akka.testkit.{EventFilter, TestKit}
import com.cave.metrics.data.{Alert, AlertJsonData, Check, Schedule}
import init.AwsWrapper
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}
import org.specs2.matcher.ShouldMatchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class SchedulerSpec extends TestKit(ActorSystem()) with WordSpecLike with ShouldMatchers with BeforeAndAfter with BeforeAndAfterAll with AlertJsonData with MockitoSugar {

  val mockAwsWrapper = mock[AwsWrapper]
  def mockLeader(imLeader: Boolean) = system.actorOf(Props(new Actor {
    def receive = {
      case Leadership.IsLeader =>
        println("Asked if I'm the leader, I'm about to respond with " + imLeader)
        sender ! imLeader
    }
  }))

  def mockSchedulerProps(schedule: Schedule, awsWrapper: AwsWrapper, imLeader: Boolean) = Props(new Scheduler(schedule, awsWrapper) {
    override def leader = mockLeader(imLeader)
  })

  val testPeriod = "1s"
  val schedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, testPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))

  val badPeriod = "78F"
  val badSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(None, AlertDescription, AlertEnabled, badPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))

  val newUrl = "http://over.there/new"

  override def afterAll() = {
    system.shutdown()
  }

  before {
    Mockito.reset(mockAwsWrapper)
  }

  "A scheduler" must {

    "emit events according to the schedule" in {

      val scheduler = system.actorOf(mockSchedulerProps(schedule, mockAwsWrapper, imLeader = true))

      // wait for 2 events to be emitted
      Thread.sleep(2000L)

      val captor = ArgumentCaptor.forClass(classOf[Check])
      verify(mockAwsWrapper, times(2)).sendMessage(captor.capture)(any[ExecutionContext])
      captor.getAllValues.asScala foreach { check =>
        check.schedule should be (schedule)
      }

      scheduler ! Scheduler.Die
      watch(scheduler)
      expectTerminated(scheduler)
    }

    "change the URL after receiving a NotificationUrlChanged event" in {
      val scheduler = system.actorOf(mockSchedulerProps(schedule, mockAwsWrapper, imLeader = true))

      scheduler ! NotificationUrlChange(newUrl)

      // wait for 1 event to be emitted
      Thread.sleep(2000L)

      val captor = ArgumentCaptor.forClass(classOf[Check])
      verify(mockAwsWrapper, times(2)).sendMessage(captor.capture)(any[ExecutionContext])
      captor.getAllValues.asScala foreach { check =>
        check.schedule.notificationUrl shouldEqual newUrl
      }

      scheduler ! Scheduler.Die
      watch(scheduler)
      expectTerminated(scheduler)
    }

    "shut down when it receives the Die object" in {
      val scheduler = system.actorOf(mockSchedulerProps(schedule, mockAwsWrapper, imLeader = true))
      Thread.sleep(1000L)
      scheduler ! Scheduler.Die
      watch(scheduler)
      expectTerminated(scheduler)
    }

    "shut down immediately if it cannot parse the period" in {
      system.eventStream.publish(Mute(EventFilter.error(pattern = "Unexpected alert period")))
      val scheduler = system.actorOf(mockSchedulerProps(badSchedule, mockAwsWrapper, imLeader = true))

      watch(scheduler)
      expectTerminated(scheduler)
    }

    "not emit events if not leader" in {

      val scheduler = system.actorOf(mockSchedulerProps(schedule, mockAwsWrapper, imLeader = false))

      // wait for 2 events to be emitted
      Thread.sleep(2000L)

      verify(mockAwsWrapper, never()).sendMessage(any[Check])(any[ExecutionContext])

      scheduler ! Scheduler.Die
      watch(scheduler)
      expectTerminated(scheduler)
    }

  }
}
