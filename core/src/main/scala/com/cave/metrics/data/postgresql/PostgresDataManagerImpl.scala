package com.cave.metrics.data.postgresql

import java.sql.{SQLException, Timestamp}
import java.util.UUID

import com.cave.metrics.data._
import com.cave.metrics.data.postgresql.Tables._
import org.joda.time.DateTime
import org.postgresql.util.PSQLException

import scala.concurrent._
import scala.slick.driver.PostgresDriver
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.TableQuery
import scala.util.{Success, Try}

class PostgresDataManagerImpl(awsConfig: AwsConfig) extends DatabaseConnection(awsConfig) with DataManager {

  lazy val organizationsTable      = TableQuery[OrganizationsTable]
  lazy val tokensTable             = TableQuery[TokensTable]
  lazy val alertsTable             = TableQuery[AlertsTable]
  lazy val teamsTable              = TableQuery[TeamsTable]
  lazy val queriesTable            = TableQuery[QueriesTable]
  lazy val alert2queriesTable      = TableQuery[AlertQueriesTable]
  lazy val usersTable              = TableQuery[UsersTable]
  lazy val confirmationTokens      = TableQuery[ConfirmationTokensTable]
  lazy val organizationUsersTable  = TableQuery[OrganizationUsersTable]
  lazy val teamUsersTable          = TableQuery[TeamUsersTable]
  lazy val sessionTokensTable      = TableQuery[SessionTokensTable]
  lazy val statusTable             = TableQuery[StatusTable]

  val DEFAULT_MAX_PAGINATION_LIMIT = 5000
  private[postgresql] val orgSerializer = OrganizationSerializer
  private[postgresql] val alertSerializer = AlertSerializer
  private[postgresql] val teamSerializer = TeamSerializer
  private[postgresql] val statusSerializer = StatusSerializer

