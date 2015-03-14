package com.cave.metrics.data.postgresql

import java.util.UUID

import com.cave.metrics.data._
import com.cave.metrics.data.postgresql.Tables._
import org.scalatest.BeforeAndAfter

import scala.slick.driver.H2Driver.simple._
import scala.util.{Failure, Success, Try}

class DataManagerSpec extends AbstractDataManagerSpec with BeforeAndAfter {

  var dm: PostgresCacheDataManagerImpl = _

  before {
    dm = new PostgresCacheDataManagerImpl(awsConfig)
  }

  "Postgres Data Manager" should "be able to update an alert for organization" in {
    val condition = "condition==true"
    val alert = Alert(None, "ALERT_TO_UPDATE" + UUID.randomUUID().toString, true, "15m", condition, None, None)
    val originalAlert = dm.createOrganizationAlert(dm.getOrganization(GiltOrgName).get.get, alert, Set()).get.get
    val alertId = originalAlert.id.get
    val updatedDescription = "UPDATED"
    val updatedPeriod = "1h"
    val updatedHandbookUrl = "https://new.url.com/alert"
    val alertToUpdate = AlertPatch(Some(updatedDescription), Some(false), Some(updatedPeriod), Some(updatedHandbookUrl), None)
    dm.updateAlert(originalAlert, alertToUpdate) match {
      case Success(Some(alert)) => {
        alert.id.get should be(alertId)
        alert.description should be(updatedDescription)
        alert.enabled should be(false)
        alert.period should be(updatedPeriod)
        alert.condition should be(condition)
        alert.handbookUrl should be(Some(updatedHandbookUrl))
        alert.routing should be(None)
      }
      case Failure(e) => sys.error(s"unable to update alert: $e")
      case _ => sys.error("unable to update alert")
    }

    dm.getAlert(alertId) match {
      case Success(Some(updatedAlert)) => {
        updatedAlert.id.get should be(alertId)
        updatedAlert.description should be(updatedDescription)
        updatedAlert.enabled should be(false)
        updatedAlert.period should be(updatedPeriod)
        updatedAlert.condition should be(condition)
        updatedAlert.handbookUrl should be(Some(updatedHandbookUrl))
        updatedAlert.routing should be(None)
      }
      case _ => sys.error("unable find alert")
    }

  }

