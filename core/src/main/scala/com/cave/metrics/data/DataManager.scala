package com.cave.metrics.data

import org.joda.time.DateTime

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

trait DataManager {

  /**
   * Check if the continuous
   *
   * @return true if healthy, false otherwise
   */
  def queryDoesNotExist(query: String): Boolean

  /**
   * Check if the dataManager is healthy
   *
   * @return true if healthy, false otherwise
   */
  def isHealthy: Boolean

  /**
   * Close database connection
   */
  def closeDbConnection(): Unit

  /**
   * Create a new organization.
   *
   * Conditional on an organization not already existing with this name.
   *
   * @param admin        the owner of the organization
   * @param organization the organization to create
   * @return the organization that was created
   */
  def createOrganization(admin: User, organization: Organization): Try[Option[Organization]]

  /**
   * Fetch an organization from the table with the given name.
   *
   * @param name the name of the organization to find
   * @return the organization object
   */
  def getOrganization(name: String): Try[Option[Organization]]

  /**
   * Update existing organization from given data
   *
   * @param organization       the organization to update
   * @param organizationPatch  the new data for update
   * @return                   the updated organization
   */
  def updateOrganization(organization: Organization, organizationPatch: OrganizationPatch): Try[Option[Organization]]

  /**
   * Update cluster for existing organization
   *
   * @param organization       the organization to update
   * @param cluster            the new cluster
   * @return                   the updated organization
   */
  def updateOrganizationCluster(organization: Organization, cluster: Option[String]): Try[Option[Organization]]

  /**
   * Delete an organization from the table with the given name.
   *
   * @param name the name of the organization to delete
   * @return true if successful, false if not found
   */
  def deleteOrganization(name: String): Try[Boolean]

  /**
   * Find all teams for specified organization
   *
   * @param organization  the organization to lookup
   * @return the Team object, if found
   */
  def getTeams(organization: Organization): Try[Seq[Team]]

  /**
   * Find team with given name for specified organization
   *
   * @param organization  the organization to lookup
   * @param teamName a team name to find
   * @return the Team object, if found
   */
  def getTeam(organization: Organization, teamName: String): Try[Option[Team]]

  /**
   * Create team with given data for specified organization
   *
   * @param organization  the organization to lookup
   * @param team     a team object to store
   * @return         the resulting Team object
   */
  def createTeam(organization: Organization, team: Team): Try[Option[Team]]

  /**
   * Update cluster for existing team in given organization
   *
   * @param organization       the organization for this team
   * @param team               the team for which we update the cluster
   * @param cluster            the new cluster
   * @return                   the updated team
   */
  def updateTeamCluster(organization: Organization, team: Team, cluster: Option[String]): Try[Option[Team]]

  /**
   * Delete team with given name for specified organization
   *
   * @param organization  the organization to lookup
   * @param teamName a team name to delete
   * @return true if deleted, false if it doesn't exist
   */
  def deleteTeam(organization: Organization, teamName: String): Try[Boolean]

  /**
   * Add a token to an existing organization
   *
   * @param organization the organization to modify
   * @param token   the token to add
   * @return        the new token object
   */
  def addOrganizationToken(organization: Organization, token: Token): Try[Token]

  /**
   * Delete a token from an existing organization
   *
   * @param tokenId identifier of the token to delete
   * @return        true if deleted, false if not found, or error
   */
  def deleteToken(tokenId: String): Try[Boolean]

  /**
   * Add a token to an existing team
   *
   * @param organization the organization to modify
   * @param team    the team to modify
   * @param token   the token to add
   * @return        the new token object
   */
  def addTeamToken(organization: Organization, team: Team, token: Token): Try[Token]

  /**
   * Add an alert configuration for the given organization
   *
   * @param organization   the organization for this alert
   * @param alert          the alert configuration
   * @return               the created alert
   */
  def createOrganizationAlert(organization: Organization, alert: Alert, continuousQueries: Set[String]): Try[Option[Alert]]

  /**
   * Retrieve configured alerts for given organization
   *
   * @param organization   the organization whose alerts to fetch
   * @param limit          an optional limit for number of items to fetch
   * @param offset         offset for pagination
   * @return               list of alerts, and (optional) continuation key
   */
  def getOrganizationAlerts(organization: Organization, limit: Int = 20, offset: Int = 0): Try[Seq[Alert]]

  /**
   * Delete alert with given ID for given organization and team
   *
   * @param alertId        the identifier of the alert
   * @return               true, if found; None if not found; Failure if error
   */
  def deleteAlert(alertId: String): Try[Boolean]

