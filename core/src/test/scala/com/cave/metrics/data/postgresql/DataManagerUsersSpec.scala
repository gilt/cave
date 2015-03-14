package com.cave.metrics.data.postgresql

import java.util.UUID

import com.cave.metrics.data._
import org.joda.time.format.ISODateTimeFormat
import org.scalatest.BeforeAndAfter

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DataManagerUsersSpec extends AbstractDataManagerSpec with BeforeAndAfter {

  var dm: PostgresDataManagerImpl = _
  val timeout = 5.seconds
  var GiltOrg: Organization = _
  var SecondOrg: Organization = _

  var TestTeam: Team = _
  var TestTeam2: Team = _

  before {
    dm = new PostgresDataManagerImpl(awsConfig)
    GiltOrg = dm.getOrganization(GiltOrgName).get.get
    SecondOrg = dm.getOrganization(SecondOrgName).get.get
    TestTeam = dm.getTeam(GiltOrg, testTeamName).get.get
    TestTeam2 = dm.getTeam(GiltOrg, testTeamName2).get.get
  }

  "Web Data Manager" should "create a new user" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    user.id.isDefined should be(true)
    verifyUser(User1, user)
    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "update an existing user" in {
    val user = Await.result(dm.createUser(User1), timeout).get

    val updatedUser1 = Await.result(dm.updateUser(user, Some(First2), None, None), timeout).get
    verifyUser(User(user.id, First2, Last1, Email1, Password1, Salt1), updatedUser1)

    val updatedUser2 = Await.result(dm.updateUser(updatedUser1, None, Some(Last2), None), timeout).get
    verifyUser(User(user.id, First2, Last2, Email1, Password1, Salt1), updatedUser2)

    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "fail to update a non-existent user" in {
    val user = User(Some(1000L), First1, Last1, Email1, Password1, Salt1)
    Await.result(dm.updateUser(user, Some(First2), None, None), timeout) should be(None)
  }

  it should "find some users" in {
    val user1 = Await.result(dm.createUser(User(None, "John", "Appleseed", "john.appleseed@gmail.com", "hash", None)), timeout).get
    val user2 = Await.result(dm.createUser(User(None, "Dave", "Bumblebee", "dave.bumblebee@gmail.com", "hash", None)), timeout).get
    val user3 = Await.result(dm.createUser(User(None, "Stan", "Cornholer", "stan.cornholer@gmail.com", "hash", None)), timeout).get

    verifyLists(Await.result(dm.findUser("apples"), timeout), List(user1))
    verifyLists(Await.result(dm.findUser("ve.bu"), timeout), List(user2))
    verifyLists(Await.result(dm.findUser("stan"), timeout), List(user3))
    verifyLists(Await.result(dm.findUser("EE"), timeout), List(user1, user2))
    verifyLists(Await.result(dm.findUser("GMail"), timeout), List(user1, user2, user3))
  }

  val SOME_UUID = UUID.randomUUID().toString
  val SOME_EMAIL = "user@domain.com"
  val SOME_DATETIME = ISODateTimeFormat.dateTimeNoMillis().parseDateTime("2014-10-02T13:00:00Z")
  val LATER_DATETIME = ISODateTimeFormat.dateTimeNoMillis().parseDateTime("2014-10-02T14:00:00Z")

  val EXPIRATION1 = ISODateTimeFormat.dateTimeNoMillis().parseDateTime("2014-10-02T13:59:00Z")
  val EXPIRATION2 = ISODateTimeFormat.dateTimeNoMillis().parseDateTime("2014-10-02T14:01:00Z")

  val SOME_TOKEN = ConfirmationToken(None, SOME_UUID, SOME_EMAIL, SOME_DATETIME, LATER_DATETIME, isSignUp = true)

  it should "create a confirmation token" in {
    validateToken(Await.result(dm.createConfirmationToken(SOME_TOKEN), timeout).get)
    Await.result(dm.deleteConfirmationToken(SOME_UUID), timeout)
  }

  it should "delete a confirmation token by uuid" in {
    Await.result(dm.createConfirmationToken(SOME_TOKEN), timeout)
    val token = Await.result(dm.deleteConfirmationToken(SOME_UUID), timeout).get
    validateToken(token)
  }

  it should "fail to delete non-existent token" in {
    if (Await.result(dm.deleteConfirmationToken(SOME_UUID), timeout).isDefined)
      fail("We expected a failure here.")
  }

  it should "return None if requested token doesn't exist" in {
    if (Await.result(dm.getConfirmationTokenByUUID(SOME_UUID), timeout).isDefined)
      fail("We expected token to be not found.")
  }

  it should "return a token if it exists" in {
    Await.result(dm.createConfirmationToken(SOME_TOKEN), timeout)
    val token = Await.result(dm.getConfirmationTokenByUUID(SOME_UUID), timeout).get
    validateToken(token)
    Await.result(dm.deleteConfirmationToken(SOME_UUID), timeout)
  }

  private[this] def validateToken(token: ConfirmationToken) = {
    token.id.isDefined should be (true)
    token.id.get should not be 0

    token.uuid should be (SOME_UUID)
    token.email should be(SOME_EMAIL)
    token.creationTime should be(SOME_DATETIME)
    token.expirationTime should be(LATER_DATETIME)
    token.isSignUp should be(true)
  }

  it should "not remove active tokens" in {
    Await.result(dm.createConfirmationToken(SOME_TOKEN), timeout)
    Await.result(dm.deleteExpiredConfirmationTokens(EXPIRATION1), timeout)

    Await.result(dm.getConfirmationTokenByUUID(SOME_UUID), timeout) match {
      case Some(token) => validateToken(token)
      case None => fail("Expected token to still exist!")
    }
    Await.result(dm.deleteConfirmationToken(SOME_UUID), timeout)
  }

  it should "remove expired tokens" in {
    Await.result(dm.createConfirmationToken(SOME_TOKEN), timeout)
    Await.result(dm.deleteExpiredConfirmationTokens(EXPIRATION2), timeout)

    Await.result(dm.getConfirmationTokenByUUID(SOME_UUID), timeout) match {
      case Some(token) => fail("Expected token to be deleted!")
      case None => // OK
    }
  }

  it should "create session token for a user" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    val token = Await.result(dm.createSessionToken(user.id.get, SOME_DATETIME, LATER_DATETIME), timeout)
    token.token.size > 0 should be(true)
    token.creationTime should be(SOME_DATETIME)
    token.expirationTime should be(LATER_DATETIME)

    Await.result(dm.deleteSessionToken(token.id.get), timeout)
    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "find user by token" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    val token = Await.result(dm.createSessionToken(user.id.get, SOME_DATETIME, LATER_DATETIME), timeout)

    val userFound = Await.result(dm.findUserByToken(token.token, EXPIRATION1), timeout).get
    userFound should be (user)

    Await.result(dm.deleteSessionToken(token.id.get), timeout)
    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "not remove active session tokens" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    val token = Await.result(dm.createSessionToken(user.id.get, SOME_DATETIME, LATER_DATETIME), timeout)

    Await.result(dm.deleteExpiredSessionTokens(EXPIRATION1), timeout)

    Await.result(dm.findUserByToken(token.token, EXPIRATION1), timeout) match {
      case Some(user) => // OK
      case None => fail("Expected token to still exist")
    }

    Await.result(dm.deleteSessionToken(token.id.get), timeout)
    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "remove expired session tokens" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    val token = Await.result(dm.createSessionToken(user.id.get, SOME_DATETIME, LATER_DATETIME), timeout)

    Await.result(dm.deleteExpiredSessionTokens(EXPIRATION2), timeout)

    Await.result(dm.findUserByToken(token.token, EXPIRATION1), timeout) match {
      case Some(user) => fail("Expected token to be deleted")
      case None =>
    }

    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "return no organizations or teams for new user" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    val list = Await.result(dm.getOrganizationsForUser(user), timeout)
    list.size should be (0)

    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "add users to organizations and return them later" in {
    val user1 = Await.result(dm.createUser(User1), timeout).get
    val user2 = Await.result(dm.createUser(User2), timeout).get

    Await.result(dm.addUserToOrganization(user1, GiltOrg, Role.Admin), timeout)
    Await.result(dm.addUserToOrganization(user2, GiltOrg, Role.Member), timeout)
    Await.result(dm.addUserToOrganization(user1, SecondOrg, Role.Member), timeout)

    verifyOrganizationsList(Await.result(dm.getOrganizationsForUser(user1), timeout), List(GiltOrgName), List(SecondOrgName))

    verifyUsersList(Await.result(dm.getUsersForOrganization(GiltOrg), timeout), List(user1), List(user2))
    verifyUsersList(Await.result(dm.getUsersForOrganization(SecondOrg), timeout), List(), List(user1))

    Await.result(dm.deleteUser(user1.id.get), timeout)
    Await.result(dm.deleteUser(user2.id.get), timeout)
  }

  it should "add users to teams and return them later" in {
    val user1 = Await.result(dm.createUser(User1), timeout).get
    val user2 = Await.result(dm.createUser(User2), timeout).get

    Await.result(dm.addUserToTeam(user1, TestTeam, Role.Admin), timeout)
    Await.result(dm.addUserToTeam(user2, TestTeam, Role.Member), timeout)
    Await.result(dm.addUserToTeam(user1, TestTeam2, Role.Member), timeout)

    verifyTeamsList(Await.result(dm.getTeamsForUser(GiltOrg, user1), timeout), List(testTeamName), List(testTeamName2))
    verifyUsersList(Await.result(dm.getUsersForTeam(TestTeam), timeout), List(user1), List(user2))
    verifyUsersList(Await.result(dm.getUsersForTeam(TestTeam2), timeout), List(), List(user1))

    Await.result(dm.deleteUser(user1.id.get), timeout)
    Await.result(dm.deleteUser(user2.id.get), timeout)
  }

  it should "return indirect organizations" in {
    val user1 = Await.result(dm.createUser(User1), timeout).get

    Await.result(dm.addUserToTeam(user1, TestTeam, Role.Admin), timeout)
    Await.result(dm.addUserToTeam(user1, TestTeam2, Role.Member), timeout)

    val orgList = Await.result(dm.getOrganizationsForUser(user1), timeout)


    orgList.size should be(1)
    orgList.head match {
      case (name, role) =>
        name should be(GiltOrgName)
        role should be(Role.Team)

      case _ => fail("Expected a pair of name and role")
    }

    Await.result(dm.deleteUser(user1.id.get), timeout)
  }

  it should "return only once the organizations that are both direct and indirect" in {
    val user1 = Await.result(dm.createUser(User1), timeout).get

    Await.result(dm.addUserToOrganization(user1, GiltOrg, Role.Member), timeout)
    Await.result(dm.addUserToTeam(user1, TestTeam, Role.Admin), timeout)
    Await.result(dm.addUserToTeam(user1, TestTeam2, Role.Member), timeout)

    val orgList = Await.result(dm.getOrganizationsForUser(user1), timeout)

    orgList.size should be(1)
    orgList.head match {
      case (name, role) =>
        name should be(GiltOrgName)
        role should be(Role.Member)

      case _ => fail("Expected a pair of name and role")
    }

    Await.result(dm.deleteUser(user1.id.get), timeout)
  }

  it should "change user role for organization or team" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    Await.result(dm.addUserToOrganization(user, GiltOrg, Role.Admin), timeout)
    Await.result(dm.addUserToOrganization(user, SecondOrg, Role.Member), timeout)
    Await.result(dm.addUserToTeam(user, TestTeam, Role.Admin), timeout)
    Await.result(dm.addUserToTeam(user, TestTeam2, Role.Member), timeout)

    verifyOrganizationsList(Await.result(dm.getOrganizationsForUser(user), timeout), List(GiltOrgName), List(SecondOrgName))
    verifyTeamsList(Await.result(dm.getTeamsForUser(GiltOrg, user), timeout), List(testTeamName), List(testTeamName2))

    Await.result(dm.changeOrganizationRole(user, GiltOrg, Role.Member), timeout)
    Await.result(dm.changeOrganizationRole(user, SecondOrg, Role.Admin), timeout)
    Await.result(dm.changeTeamRole(user, TestTeam, Role.Member), timeout)
    Await.result(dm.changeTeamRole(user, TestTeam2, Role.Admin), timeout)

    verifyOrganizationsList(Await.result(dm.getOrganizationsForUser(user), timeout), List(SecondOrgName), List(GiltOrgName))
    verifyTeamsList(Await.result(dm.getTeamsForUser(GiltOrg, user), timeout), List(testTeamName2), List(testTeamName))

    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  it should "remove users from organizations and teams" in {
    val user = Await.result(dm.createUser(User1), timeout).get
    Await.result(dm.addUserToOrganization(user, GiltOrg, Role.Admin), timeout)
    Await.result(dm.addUserToOrganization(user, SecondOrg, Role.Member), timeout)
    Await.result(dm.addUserToTeam(user, TestTeam, Role.Admin), timeout)
    Await.result(dm.addUserToTeam(user, TestTeam2, Role.Member), timeout)

    verifyOrganizationsList(Await.result(dm.getOrganizationsForUser(user), timeout), List(GiltOrgName), List(SecondOrgName))
    verifyTeamsList(Await.result(dm.getTeamsForUser(GiltOrg, user), timeout), List(testTeamName), List(testTeamName2))

    Await.result(dm.deleteUserFromOrganization(user, GiltOrg), timeout)
    Await.result(dm.deleteUserFromTeam(user, TestTeam2), timeout)

    verifyOrganizationsList(Await.result(dm.getOrganizationsForUser(user), timeout), List(), List(SecondOrgName))
    verifyTeamsList(Await.result(dm.getTeamsForUser(GiltOrg, user), timeout), List(testTeamName), List())

    Await.result(dm.deleteUserFromOrganization(user, SecondOrg), timeout)
    verifyOrganizationsList(Await.result(dm.getOrganizationsForUser(user), timeout), List(), List())

    Await.result(dm.deleteUserFromTeam(user, TestTeam), timeout)
    verifyTeamsList(Await.result(dm.getTeamsForUser(GiltOrg, user), timeout), List(), List())

    Await.result(dm.deleteUser(user.id.get), timeout)
  }

  private[this] def verifyUser(expected: User, actual: User): Unit = {
    actual.firstName should be(expected.firstName)
    actual.lastName should be(expected.lastName)
    actual.email should be(expected.email)
    actual.password should be(expected.password)
  }

  private[this] def verifyOrganizationsList(list: List[(String, Role)], admins: List[String], members: List[String]): Unit = {
    verifyLists(admins, list.filter(_._2.is(Role.Admin)).map(_._1))
    verifyLists(members, list.filter(_._2.is(Role.Member)).map(_._1))
  }
  private[this] def verifyTeamsList(list: List[(String, Role)], admins: List[String], members: List[String]): Unit = {
    verifyLists(admins, list.filter(_._2.is(Role.Admin)).map(_._1))
    verifyLists(members, list.filter(_._2.is(Role.Member)).map(_._1))
  }

  private[this] def verifyUsersList(list: List[(User, Role)], admins: List[User], members: List[User]): Unit = {
    verifyLists(admins, list.filter(_._2.is(Role.Admin)).map(_._1))
    verifyLists(members, list.filter(_._2.is(Role.Member)).map(_._1))
  }

  private[this] def verifyLists[T](expected: List[T], actual: List[T]): Unit = {
    actual.size should be(expected.size)
    expected foreach { item =>
      actual.contains(item) should be(true)
    }
  }
}