  it should "be able to fetch all teams for a given org" in {
    val giltOrg = Organization(Some(GiltOrganizationId.toString), GiltOrgName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    dm.getTeams(giltOrg) match {
      case Success(seq) => {
        seq.length should be(2)
        seq foreach (t => {
          t.tokens.get.length should be(1)
        })
      }
      case _ => sys.error("unable to add a new token")
    }
  }

  it should "be able to add a new token to an organization" in {
    val organization: Try[Option[Organization]] = dm.getOrganization(GiltOrgName)

    dm.addOrganizationToken(organization.get.get, Token.createToken(GiltOrgTokenDescription)) match {
      case Success(token) => {
        assert(token.id.nonEmpty)
        token.description should be(GiltOrgTokenDescription)
        dm.getOrganization(GiltOrgName).get.get.tokens.get.length - 1 should be(organization.get.get.tokens.get.length)
        // cleanup for other tests
        dm.deleteToken(token.id.get)
      }
      case _ => sys.error("unable to add a new token")
    }
  }

  it should "be able to add a new token to a team" in {
    val organization = dm.getOrganization(GiltOrgName).get.get
    val team = dm.getTeam(organization, testTeamName).get.get
    dm.addTeamToken(organization, team, Token.createToken(GiltOrgTokenDescription)) match {
      case Success(token) => {
        token.description should be(GiltOrgTokenDescription)
        assert(token.id.nonEmpty)
        dm.getTeam(organization, testTeamName).get.get.tokens.get.length - 1 should be(team.tokens.get.length)
        // cleanup for other tests
        dm.deleteToken(token.id.get)
      }
      case _ => sys.error("unable to add a new token")
    }
  }

  it should "update delete_at column for that token" in {
    dm.getOrganization(GiltOrgName) match {
      case Success(Some(org)) => {
        org.id.get should be(GiltOrganizationId.toString)
        org.tokens.get.length should be(3)
      }
      case _ => fail("Organization not found")
    }
    val uuid = UUID.randomUUID()
    val timestamp = new java.sql.Timestamp(System.currentTimeMillis())
    val tokenId = (tokensTable returning tokensTable.map(_.id)) += TokensRow(1, GiltOrganizationId, None, "TOKEN_TO_BE_REMOVED", "TOKEN_TO_BE_REMOVED_VALUE", uuid, timestamp, uuid, timestamp, None, None)
    dm.deleteToken(tokenId.toString).get should be(true)
    dm.deleteToken(tokenId.toString).get should be(false)
    dm.getOrganization(GiltOrgName) match {
      case Success(Some(org)) => {
        org.id.get should be(GiltOrganizationId.toString)
        org.tokens.get.foreach(t => assert(t.id != tokenId.toString))
        org.tokens.get.length should be(3)
      }
      case _ => fail("Organization not found")
    }
  }

  "join query on orgs and teams" should "return 3 tokens for test org" in {
    dm.getOrganization("test-org") match {
      case Success(Some(org)) => {
        org.tokens.get.length should be(3)
        org.email should be(GiltEmail)
        org.name should be(GiltOrgName)
        org.notificationUrl should be(GiltNotificationUrl)
      }
      case _ => fail("Organization not found")
    }
  }

  "an attempt to create the same organization twice" should "fail and return None" in {
    val GiltOrg = Organization(None, "TEST:" + UUID.randomUUID().toString, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))

    dm.createOrganization(User1, GiltOrg) match {
      case Success(Some(org)) => {
        org.tokens.get.length should be(GiltOrg.tokens.get.length)
        org.email should be(GiltOrg.email)
        org.name should be(GiltOrg.name)
        org.notificationUrl should be(GiltOrg.notificationUrl)
      }
      case Success(None) => fail(s"Unable to create organization")
      case Failure(e) => fail(s"Unable to create organization ${e.getMessage}")
    }
    dm.createOrganization(User2, GiltOrg) match {
      case Success(None) => println("ok")
      case Failure(e) => fail(s"Unable to create organization $e")
      case _ => fail("unexpected error")
    }

    dm.deleteOrganization(GiltOrg.name)
  }

  "create a new organization and fetch it from db" should "work" in {
    val GiltOrg = Organization(None, "TEST:" + UUID.randomUUID().toString, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))