  /**
   * Add an alert configuration for the given organization and team
   *
   * @param team           the team for this alert
   * @param alert          the alert configuration
   * @return               the created alert
   */
  def createTeamAlert(organization: Organization, team: Team, alert: Alert, continuousQueries: Set[String]): Try[Option[Alert]]

  /**
   * Update an alert configuration for the given organization and team
   *
   * @param alert          the alert configuration to be updated
   * @param alertPatch       the new alert configuration
   * @return               the updated alert
   */
  def updateAlert(alert: Alert, alertPatch: AlertPatch): Try[Option[Alert]]

  /**
   * Retrieve configured alerts for given organization and team
   *
   * @param organization   the organization whose alerts to fetch
   * @param team           the team whose alerts to fetch
   * @param limit          an optional limit for number of items to fetch
   * @param offset         offset for pagination
   * @return               list of alerts, and (optional) continuation key
   */
  def getTeamAlerts(organization: Organization, team: Team, limit: Int = 20, offset: Int = 0): Try[Seq[Alert]]

  /**
   * Retrieve alert with given ID for given org and (optional) team
   *
   * @param alertId        the identifier of the alert
   * @return               the alert configuration, if found; None if not found; Failure if error
   */
  def getAlert(alertId: String): Try[Option[Alert]]


  // Operations for Users
  def createUser(user: User)(implicit ec: ExecutionContext): Future[Option[User]]
  def updateUser(user: User, first: Option[String], last: Option[String], passwordInfo: Option[PasswordInfo])(implicit ec: ExecutionContext): Future[Option[User]]
  def getUser(id: Long)(implicit ec: ExecutionContext): Future[Option[User]]
  def getUserByEmail(email: String)(implicit ec: ExecutionContext): Future[Option[User]]
  def deleteUser(id: Long)(implicit ec: ExecutionContext): Future[Unit]
  def findUser(query: String)(implicit ec: ExecutionContext): Future[List[User]]

  // Operations for Session Tokens
  def createSessionToken(userId: Long, creationTime: DateTime, expirationTime: DateTime)(implicit ec: ExecutionContext): Future[SessionToken]
  def findUserByToken(token: String, expirationTime: DateTime)(implicit ec: ExecutionContext): Future[Option[User]]
  def deleteSessionToken(id: Long)(implicit ec: ExecutionContext): Future[Unit]
  def deleteExpiredSessionTokens(expirationTime: DateTime)(implicit ec: ExecutionContext): Future[Unit]

  // Operations for Users and Organizations / Teams
  def getOrganizationsForUser(user: User)(implicit ec: ExecutionContext): Future[List[(String, Role)]]
  def getTeamsForUser(org: Organization, user: User)(implicit ec: ExecutionContext): Future[List[(String, Role)]]
  def getUsersForOrganization(organization: Organization)(implicit ec: ExecutionContext): Future[List[(User, Role)]]
  def getUsersForTeam(team: Team)(implicit ec: ExecutionContext): Future[List[(User, Role)]]
  def addUserToOrganization(user: User, organization: Organization, role: Role)(implicit ec: ExecutionContext): Future[Boolean]
  def addUserToTeam(user: User, team: Team, role: Role)(implicit ec: ExecutionContext): Future[Boolean]
  def deleteUserFromOrganization(user: User, organization: Organization)(implicit ec: ExecutionContext): Future[Unit]
  def deleteUserFromTeam(user: User, team: Team)(implicit ec: ExecutionContext): Future[Unit]
  def changeOrganizationRole(user: User, organization: Organization, role: Role)(implicit ec: ExecutionContext): Future[Unit]
  def changeTeamRole(user: User, team: Team, role: Role)(implicit ec: ExecutionContext): Future[Unit]

  // Operations for Confirmation Tokens
  def createConfirmationToken(confirmationToken: ConfirmationToken)(implicit ec: ExecutionContext): Future[Option[ConfirmationToken]]
  def getConfirmationTokenByUUID(uuid: String)(implicit ec: ExecutionContext): Future[Option[ConfirmationToken]]
  def deleteConfirmationToken(uuid: String)(implicit ec: ExecutionContext): Future[Option[ConfirmationToken]]
  def deleteExpiredConfirmationTokens(expirationTime: DateTime)(implicit ec: ExecutionContext): Future[Unit]

  def getStatus()(implicit ec: ExecutionContext): Future[CaveStatus]
}
