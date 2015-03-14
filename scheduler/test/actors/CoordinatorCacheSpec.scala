package actors

import com.cave.metrics.data._
import init.AwsWrapper.WorkItem
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.specs2.matcher.ShouldMatchers
import play.api.libs.json.Json

import scala.util.{Failure, Success}

class CoordinatorCacheSpec extends FlatSpec with ShouldMatchers with MockitoSugar with BeforeAndAfter with AlertJsonData {

  val mockDataManager = mock[CacheDataManager]
  val mockListener = mock[CoordinatorCacheListener]

  val ItemId = "12345"
  val ReceiptHandle = "RecHandle12345"
  val NewUrl = "http://new.url/"

  before {
    Mockito.reset(mockDataManager, mockListener)
  }

  "CoordinatorCache" should "create an empty list for a new org" in {

    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map.empty[String, List[Schedule]]))
    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(0)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Organization, Operation.Create, OrgName, "")))
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "log a warning if it receives a create event for known organization" in {
    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Organization, Operation.Create, OrgName, "")))
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "send new url to listener when org updated" in {

    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Organization, Operation.Update, OrgName, NewUrl)))
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).notifyUrlChange(SomeId, NewUrl)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "log a warning if it receives an update event for unknown organization" in {

    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map.empty[String, List[Schedule]]))
    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(0)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Organization, Operation.Update, OrgName, NotificationUrl)))
    cache.schedulesByOrganization should haveSize(0)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "delete all alerts when their org is deleted" in {
    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    val OtherId = "4321"
    val OtherSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(OtherId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule, OtherSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(2)
    verify(mockListener).createScheduler(SomeSchedule)
    verify(mockListener).createScheduler(OtherSchedule)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Organization, Operation.Delete, OrgName, "")))
    cache.schedulesByOrganization should haveSize(0)
    verify(mockListener).stopScheduler(SomeId)
    verify(mockListener).stopScheduler(OtherId)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "log a warning if it receives a delete event for unknown organization" in {

    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map.empty[String, List[Schedule]]))
    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(0)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Organization, Operation.Delete, OrgName, "")))
    cache.schedulesByOrganization should haveSize(0)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }


  it should "add a new alert when it receives a create alert event and alert is enabled" in {

    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    val OtherId = "4321"
    val OtherSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(OtherId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Create, OtherId, Json.stringify(Json.toJson(OtherSchedule)))))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(2)
    verify(mockListener).createScheduler(OtherSchedule)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)

  }

  it should "ignore a new alert when it receives a create alert event and alert is disabled" in {

    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List())))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)

    val OtherId = "4321"
    val OtherSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(OtherId), AlertDescription, AlertDisabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Create, OtherId, Json.stringify(Json.toJson(OtherSchedule)))))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)

  }

  it should "log a warning and skip a duplicate create alert event" in {
    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    val OtherSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), OtherAlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Create, SomeId, Json.stringify(Json.toJson(OtherSchedule)))))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)

  }


  it should "stop and recreate a scheduler when an alert is updated" in {

    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    val NewSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), OtherAlertDescription, OtherAlertEnabled, OtherAlertPeriod, OtherAlertCondition, Some(AlertHandbookUrl), Some(OtherAlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Update, SomeId, Json.stringify(Json.toJson(NewSchedule)))))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).stopScheduler(SomeId)
    verify(mockListener).createScheduler(NewSchedule)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "stop but not recreate a scheduler when an alert is updated and disabled" in {

    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    val NewSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), OtherAlertDescription, AlertDisabled, OtherAlertPeriod, OtherAlertCondition, Some(OtherAlertHandbookUrl), Some(OtherAlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Update, SomeId, Json.stringify(Json.toJson(NewSchedule)))))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)
    verify(mockListener).stopScheduler(SomeId)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "log a warning but skip an alert update that can't be parsed" in {
    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    val NewSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), OtherAlertDescription, OtherAlertEnabled, OtherAlertPeriod, OtherAlertCondition, Some(OtherAlertHandbookUrl), Some(OtherAlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Update, SomeId, "Boom!")))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "create an alert when it receives an update for a previously disabled alert" in {
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List())))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)

    val OtherId = "4321"
    val OtherSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(OtherId), OtherAlertDescription, OtherAlertEnabled, OtherAlertPeriod, OtherAlertCondition, Some(OtherAlertHandbookUrl), Some(OtherAlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Update, OtherId, Json.stringify(Json.toJson(OtherSchedule)))))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(OtherSchedule)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)

  }

  it should "ignore an alert when it receives an update for a previously disabled alert that remains disabled" in {
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List())))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)

    val OtherId = "4321"
    val OtherSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(OtherId), OtherAlertDescription, AlertDisabled, OtherAlertPeriod, OtherAlertCondition, Some(OtherAlertHandbookUrl), Some(OtherAlertRouting)))
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Update, OtherId, Json.stringify(Json.toJson(OtherSchedule)))))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)

  }

  it should "stop the scheduler when an alert is deleted" in {
    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Delete, SomeId, OrgName)))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)
    verify(mockListener).stopScheduler(SomeId)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "log a warning but skip an alert delete for an unknown alert id" in {
    val SomeId = "1234"
    val SomeSchedule = Schedule(OrgName, Some(TeamName), None, NotificationUrl, Alert(Some(SomeId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting)))
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map(OrgName -> List(SomeSchedule))))

    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(1)
    verify(mockListener).createScheduler(SomeSchedule)

    val OtherId = "4321"
    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Alert, Operation.Delete, OtherId, OrgName)))

    cache.schedulesByOrganization should haveSize(1)
    cache.schedulesByOrganization(OrgName) should haveSize(0)

    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

  it should "throw exception if it cannot read the initial list from database" in {
    when(mockDataManager.getEnabledAlerts()).thenReturn(Failure(new RuntimeException("Boom!")))
    intercept[RuntimeException] {
      new CoordinatorCache(mockDataManager, mockListener)
    }
  }

  it should "log a message but skip an update it doesn't support" in {
    when(mockDataManager.getEnabledAlerts()).thenReturn(Success(Map.empty[String, List[Schedule]]))
    val cache = new CoordinatorCache(mockDataManager, mockListener)
    cache.schedulesByOrganization should haveSize(0)

    cache.updateCache(WorkItem(ItemId, ReceiptHandle, Update(Entity.Team, Operation.Create, OrgName, "")))
    cache.schedulesByOrganization should haveSize(0)
    verify(mockListener).deleteMessage(ReceiptHandle)

    verifyNoMoreInteractions(mockListener)
  }

}