    dm.createOrganization(User1, GiltOrg) match {
      case Success(Some(org)) => {
        org.tokens.get.length should be(GiltOrg.tokens.get.length)
        org.email should be(GiltOrg.email)
        org.name should be(GiltOrg.name)
        org.notificationUrl should be(GiltOrg.notificationUrl)
      }
      case Success(None) => fail(s"Unable to create organization")
      case Failure(e) => fail(s"Unable to create organization ${e.getMessage}")
    }
    dm.getOrganization(GiltOrg.name) match {
      case Success(Some(org)) => {
        org.tokens.get.length should be(GiltOrg.tokens.get.length)
        org.email should be(GiltOrg.email)
        org.name should be(GiltOrg.name)
        org.notificationUrl should be(GiltOrg.notificationUrl)
      }
      case _ => fail("Organization not found")
    }

  }

  "Postgres Data Manager" should "be able to fetch a team from DB" in {
    val giltOrg = Organization(Some(GiltOrganizationId.toString), GiltOrgName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    dm.getTeam(giltOrg, testTeamName) match {
      case Success(Some(team)) => {
        team.name should be(testTeamName)
        team.tokens.get.length should be(1)
      }
      case _ => fail("unable to find team 'twain'")
    }
  }

  it should "be able to create an alert for an organization" in {
    val giltOrg = Organization(Some(GiltOrganizationId.toString), GiltOrgName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    val alert = Alert(None, "my_alert=" + UUID.randomUUID().toString, true, "15m", "condition=true", None, None)
    dm.createOrganizationAlert(giltOrg, alert, Set()) match {
      case Success(Some(alert)) => {
        alert.description should be(alert.description)
        assert(alert.id.nonEmpty)
        dm.getAlert(alert.id.get.toString) match {
          case Success(Some(foundAlert)) => {
            foundAlert.description should be(foundAlert.description)
            foundAlert.id should be(alert.id)
          }
          case _ => fail("unable to find a new foundAlert in DB")
        }
      }
      case _ => fail("unable to insert a new alert to DB")
    }
  }

  it should "be able to create an alert for a team" in {
    val giltOrg = Organization(Some(GiltOrganizationId.toString), GiltOrgName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    val alert = Alert(None, "my_alert=" + UUID.randomUUID().toString, true, "15m", "condition=true", None, None)
    val team = dm.getTeam(giltOrg, testTeamName).get.get
    dm.createTeamAlert(giltOrg, team, alert, Set()) match {
      case Success(Some(alert)) => {
        alert.description should be(alert.description)
        assert(alert.id.nonEmpty)
        dm.getAlert(alert.id.get.toString) match {
          case Success(Some(foundAlert)) => {
            foundAlert.description should be(foundAlert.description)
            foundAlert.id should be(alert.id)
          }
          case _ => fail("unable to find a new foundAlert in DB")
        }
      }
      case _ => fail("unable to insert a new alert to DB")
    }
  }

  it should "be able to create a team for organization" in {
    val giltOrg = Organization(Some(GiltOrganizationId.toString), GiltOrgName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    val team = new Team(None, "testTEAM" + UUID.randomUUID(), Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    dm.createTeam(giltOrg, team) match {
      case Success(Some(t)) => {
        t.name should be(team.name)
        t.tokens.get.length should be(team.tokens.get.length)
      }
      case Failure(e) => fail(s"unable to create a team: ${e.getMessage}")
      case _ => fail("unable to create a team")
    }

    dm.createTeam(giltOrg, team) match {
      case Success(None) => println("ok")
      case _ => fail("expected None")
    }
  }

  it should "not let delete an organization which has teams" in {
    val giltOrg = Organization(Some(GiltOrganizationId.toString), GiltOrgName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken1, GiltOrgToken2, GiltOrgToken3)))
    val team = dm.getTeam(giltOrg, testTeamName).get.get
    assert(team.id.nonEmpty)

    dm.deleteOrganization(GiltOrgName) match {
      case Failure(e) => {
        println(e.getMessage)
      }
      case Success(status) => fail(s"Organization was deleted but not supposed to: ${status}")
      case _ => fail("Organization was deleted but not supposed to")
    }
  }

  it should "be return a correct status when trying to remove non-existing team" in {
    val organization = dm.getOrganization(GiltOrgName).get.get

    dm.deleteTeam(organization, "NON-EXISTING TEAM") match {
      case Success(true) => fail(s"team doesn't exit but got Success(true)")
      case Success(false) => println("OK, Team has not been deleted")
      case Failure(e) => fail(s"Error ${e.getMessage}")
    }
  }

  it should "be return a correct status when trying to remove an existing team" in {
    val organization = dm.getOrganization(GiltOrgName).get.get
    dm.deleteTeam(organization, testTeamName) match {
      case Success(true) => println("OK, Team has been deleted")
      case Success(false) => fail(s"team exits but returen Success(false)")
      case Failure(e) => fail(s"Error ${e.getMessage}")
    }
  }

  it should "be able to update organization entity" in {
    val org = dm.getOrganization(GiltOrgName).get.get
    val numberOfExistingTeams = dm.getTeams(org).get.length

    val updatedEmail = "updatedEmail"
    val updatedOrgNotificationUrl = "updatedNotificationUrl"
    val patchOrg = OrganizationPatch(Some(updatedEmail), Some(updatedOrgNotificationUrl))


    dm.updateOrganization(org, patchOrg) match {
      case Success(Some(orgUpdated)) => {
        orgUpdated.tokens.get.length should be(3)
        orgUpdated.email should be(updatedEmail)
        orgUpdated.name should be(org.name)
        orgUpdated.notificationUrl should be(updatedOrgNotificationUrl)
        orgUpdated.id.get should be(org.id.get)
      }
      case _ => fail("Organization update failed")
    }

    dm.getOrganization(org.name) match {
      case Success(Some(orgUpdated)) => {
        orgUpdated.tokens.get.length should be(3)
        orgUpdated.email should be(updatedEmail)
        orgUpdated.name should be(org.name)
        orgUpdated.notificationUrl should be(updatedOrgNotificationUrl)
        orgUpdated.id.get should be(org.id.get)
      }
      case _ => fail("Organization update failed")
    }

    dm.getTeams(dm.getOrganization(org.name).get.get).get.length should be(numberOfExistingTeams)
  }

  it should "be able to delete an alert" in {
    val orgName = "My_test_org1"
    createOrganization(orgName)

    val condition = "true"
    val period = "15m"
    val desc = "ALERT_TO_UPDATE" + UUID.randomUUID().toString
    val status = true

    val alert = Alert(None, desc, status, period, condition, None, None)
    val organization = dm.getOrganization(orgName).get.get
    val alertId = dm.createOrganizationAlert(organization, alert, Set()).get.get.id.get

    dm.deleteAlert(alertId) match {
      case Success(status) => {
        dm.getAlert(alertId) match {
          case Success(Some(alert)) => {
            fail("found deleted alert")
          }
          case Success(None) => println("ok")
          case _ => sys.error("unable to find an alert")
        }
      }
      case _ => fail("alert not deleted")
    }
  }

  "Pagination" should "work for Organization alerts" in {
    val orgName = "My_test_org2"
    createOrganization(orgName)

    val condition = "true"
    val period = "15m"
    val desc = "ALERT_TO_UPDATE" + UUID.randomUUID().toString
    val status = true

    val alert = Alert(None, desc, status, period, condition, None, None)
    val organization = dm.getOrganization(orgName).get.get

    dm.getOrganizationAlerts(organization) should be(Success(List.empty[Alert]))

    val newAlertId = dm.createOrganizationAlert(organization, alert, Set()).get.get.id.get

    dm.getOrganizationAlerts(organization) match {
      case Success(alerts) => {
        alerts.length should be(1)
        alerts.foreach(a => {
          a.id.get should be(newAlertId)
          a.condition should be(condition)
          a.description should be(desc)
          a.period should be(period)
          a.enabled should be(status)
        })
      }
      case Failure(_) => fail("unable to find an alert")
    }

    dm.getOrganizationAlerts(organization, 0) match {
      case Success(alerts) => {
        alerts.size should be(0)
      }
      case Failure(_) => fail("unable to find an alert")
    }

    dm.getOrganizationAlerts(organization, 1, 1) match {
      case Success(alerts) => {
        alerts.length should be(0)
      }
      case Failure(_) => fail("unable to find an alert")
    }

    for (i <- 1 to 30)
      dm.createOrganizationAlert(organization, alert, Set())

    dm.getOrganizationAlerts(organization, limit = 50) match {
      case Success(alerts) => {
        alerts.length should be(31)
      }
      case Failure(_) => fail("unable to find an alert")
    }

    dm.getOrganizationAlerts(organization, limit = 10, offset = 15) match {
      case Success(alerts) => {
        alerts.head.id.get should be("20")
        alerts.last.id.get should be("29")
        alerts.length should be(10)
      }
      case Failure(_) => fail("unable to find an alert")
    }
  }

  it should "work for team alerts" in {
    val orgName = "team-alert-pagination"
    val teamName = "test-team"

    val orgId = createOrganization(orgName)
    createTeam(teamName, orgId)


    val organization = dm.getOrganization(orgName).get.get
    val team = dm.getTeam(organization, teamName).get.get

    for (i <- 1 to 9) dm.createTeamAlert(organization, team, Alert(None, s"alert=$i", true, "666m", "condition=true", None, None), Set())

    val lastAlertId = dm.createTeamAlert(organization, team, Alert(None, s"alert=10", true, "666m", "condition=true", None, None), Set()).get.get.id.get

    dm.getTeamAlerts(organization, team, 20, 0) match {
      case Success(alerts) => {
        alerts.size should be(10)
        alerts foreach { a => a.period should be("666m")}
      }
      case _ => fail("unable to find alerts")
    }

    dm.getTeamAlerts(organization, team, 5, 0) match {
      case Success(alerts) => {
        alerts.size should be(5)
        alerts foreach { a => a.period should be("666m")}
      }
      case _ => fail("unable to find alerts")
    }


    dm.getTeamAlerts(organization, team, 150000, 0) match {
      case Success(alerts) => {
        alerts.size should be(10)
        alerts foreach { a => a.period should be("666m")}
        alerts.last.id.get should be(lastAlertId)
      }
      case _ => fail("unable to find alerts")
    }

    dm.getTeamAlerts(organization, team, 15, 9) match {
      case Success(alerts) => {
        alerts.size should be(1)
        alerts foreach { a => a.period should be("666m")}
        alerts.last.id.get should be(lastAlertId)
      }
      case _ => fail("unable to find alerts")
    }
  }

  "Data manager" should "be able to delete an organization with no teams" in {
    assert(dm.isHealthy)

    val orgName = "org-to-delete"
    val teamName = "test-team5"
    val orgId = createOrganization(orgName)
    createTeam(teamName, orgId)
    val organization = dm.getOrganization(orgName).get.get
    val team = dm.getTeam(organization, teamName).get.get
    for (i <- 1 to 9) dm.createOrganizationAlert(organization, Alert(None, s"alert=$i", true, "666m", "condition=true", None, None), Set())

    dm.deleteOrganization(orgName) match {
      case Failure(e) => {
        println(e.getMessage)
      }
      case Success(status) => fail(s"Organization was deleted but not supposed to: ${status}")
      case _ => fail("Organization was deleted but not supposed to")
    }

    dm.deleteTeam(organization, teamName) match {
      case Success(true) => println("OK, Team has been deleted")
      case Success(false) => fail(s"team exists but returns Success(false)")
      case Failure(e) => fail(s"Error ${e.getMessage}")
    }

    dm.deleteOrganization(orgName) match {
      case Success(true) => println(s"Organization was deleted successfully")
      case _ => fail("Organization was NOT deleted")
    }

    dm.getOrganization(orgName) match {
      case Success(None) => println(s"Organization not found")
      case _ => fail("Organization found after being deleted")
    }
  }

  it should "handle all edge cases" in {
    val orgName = "org-to-test2"
    val teamName = "test-team6"
    val orgId = createOrganization(orgName)
    createTeam(teamName, orgId)
    val organization = dm.getOrganization(orgName).get.get
    val team = dm.getTeam(organization, teamName).get.get
    for (i <- 1 to 9) dm.createOrganizationAlert(organization, Alert(None, s"alert=$i", true, "666m", "condition=true", None, None), Set())
    val alert = Alert(Some(444.toString), s"alert", true, "666m", "condition=true", None, None)
    val alertToUpdate = AlertPatch(Some("alert"), Some(true), Some("666m"), None, None)

    dm.updateAlert(alert, alertToUpdate) match {
      case Success(None) => println("ok")
      case _ => fail("updated non-existing alert")
    }

    dm.deleteAlert("non-existing id") match {
      case Success(false) => println("ok")
      case _ => fail("deleted non-existing team")
    }

    dm.deleteAlert("1234") match {
      case Success(false) => println("ok")
      case _ => fail("deleted non-existing team")
    }

    dm.deleteTeam(organization, "i don't exist") match {
      case Success(false) => println("ok")
      case _ => fail("deleted non-existing team")
    }
  }
}
