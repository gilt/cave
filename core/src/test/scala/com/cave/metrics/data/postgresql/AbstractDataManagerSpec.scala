package com.cave.metrics.data.postgresql

import java.util.UUID

import com.cave.metrics.data.postgresql.Tables._
import com.cave.metrics.data.{User, AwsConfig, Token}
import com.typesafe.config.ConfigFactory
import org.apache.commons.logging.LogFactory
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.TableQuery

abstract class AbstractDataManagerSpec extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterAll {

  val log = LogFactory.getLog(classOf[AbstractDataManagerSpec])

  val First1 = "First"
  val Last1 = "Last"
  val Email1 = "first.last@hotmail.com"
  val Password1 = "hash-hash-hash"
  val Salt1 = Some("12345")

  val First2 = "FirstFirst"
  val Last2 = "LastLast"
  val Email2 = "firstfirst.lastlast@hotmail.com"
  val Password2 = "hush-hush-hush"
  val Salt2 = Some("54321")

  val GiltEmail = "test@gilt.com"
  val GiltOrgName = "test-org"
  val GiltNotificationUrl = "https://notifications.gilt.com/alert"
  val GiltOrgTokenDescription = "test token"
  val GiltOrgToken1 = Token.createToken(GiltOrgTokenDescription)
  val GiltOrgToken2 = Token.createToken(GiltOrgTokenDescription)
  val GiltOrgToken3 = Token.createToken(GiltOrgTokenDescription)
  var GiltOrganizationId: Long = _
  var SecondOrganizationId: Long = _

  val testTeamName = "twain test Team"
  val testTeamName2 = "second test Team"
  var teamId: Long = _
  var team2Id: Long = _

  var User1: User = _
  var User2: User = _

  val SecondOrgName = GiltOrgName + "-second"

  lazy val organizationsTable = TableQuery[OrganizationsTable]
  lazy val tokensTable = TableQuery[TokensTable]
  lazy val alertsTable = TableQuery[AlertsTable]
  lazy val teamsTable = TableQuery[TeamsTable]
  lazy val queriesTable: TableQuery[QueriesTable] = TableQuery[QueriesTable]
  lazy val alert2queriesTable: TableQuery[AlertQueriesTable] = TableQuery[AlertQueriesTable]
  lazy val usersTable = TableQuery[UsersTable]

  val appConfig = ConfigFactory.load("test-inmemorydb.conf")
  val awsConfig: AwsConfig = new AwsConfig(appConfig)
  val database = Database.forURL(awsConfig.rdsJdbcDatabaseUrl, driver = awsConfig.rdsJdbcDatabaseClass)

  implicit val session: Session = database.createSession()

  override def beforeAll() = {
    log.info("Creating the DB schema...")
    createSchema()
    log.info("Populating the DB ...")
    populateDatabase()
  }

  override def afterAll() = {
    dropSchema()
    session.close()
  }

  private[postgresql] def createSchema() = Tables.createSchema()

  private[postgresql] def dropSchema() = Tables.dropSchema()

  private[postgresql] def populateDatabase() = {
    GiltOrganizationId = createOrganization(GiltOrgName)
    SecondOrganizationId = createOrganization(SecondOrgName)

    teamId = createTeam(testTeamName, GiltOrganizationId)
    team2Id = createTeam(testTeamName2, GiltOrganizationId)

    User1 = createUser(First1, Last1, Email1, Password1, Salt1)
    User2 = createUser(First2, Last2, Email2, Password2, Salt2)
  }

  private[postgresql] def createTeam(teamName: String, orgId: Long): Long = {
    val (uuid, timestamp) = (UUID.randomUUID(), new java.sql.Timestamp(System.currentTimeMillis()))
    val newTeamId2 = (teamsTable returning teamsTable.map(_.id)) += TeamsRow(1, orgId, teamName, None, uuid, timestamp, uuid, timestamp, None, None)
    tokensTable += TokensRow(1, GiltOrganizationId, Some(newTeamId2), "token for team twain 2 desc", "token_twain_value2", uuid, timestamp, uuid, timestamp, None, None)
    newTeamId2
  }

  private[postgresql] def createOrganization(orgName: String): Long = {
    val (uuid, timestamp) = (UUID.randomUUID(), new java.sql.Timestamp(System.currentTimeMillis()))
    val orgId = (organizationsTable returning organizationsTable.map(_.id)) += OrganizationsRow(1, orgName, GiltEmail, GiltNotificationUrl, None, uuid, timestamp, uuid, timestamp, None, None)
    for (i <- 1 to 3) tokensTable += TokensRow(1, orgId, None, s"token $i description for $orgName", s"token $i value for $orgName", uuid, timestamp, uuid, timestamp, None, None)
    tokensTable += TokensRow(1, orgId, None, "DELETED token 4 desc", "DELETED token_value4", uuid, timestamp, uuid, timestamp, Some(uuid), Some(timestamp))
    orgId
  }

  private[postgresql] def createUser(first: String, last: String, email: String, password: String, salt: Option[String]): User = {
    val id = (usersTable returning usersTable.map(_.id)) += UsersRow(1, first, last, email, password, salt)
    User(Some(id), first, last, email, password, salt)
  }
}