  /**
   * Delete a token from an existing team
   *
   * @param tokenId identifier of the token to delete
   * @return        true if deleted, false if not found, or error
   */
  override def deleteToken(tokenId: String): Try[Boolean] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      db.withTransaction { implicit session =>
        tokensTable.filter(t => t.deletedAt.isEmpty && t.id === tokenId.toLong).map(t => (t.deletedByGuid, t.deletedAt)).update(Some(uuid), Some(timestamp)) == 1
      }
    }
  }

  /**
   * Retrieve alert with given ID
   *
   * @param alertId        the identifier of the alert
   * @return               the alert configuration, if found; None if not found; Failure if error
   */
  override def getAlert(alertId: String): Try[Option[Alert]] = {
    Try {
      db.withTransaction { implicit session =>
        alertsTable.sortBy(_.createdAt).filter(a => a.deletedAt.isEmpty && a.id === alertId.toLong).list match {
          case List() => None
          case List(a) => Some(alertSerializer.fromPostgresRecord(a))
        }
      }
    } recover {
      case e => sys.error(s"Unable to fetch an alert from DB ${e.getMessage}")
    }
  }

  /**
   *
   * @param organization       the organization to update
   * @param organizationPatch  the new data for update
   * @return                   the updated organization
   */
  override def updateOrganization(organization: Organization, organizationPatch: OrganizationPatch): Try[Option[Organization]] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      db.withTransaction { implicit session =>
        val email = organizationPatch.email.getOrElse(organization.email)
        val notificationUrl = organizationPatch.notificationUrl.getOrElse(organization.notificationUrl)
        organizationsTable.filter(o => o.deletedAt.isEmpty && o.id === organization.id.get.toLong).
          map(a => (a.email, a.notificationUrl, a.updatedAt, a.updatedByGuid)).
          update(email, notificationUrl, timestamp, uuid) match {
          case 1 => Some(Organization(organization.id, organization.name, email, notificationUrl, organization.tokens))
          case 0 => None
          case _ => sys.error(s"Unable to update Organization with id ${organization.id.get}")
        }
      }
    } recover {
      case e => sys.error(s"Unable to update an organization ${e.getMessage}")
    }
  }

  /**
   *
   * @param organization       the organization to update
   * @param cluster            the new cluster for this organization
   * @return                   the updated organization
   */
  override def updateOrganizationCluster(organization: Organization, cluster: Option[String]): Try[Option[Organization]] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      db.withTransaction { implicit session =>
        organizationsTable.filter(o => o.deletedAt.isEmpty && o.id === organization.id.get.toLong).
          map(a => (a.cluster, a.updatedAt, a.updatedByGuid)).
          update(cluster, timestamp, uuid) match {
          case 1 => Some(Organization(organization.id, organization.name, organization.email, organization.notificationUrl, organization.tokens, cluster))
          case 0 => None
          case _ => sys.error(s"Unable to update Organization with id ${organization.id.get}")
        }
      }
    } recover {
      case e => sys.error(s"Unable to update an organization ${e.getMessage}")
    }
  }

  /**
   *
   * @param alert          the alert configuration to be updated
   * @param alertPatch       the new alert configuration
   * @return               the updated alert
   */
  override def updateAlert(alert: Alert, alertPatch: AlertPatch): Try[Option[Alert]] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      val description = alertPatch.description.getOrElse(alert.description)
      val enabled = alertPatch.enabled.getOrElse(alert.enabled)
      val period = alertPatch.period.getOrElse(alert.period)
      val handbookUrl = alertPatch.handbookUrl.orElse(alert.handbookUrl)
      val routingMap = alertPatch.routing.orElse(alert.routing)
      val routing = Alert.routingAsStr(routingMap)

      db.withTransaction { implicit session =>
        val alertToUpdate = alertsTable.filter(a => a.deletedAt.isEmpty && a.id === alert.id.get.toLong).map(a => (a.description, a.status, a.period, a.handbookUrl, a.routing, a.updatedAt, a.updatedByGuid))
        alertToUpdate.update(description, Some(enabled), period, handbookUrl, routing, timestamp, uuid) match {
          case 1 => Some(Alert(alert.id, description, enabled, period, alert.condition, handbookUrl, routingMap))
          case 0 => None
          case _ => sys.error(s"Unable to update Alert with id=" + alert.id.getOrElse("ALERT_ID_UNDEFINED"))
        }
      }
    } recover {
      case e => sys.error(s"Unable to update an alert: ${e.getMessage}")
    }
  }

  /**
   * Add a token to an existing organization
   *
   * @param organization the organization to modify
   * @param token   the token to add
   * @return        the new token object
   */
  override def addOrganizationToken(organization: Organization, token: Token): Try[Token] = {
    addToken(organization, None, token)
  }

  private def addToken(organization: Organization, teamId: Option[Long], token: Token): Try[Token] = {
    val (uuid, timestamp) = createUuidAndTimestamp
    Try {
      db.withTransaction { implicit session =>
        val newTokenId = (tokensTable returning tokensTable.map(_.id)) += TokensRow(1, organization.id.get.toLong, teamId, token.description, token.value, uuid, timestamp, uuid, timestamp, None, None)
        Token(Some(newTokenId.toString), token.description, token.value, new DateTime(timestamp))
      }
    } recover {
      case e => sys.error(s"Unable to add a new token to organization: ${e.getMessage}")
    }
  }

  /**
   * Add a token to an existing team
   *
   * @param organization the organization to modify
   * @param team    the team to modify
   * @param token   the token to add
   * @return        the new token object
   */
  override def addTeamToken(organization: Organization, team: Team, token: Token): Try[Token] = {
    addToken(organization, Some(team.id.get.toLong), token)
  }

  /**
   * Add an alert configuration for the given organization
   *
   * @param organization   the organization for this alert
   * @param alert          the alert configuration
   * @return               the created alert
   */
  override def createOrganizationAlert(organization: Organization, alert: Alert, queries: Set[String]): Try[Option[Alert]] = {
    createAlert(organization, alert, None, queries)
  }

  private def createAlert(organization: Organization, alert: Alert, teamId: Option[Long], queries: Set[String]): Try[Option[Alert]] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      val orgId: Long = organization.id.get.toLong
      db.withTransaction { implicit session =>
        val alertId = (alertsTable returning alertsTable.map(_.id)) += AlertsRow(1, orgId, teamId, alert.description, Some(alert.enabled), alert.condition, alert.period, alert.handbookUrl, alert.routingStr, uuid, timestamp, uuid, timestamp, None, None)

        queries.filter(queryName => {
          queriesTable.filter(_.name === queryName).list.isEmpty
        }).map((queriesTable returning queriesTable.map(_.id)) += QueriesRow(1, _, uuid, timestamp, uuid, timestamp, None, None))

        queries.foreach(queryName => {
          queriesTable.filter(_.name === queryName).list.map(query => {
            alert2queriesTable += AlertQueriesRow(1, alertId, query.id, uuid, timestamp, uuid, timestamp, None, None)
          })
        })

        if (alertId >= 0) {
          Some(Alert(Some(alertId.toString), alert.description, alert.enabled, alert.period, alert.condition, alert.handbookUrl, alert.routing))
        } else None
      }
    } recover {
      case e => sys.error(s"Unable to create an alert for organization: ${e.getMessage}")
    }
  }

  private def createUuidAndTimestamp: (UUID, Timestamp) = {
    val uuid = UUID.randomUUID() // FIXME: where to get uuid from?
    val timestamp = new Timestamp(System.currentTimeMillis())
    (uuid, timestamp)
  }

  /**
   * Find all teams for specified organization
   *
   * @param organization  the organization to lookup
   * @return the Team object, if found
   */
  override def getTeams(organization: Organization): Try[Seq[Team]] = {
    Try {
      db.withTransaction { implicit session =>
        val result = for {
          token <- tokensTable.sortBy(_.createdAt).filter(_.deletedAt.isEmpty)
          team <- token.teamsFk.filter(_.deletedAt.isEmpty)
          o <- team.organizationsFk.filter(org => org.deletedAt.isEmpty && org.id === organization.id.get.toLong)
        } yield (token, team)
        result.list.groupBy(_._2.id).map(team => teamSerializer.fromPostgresRecord(team._2).get).toList
      }
    } recover {
      case e => sys.error(s"Unable to fetch Teams from DB ${e.getMessage}")
    }
  }

  /**
   * Delete team with given name for specified organization
   *
   * @param organization  the organization to lookup
   * @param teamName a team name to delete
   * @return true if deleted, false if it doesn't exist
   */
  override def deleteTeam(organization: Organization, teamName: String): Try[Boolean] = {
    Try {
      getTeam(organization, teamName) match {
        case Success(Some(team)) =>
          deleteTeamAlerts(team)
          db.withTransaction { implicit session =>
            val (uuid, timestamp) = createUuidAndTimestamp
            teamsTable.filter(t => t.deletedAt.isEmpty && t.id === team.id.get.toLong).map(t => (t.deletedByGuid, t.deletedAt)).update(Some(uuid), Some(timestamp)) == 1
          }

        case Success(None) => false
        case _ => sys.error(s"Unable to find a team with name=$teamName")
      }
    }
  }

  private def deleteTeamAlerts(team: Team) = {
    val (uuid, timestamp) = createUuidAndTimestamp
    db.withTransaction { implicit session =>
      alertsTable.filter(a => a.deletedAt.isEmpty && a.id === team.id.get.toLong).map(t => (t.deletedByGuid, t.deletedAt)).update(Some(uuid), Some(timestamp))
    }
  }

  /**
   * Find team with given name for specified organization
   *
   * @param organization  the organization to lookup
   * @param teamName a team name to find
   * @return the Team object, if found
   */
  override def getTeam(organization: Organization, teamName: String): Try[Option[Team]] = {
    Try {
      db.withTransaction { implicit session =>
        val result = for {
          token <- tokensTable.sortBy(_.createdAt).filter(_.deletedAt.isEmpty)
          team <- token.teamsFk.filter(teamRow => teamRow.deletedAt.isEmpty && teamRow.name === teamName)
          o <- team.organizationsFk.filter(orgRow => orgRow.deletedAt.isEmpty && orgRow.id === organization.id.get.toLong)
        } yield (token, team)
        teamSerializer.fromPostgresRecord(result.list)
      }
    } recover {
      case e => sys.error(s"Unable to fetch a team from DB: ${e.getMessage}")
    }
  }

  /**
   * Update cluster for existing team in given organization
   *
   * @param organization       the organization for this team
   * @param team               the team for which we update the cluster
   * @param cluster            the new cluster
   * @return                   the updated team
   */
  override def updateTeamCluster(organization: Organization, team: Team, cluster: Option[String]): Try[Option[Team]] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      db.withTransaction { implicit session =>
        teamsTable.filter(o => o.deletedAt.isEmpty && o.id === team.id.get.toLong && o.organizationId === organization.id.get.toLong).
          map(a => (a.cluster, a.updatedAt, a.updatedByGuid)).
          update(cluster, timestamp, uuid) match {
          case 1 => Some(Team(team.id, team.name, team.tokens, cluster))
          case 0 => None
          case _ => sys.error(s"Unable to update Team with id ${team.id.get}")
        }
      }
    } recover {
      case e => sys.error(s"Unable to update a team: ${e.getMessage}")
    }
  }


  /**
   * Delete alert with given ID for given organization and team
   *
   * @param alertId        the identifier of the alert
   * @return               true, if found; None if not found; Failure if error
   */
  override def deleteAlert(alertId: String): Try[Boolean] = {
    Try {
      db.withTransaction { implicit session =>
        val toSoftDelete = alertsTable.filter(a => a.deletedAt.isEmpty && a.id === alertId.toLong).map(a => (a.deletedByGuid, a.deletedAt))
        val (uuid, timestamp) = createUuidAndTimestamp
        toSoftDelete.update(Some(uuid), Some(timestamp)) match {
          case 1 => true
          case 0 => false
          case _ => sys.error(s"Unable to delete Alert with id $alertId")
        }
      }
    } recover {
      case e => false
    }
  }

  /**
   * Delete an organization from the table with the given name.
   *
   * @param name the name of the organization to delete
   * @return true if successful, false if not found
   */
  override def deleteOrganization(name: String): Try[Boolean] = {
    Try {
      db.withTransaction { implicit session =>
        val result = for {
          t <- teamsTable.filter(_.deletedAt.isEmpty)
          o <- t.organizationsFk.filter(o => o.deletedAt.isEmpty && o.name === name)
        } yield o

        if (result.list.isEmpty) {
          val (uuid, timestamp) = createUuidAndTimestamp
          organizationsTable.filter(_.name === name).map(o => (o.deletedByGuid, o.deletedAt)).update(Some(uuid), Some(timestamp)) > 0
        } else
          sys.error(s"Unable to delete Organization $name. Delete the organization teams first.")
      }
    } recover {
      case e => sys.error(s"Unable to fetch an organization from DB ${e.getMessage}")
    }
  }

  /**
   * Add an alert configuration for the given organization and team
   *
   * @param organization   the organization for this alert
   * @param team           the team for this alert
   * @param alert          the alert configuration
   * @return               the created alert
   */
  override def createTeamAlert(organization: Organization, team: Team, alert: Alert, queries: Set[String]): Try[Option[Alert]] = {
    createAlert(organization, alert, Some(team.id.get.toLong), queries)
  }

  /**
   * Retrieve configured alerts for given organization and team
   *
   * @param organization   the organization whose alerts to fetch
   * @param team           the team whose alerts to fetch
   * @param limit          an optional limit for number of items to fetch
   * @param offset         offset for pagination
   * @return               list of alerts, and (optional) continuation key
   */
  override def getTeamAlerts(organization: Organization, team: Team, limit: Int, offset: Int): Try[List[Alert]] = {
    def queryAlerts: Query[Tables.AlertsTable, Tables.AlertsRow, Seq] = {
      alertsTable.filter(a => a.deletedAt.isEmpty && a.organizationId === organization.id.get.toLong && a.teamId === team.id.get.toLong).sortBy(_.id)
    }
    getAlerts(limit, offset, queryAlerts)
  }

  /**
   * Create team with given data for specified organization
   *
   * @param organization  the organization to lookup
   * @param team     a team object to store
   * @return         the resulting Team object
   */
  override def createTeam(organization: Organization, team: Team): Try[Option[Team]] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      val orgId: Long = organization.id.get.toLong
      db.withTransaction { implicit session =>
        val newTeamId = (teamsTable returning teamsTable.map(_.id)) += TeamsRow(1, orgId, team.name, None, uuid, timestamp, uuid, timestamp, None, None)
        val tokens = team.tokens.map(_.map { token =>
          val tokenId = (tokensTable returning tokensTable.map(_.id)) += TokensRow(1, orgId, Some(newTeamId), token.description, token.value, uuid, timestamp, uuid, timestamp, None, None)
          Token(Some(tokenId.toString), token.description, token.value, DateTime.now())
        })
        Some(Team(Some(newTeamId.toString), team.name, tokens))
      }
    } recover {
      case e: SQLException => None
      case e: Exception => sys.error(s"Error while saving 'team' to DB  ${e.getMessage}")
    }
  }

  /**
   * Retrieve configured alerts for given organization
   *
   * @param organization   the organization whose alerts to fetch
   * @param limit          an optional limit for number of items to fetch
   * @param offset         offset for pagination
   * @return               list of alerts, and (optional) continuation key
   */
  override def getOrganizationAlerts(organization: Organization, limit: Int, offset: Int): Try[List[Alert]] = {
    def queryAlerts: Query[Tables.AlertsTable, Tables.AlertsRow, Seq] = {
      alertsTable.filter(a => a.deletedAt.isEmpty && a.organizationId === organization.id.get.toLong && a.teamId.isEmpty).sortBy(_.id)
    }
    getAlerts(limit, offset, queryAlerts)
  }

  private def getAlerts(limitRaw: Int, offset: Int, queryAlerts: PostgresDriver.simple.Query[Tables.AlertsTable, Tables.AlertsRow, Seq]): Try[List[Alert]] = {
    Try {
      val limit = Math.min(limitRaw, DEFAULT_MAX_PAGINATION_LIMIT)

      db.withTransaction { implicit session =>
        queryAlerts.drop(offset).take(limit).list.map(r => alertSerializer.fromPostgresRecord(r)).toList
      }
    } recover {
      case e => sys.error(s"Unable to fetch Alerts from DB: ${e.getMessage}")
    }
  }

  /**
   * Create a new organization.
   *
   * Conditional on an organization not already existing with this name.
   *
   * @param user         the organization admin
   * @param organization the organization to create
   * @return the organization that was created
   */
  override def createOrganization(user: User, organization: Organization): Try[Option[Organization]] = {
    Try {
      val (uuid, timestamp) = createUuidAndTimestamp
      db.withTransaction { implicit session =>
        val newOrganizationId = (organizationsTable returning organizationsTable.map(_.id)) += OrganizationsRow(1, organization.name, organization.email, organization.notificationUrl, None, uuid, timestamp, uuid, timestamp, None, None)
        organizationUsersTable += OrganizationUsersRow(1, newOrganizationId, user.id.get, Role.Admin.value)

        val tokens = organization.tokens.map { list =>
          list.map { token =>
            val tokenId = (tokensTable returning tokensTable.map(_.id)) += TokensRow(1, newOrganizationId, None, token.description, token.value, uuid, timestamp, uuid, timestamp, None, None)
            Token(Some(tokenId.toString), token.description, token.value, DateTime.now())
          }
        }
        Some(Organization(Some(newOrganizationId.toString), organization.name, organization.email, organization.notificationUrl, tokens))
      }
    } recover {
      case e: SQLException => None
      case e => sys.error(s"Unable to create an organization in DB $e")
    }
  }

  /**
   * Check if the dataManager is healthy
   *
   * @return true if healthy, false otherwise
   */
  override def isHealthy: Boolean =
    // TODO: implement me
    true

  /**
   * Fetch an organization from the table with the given name.
   *
   * @param name the name of the organization to find
   * @return the organization object
   */
  override def getOrganization(name: String): Try[Option[Organization]] = {
    Try {
      db.withTransaction { implicit session =>
        val result = for {
          t <- tokensTable.sortBy(_.createdAt).filter(t => t.deletedAt.isEmpty && t.teamId.isEmpty)
          o <- t.organizationsFk.filter(o => o.deletedAt.isEmpty && o.name === name)
        } yield (t, o)
        orgSerializer.fromPostgresRecord(result.list)
      }
    } recover {
      case e: PSQLException => sys.error(s"Unable to find an organization in DB ${e.getServerErrorMessage.getMessage}")
      case e => sys.error(s"Unable to find an organization in DB ${e.getMessage}")
    }
  }

  /**
   *
   * @param query continuous query name
   * @return true if CQ exists
   */
  override def queryDoesNotExist(query: String): Boolean = {
    db.withTransaction { implicit session =>
      queriesTable.filter(_.name === query).list.isEmpty
    }
  }

  override def createUser(user: User)
                         (implicit ec: ExecutionContext): Future[Option[User]] = future {
    try {
      db.withTransaction { implicit session =>
        val id = (usersTable returning usersTable.map(_.id)) += UsersRow(1, user.firstName, user.lastName, user.email, user.password, user.salt)

        Some(User(Some(id), user.firstName, user.lastName, user.email, user.password, user.salt))
      }
    } catch {
      case e: Throwable => None
    }
  }

  override def updateUser(user: User, first: Option[String], last: Option[String], passwordInfo: Option[PasswordInfo])
                         (implicit ec: ExecutionContext): Future[Option[User]] = future {
    try {
      db.withTransaction { implicit session =>
        val query = usersTable.filter(_.id === user.id)
        query.list match {
          case List() => sys.error(s"Unable to find user with ID ${user.id}.")
          case List(u) =>
            val newFirstName = first.getOrElse(u.firstName)
            val newLastName = last.getOrElse(u.lastName)
            val newHash = passwordInfo.map(_.hash) getOrElse user.password
            val newSalt = passwordInfo.map(_.salt) getOrElse user.salt

            query.map(u => (u.firstName, u.lastName, u.password, u.salt)).update(newFirstName, newLastName, newHash, newSalt) match {

              case 1 => Some(User(Some(u.id), newFirstName, newLastName, u.email, newHash, newSalt))
              case 0 => None
              case _ => None
            }
          case l => None
        }
      }
    } catch {
      case e: Throwable => None
    }
  }

  override def deleteUser(id: Long)
                         (implicit ec: ExecutionContext): Future[Unit] = future {
    db.withTransaction { implicit session =>
      val query = usersTable.filter(_.id === id)
      query.list match {
        case List() => sys.error(s"Unable to find user with ID $id.")
        case List(u) =>
          organizationUsersTable.filter(_.userId === u.id).delete
          teamUsersTable.filter(_.userId === u.id).delete
          query.delete

        case l => sys.error(s"Expected to find only one user with ID $id, but found $l")
      }
    }
  }

  override def getUser(id: Long)
                      (implicit ec: ExecutionContext): Future[Option[User]] = future {
    db.withTransaction { implicit session =>
      usersTable.filter(_.id === id).list match {
        case List() => None
        case List(user) => Some(User(Some(user.id), user.firstName, user.lastName, user.email, user.password, user.salt))

        case l => sys.error(s"Expected to find only one user with ID $id, but found $l")
      }
    }
  }

  override def getUserByEmail(email: String)                      (implicit ec: ExecutionContext): Future[Option[User]] = future {
    db.withTransaction { implicit session =>
      usersTable.filter(_.email === email).list match {
        case List() => None
        case List(user) => Some(User(Some(user.id), user.firstName, user.lastName, user.email, user.password, user.salt))

        case l => sys.error(s"Expected to find only one user with email $email, but found $l")
      }
    }
  }

  override def findUser(query: String)
                       (implicit ec: ExecutionContext): Future[List[User]] = future {
    db.withTransaction { implicit session =>
      val q = s"%$query%".toLowerCase
      usersTable.filter(u => u.firstName.toLowerCase.like(q) || u.lastName.toLowerCase.like(q) || u.email.toLowerCase.like(q)).list map { case user =>
        User(Some(user.id), user.firstName, user.lastName, user.email, user.password, user.salt)
      }
    }
  }

  override def createConfirmationToken(confirmationToken: ConfirmationToken)
                                      (implicit ec: ExecutionContext): Future[Option[ConfirmationToken]] = future {
    try {
      db.withTransaction { implicit session =>
        val id = (confirmationTokens returning confirmationTokens.map(_.id)) += ConfirmationTokensRow(1,
          confirmationToken.uuid, confirmationToken.email,
          new Timestamp(confirmationToken.creationTime.getMillis),
          new Timestamp(confirmationToken.expirationTime.getMillis),
          confirmationToken.isSignUp)

        Some(ConfirmationToken(Some(id), confirmationToken.uuid, confirmationToken.email,
          confirmationToken.creationTime, confirmationToken.expirationTime, confirmationToken.isSignUp))
      }
    } catch {
      case e: SQLException => None
      case e: Throwable => sys.error(s"Unable to create a user in DB $e")
    }
  }

  override def deleteExpiredConfirmationTokens(expirationTime: DateTime)
                                              (implicit ec: ExecutionContext): Future[Unit] = future {
    try {
      val now = new Timestamp(expirationTime.getMillis)
      db.withTransaction { implicit session =>
        confirmationTokens.filter(_.expirationTime < now).delete
      }
    } catch {
      case e: Throwable => sys.error(s"Unable to delete expired tokens: ${e.getMessage}")
    }
  }

  override def deleteConfirmationToken(uuid: String)
                                      (implicit ec: ExecutionContext): Future[Option[ConfirmationToken]] = future {
    try {
      db.withTransaction { implicit session =>
        val tokenResults = confirmationTokens.filter(_.uuid === uuid)
        tokenResults.list match {
          case List() => None
          case List(t) =>
            tokenResults.delete
            Some(ConfirmationToken(Some(t.id), t.uuid, t.email, new DateTime(t.creationTime.getTime),
              new DateTime(t.expirationTime.getTime), t.isSignUp))

          case l => sys.error(s"Should have found only one token with uuid $uuid, but found $l")
        }
      }
    } catch {
      case e: Throwable => sys.error(s"Unable to delete token with uuid $uuid: ${e.getMessage}")
    }
  }

  override def getConfirmationTokenByUUID(uuid: String)
                                         (implicit ec: ExecutionContext): Future[Option[ConfirmationToken]] = future {
    try {
      db.withTransaction { implicit session =>
        confirmationTokens.filter(t => t.uuid === uuid).list match {
          case List() => None
          case List(token) => Some(
            ConfirmationToken(Some(token.id), token.uuid, token.email,
              new DateTime(token.creationTime.getTime),
              new DateTime(token.expirationTime.getTime),
              token.isSignUp))

          case l => sys.error(s"Should have found only one token with uuid $uuid, but found $l")
        }
      }
    } catch {
      case e: Throwable => sys.error(s"Unable to get token for uuid $uuid, ${e.getMessage}")
    }
  }

  override def getOrganizationsForUser(user: User)
                                      (implicit ec: ExecutionContext): Future[List[(String, Role)]] = user.id match {
    case None => Future.failed(new RuntimeException(s"User with ID ${user.id} does not exist."))

    case Some(id) =>
      future {
        try {
          db.withTransaction { implicit session =>
            val organizations = for {
              orgUser <- organizationUsersTable.filter(_.userId === id)
              org <- orgUser.organizationsFk.filter(_.deletedAt.isEmpty)
            } yield (org.name, orgUser.role)

            val orgList = organizations.list map { case (name, role) => name -> Role(role) }

            val teams = for {
              teamUser <- teamUsersTable.filter(_.userId === id)
              team <- teamUser.teamsFk.filter(_.deletedAt.isEmpty)
              org <- team.organizationsFk.filter(_.deletedAt.isEmpty)
            } yield (org.name)

            val teamsList = teams.list.filterNot(orgList.toMap.contains(_)).toSet.toList

            orgList union teamsList.map { name => name -> Role.Team }
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to fetch organizations for user $user: ${e.getMessage}")
        }
      }
  }

  override def getTeamsForUser(org: Organization, user: User)
                              (implicit ec: ExecutionContext): Future[List[(String, Role)]] =  (org.id, user.id) match {
    case (Some(orgId), Some(userId)) =>
      future {
        try {
          db.withTransaction { implicit session =>
            val teams = for {
              teamUser <- teamUsersTable.filter(_.userId === userId)
              team <- teamUser.teamsFk.filter(t => t.organizationId === orgId.toLong && t.deletedAt.isEmpty)
            } yield (team.name, teamUser.role)

            teams.list map { case (teamName, role) => teamName -> Role(role) }
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to fetch organizations for user $user: ${e.getMessage}")
        }
      }

    case _ => Future.failed(new RuntimeException(s"Both organization ${org.id}  and user ${user.id} must exist."))
  }

  override def getUsersForOrganization(organization: Organization)
                                      (implicit ec: ExecutionContext): Future[List[(User, Role)]] = organization.id match {
    case None => Future.failed(new RuntimeException(s"Organization with id ${organization.id} does not exist."))

    case Some(id) =>
      future {
        try {
          db.withTransaction { implicit session =>
            val users = for {
              orgUser <- organizationUsersTable.filter(_.organizationId === id.toLong)
              user <- orgUser.usersFk
            } yield (user.id, user.firstName, user.lastName, user.email, user.password, user.salt, orgUser.role)

            users.list map { case (userId, first, last, email, password, salt, role) =>
              (User(Some(userId), first, last, email, password, salt), Role(role))
            }
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to fetch users for organization ${organization.name}.")
        }
      }
  }

  override def getUsersForTeam(team: Team)
                              (implicit ec: ExecutionContext): Future[List[(User, Role)]] = team.id match {
    case None => Future.failed(new RuntimeException(s"Team with id ${team.id} does not exist."))

    case Some(id) =>
      future {
        try {
          db.withTransaction { implicit session =>
            val users = for {
              teamUser <- teamUsersTable.filter(_.teamId === id.toLong)
              user <- teamUser.usersFk
            } yield (user.id, user.firstName, user.lastName, user.email, user.password, user.salt, teamUser.role)

            users.list map { case (userId, first, last, email, password, salt, role) =>
              (User(Some(userId), first, last, email, password, salt), Role(role))
            }
          }
        }
      }
  }

  override def addUserToOrganization(user: User, organization: Organization, role: Role)
                                    (implicit ec: ExecutionContext): Future[Boolean] = (user.id, organization.id) match {
    case (Some(userId), Some(orgId)) =>
      future {
        try {
          db.withTransaction { implicit session =>
            if (organizationUsersTable.filter(r => r.userId === userId && r.organizationId === orgId.toLong).list.isEmpty) {
              organizationUsersTable += OrganizationUsersRow(1, orgId.toLong, userId, role.value)
              true
            } else {
              false
            }
          }
        } catch {
          case e: Throwable =>
            log.warn(s"Error while adding user $userId to organization $orgId: ${e.getMessage}")
            sys.error(s"Unable to add user $userId to organization $orgId.")
        }
      }
    case _ => Future.failed(new RuntimeException(s"Cannot add user to organization unless both exist."))
  }

  override def addUserToTeam(user: User, team: Team, role: Role)
                            (implicit ec: ExecutionContext): Future[Boolean] = (user.id, team.id) match {
    case (Some(userId), Some(teamId)) =>
      future {
        try {
          db.withTransaction { implicit session =>
            if (teamUsersTable.filter(r => r.userId === userId && r.teamId === teamId.toLong).list.isEmpty) {
              teamUsersTable += TeamUsersRow(1, teamId.toLong, userId, role.value)
              true
            } else false
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to add user $userId to team $teamId.")
        }
      }

    case _ => Future.failed(new RuntimeException(s"Cannot add user to team unless both exist."))
  }

  override def deleteUserFromOrganization(user: User, organization: Organization)
                                         (implicit ec: ExecutionContext): Future[Unit] = (user.id, organization.id) match {
    case (Some(userId), Some(orgId)) =>
      future {
        try {
          db.withTransaction { implicit session =>
            organizationUsersTable.filter(f => f.userId === userId && f.organizationId === orgId.toLong).delete
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to delete user $userId from organization $orgId.")
        }
      }
    case _ => Future.failed(new RuntimeException(s"Cannot add user to organization unless both exist."))
  }

  override def deleteUserFromTeam(user: User, team: Team)
                                 (implicit ec: ExecutionContext): Future[Unit] =  (user.id, team.id) match {
    case (Some(userId), Some(teamId)) =>
      future {
        try {
          db.withTransaction { implicit session =>
            teamUsersTable.filter(f => f.userId === userId && f.teamId === teamId.toLong).delete
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to delete user $userId from team $teamId.")
        }
      }

    case _ => Future.failed(new RuntimeException(s"Cannot add user to team unless both exist."))
  }

  override def changeOrganizationRole(user: User, organization: Organization, role: Role)
                                     (implicit ec: ExecutionContext): Future[Unit] = (user.id, organization.id) match {
    case (Some(userId), Some(orgId)) =>
      future {
        try {
          db.withTransaction { implicit session =>
            organizationUsersTable.filter(ou => ou.organizationId === orgId.toLong && ou.userId === userId).map(_.role).update(role.value) match {
              case n if n !=1 => sys.error(s"Failed to update role of user $userId in organization $orgId.")
              case _ =>
            }
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to modify role of user $userId in organization $orgId.")
        }
      }
    case _ => Future.failed(new RuntimeException(s"Cannot modify role of user in organization unless both exist."))
  }

  override def changeTeamRole(user: User, team: Team, role: Role)
                             (implicit ec: ExecutionContext): Future[Unit] = (user.id, team.id) match {
    case (Some(userId), Some(teamId)) =>
      future {
        try {
          db.withTransaction { implicit session =>
            teamUsersTable.filter(tu => tu.teamId === teamId.toLong && tu.userId === userId).map(_.role).update(role.value) match {
              case n if n !=1 => sys.error(s"Failed to update role of user $userId in team $teamId.")
              case _ =>
            }
          }
        } catch {
          case e: Throwable => sys.error(s"Unable to add user $userId to team $teamId.")
        }
      }

    case _ => Future.failed(new RuntimeException(s"Cannot add user to team unless both exist."))
  }

  override def createSessionToken(userId: Long, creationTime: DateTime, expirationTime: DateTime)
                                 (implicit ec: ExecutionContext): Future[SessionToken] =  future {
    db.withTransaction { implicit session =>
      val token = UUID.randomUUID().toString
      val tokenId = (sessionTokensTable returning sessionTokensTable.map(_.id)) +=
        SessionTokensRow(1, userId, token, new Timestamp(creationTime.getMillis), new Timestamp(expirationTime.getMillis))

      SessionToken(Some(tokenId), userId, token, creationTime, expirationTime)
    }
  }

  override def findUserByToken(tokenString: String, expirationTime: DateTime)
                              (implicit ec: ExecutionContext): Future[Option[User]] = future {
    db.withTransaction { implicit session =>
      val now = new Timestamp(expirationTime.getMillis)
      val later = new Timestamp(new DateTime(now).plusHours(1).getMillis)

      val query = for {
        token <- sessionTokensTable.filter(t => t.token === tokenString && t.expirationTime > now)
        user <- token.usersFk
      } yield (token.id, user.id, user.firstName, user.lastName, user.email, user.password, user.salt)

      val users = query.list map { case (tokenId, id, first, last, email, password, salt) =>
        sessionTokensTable.filter(t => t.id === tokenId).map(_.expirationTime).update(later)
        User(Some(id), first, last, email, password, salt)
      }

      users.headOption
    }
  }

  override def deleteSessionToken(id: Long)(implicit ec: ExecutionContext): Future[Unit] = future {
    db.withTransaction { implicit session =>
      sessionTokensTable.filter(_.id === id).delete
    }
  }

  override def deleteExpiredSessionTokens(expirationTime: DateTime)(implicit ec: ExecutionContext): Future[Unit] = future {
    db.withTransaction { implicit session =>
      val now = new Timestamp(expirationTime.getMillis)
      db.withTransaction { implicit session =>
        sessionTokensTable.filter(_.expirationTime < now).delete
      }
    }
  }

  override def getStatus()(implicit ec: ExecutionContext): Future[CaveStatus] = future {
    db.withTransaction { implicit session =>
      statusSerializer.fromPostgresRecord(statusTable.list)
    }
  }
}
