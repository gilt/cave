package com.cave.metrics.data.postgresql

import java.util.UUID
import scala.slick.jdbc.GetResult

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = scala.slick.driver.PostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: scala.slick.driver.JdbcProfile

  import profile.simple._

import scala.slick.model.ForeignKeyAction

  /** DDL for all tables. Call .create to execute. */
  lazy val ddl = AlertQueries.ddl ++ Alerts.ddl ++ Organizations.ddl ++ Queries.ddl ++ Teams.ddl ++ Tokens.ddl ++ Schedulers.ddl ++ Users.ddl ++ SessionTokens.ddl ++ ConfirmationTokens.ddl ++ OrganizationUsers.ddl ++ TeamUsers.ddl

  /** Entity class storing rows of table AlertQueries
    * @param id Database column id AutoInc, PrimaryKey
    * @param alertId Database column alert_id
    * @param queryId Database column query_id
    * @param createdByGuid Database column created_by_guid
    * @param createdAt Database column created_at
    * @param updatedByGuid Database column updated_by_guid
    * @param updatedAt Database column updated_at
    * @param deletedByGuid Database column deleted_by_guid
    * @param deletedAt Database column deleted_at  */
  case class AlertQueriesRow(id: Long, alertId: Long, queryId: Long, createdByGuid: UUID, createdAt: java.sql.Timestamp, updatedByGuid: UUID, updatedAt: java.sql.Timestamp, deletedByGuid: Option[UUID], deletedAt: Option[java.sql.Timestamp])

  def createSchema()(implicit session: Session) = ddl.create

  def dropSchema()(implicit session: Session) = ddl.drop

  /** Table description of table alert_queries. Objects of this class serve as prototypes for rows in queries. */
  class AlertQueriesTable(tag: Tag) extends Table[AlertQueriesRow](tag, "alert_queries") {
    def * = (id, alertId, queryId, createdByGuid, createdAt, updatedByGuid, updatedAt, deletedByGuid, deletedAt) <>(AlertQueriesRow.tupled, AlertQueriesRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, alertId.?, queryId.?, createdByGuid.?, createdAt.?, updatedByGuid.?, updatedAt.?, deletedByGuid, deletedAt).shaped.<>({ r => import r._; _1.map(_ => AlertQueriesRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8, _9)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column alert_id  */
    val alertId: Column[Long] = column[Long]("alert_id")
    /** Database column query_id  */
    val queryId: Column[Long] = column[Long]("query_id")
    /** Database column created_by_guid  */
    val createdByGuid: Column[UUID] = column[UUID]("created_by_guid")
    /** Database column created_at  */
    val createdAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")
    /** Database column updated_by_guid  */
    val updatedByGuid: Column[UUID] = column[UUID]("updated_by_guid")
    /** Database column updated_at  */
    val updatedAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("updated_at")
    /** Database column deleted_by_guid  */
    val deletedByGuid: Column[Option[UUID]] = column[Option[UUID]]("deleted_by_guid")
    /** Database column deleted_at  */
    val deletedAt: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("deleted_at")

    /** Foreign key referencing Alerts (database name alertquery_alert_fk) */
    lazy val alertsFk = foreignKey("alertquery_alert_fk", alertId, Alerts)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing Queries (database name alertquery_query_fk) */
    lazy val queriesFk = foreignKey("alertquery_query_fk", queryId, Queries)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (alertId,queryId) (database name alert_queries_alert_id_query_id_not_deleted_un_idx) */
    val index1 = index("alert_queries_alert_id_query_id_not_deleted_un_idx", (alertId, queryId), unique = true)
  }

  /** Collection-like TableQuery object for table AlertQueries */
  lazy val AlertQueries = new TableQuery(tag => new AlertQueriesTable(tag))

  /** Entity class storing rows of table Alerts
    * @param id Database column id AutoInc, PrimaryKey
    * @param organizationId Database column organization_id
    * @param teamId Database column team_id
    * @param description Database column description
    * @param status Database column status
    * @param condition Database column condition
    * @param period Database column period
    * @param routing Alert routing information as JSON
    * @param createdByGuid Database column created_by_guid
    * @param createdAt Database column created_at
    * @param updatedByGuid Database column updated_by_guid
    * @param updatedAt Database column updated_at
    * @param deletedByGuid Database column deleted_by_guid
    * @param deletedAt Database column deleted_at  */
  case class AlertsRow(id: Long, organizationId: Long, teamId: Option[Long], description: String, status: Option[Boolean], condition: String, period: String, handbookUrl: Option[String], routing: Option[String], createdByGuid: UUID, createdAt: java.sql.Timestamp, updatedByGuid: UUID, updatedAt: java.sql.Timestamp, deletedByGuid: Option[UUID], deletedAt: Option[java.sql.Timestamp])

  /** Table description of table alerts. Objects of this class serve as prototypes for rows in queries. */
  class AlertsTable(tag: Tag) extends Table[AlertsRow](tag, "alerts") {
    def * = (id, organizationId, teamId, description, status, condition, period, handbookUrl, routing, createdByGuid, createdAt, updatedByGuid, updatedAt, deletedByGuid, deletedAt) <>(AlertsRow.tupled, AlertsRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, organizationId.?, teamId, description.?, status, condition.?, period.?, handbookUrl, routing, createdByGuid.?, createdAt.?, updatedByGuid.?, updatedAt.?, deletedByGuid, deletedAt).shaped.<>({ r => import r._; _1.map(_ => AlertsRow.tupled((_1.get, _2.get, _3, _4.get, _5, _6.get, _7.get, _8, _9, _10.get, _11.get, _12.get, _13.get, _14, _15)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column organization_id  */
    val organizationId: Column[Long] = column[Long]("organization_id")
    /** Database column team_id  */
    val teamId: Column[Option[Long]] = column[Option[Long]]("team_id")
    /** Database column description  */
    val description: Column[String] = column[String]("description")
    /** Database column status  */
    val status: Column[Option[Boolean]] = column[Option[Boolean]]("status")
    /** Database column condition  */
    val condition: Column[String] = column[String]("condition")
    /** Database column period  */
    val period: Column[String] = column[String]("period")
    /** Database column handbook_url */
    val handbookUrl: Column[Option[String]] = column[Option[String]]("handbook_url")
    /** Database column routing  */
    val routing: Column[Option[String]] = column[Option[String]]("routing")
    /** Database column created_by_guid  */
    val createdByGuid: Column[UUID] = column[UUID]("created_by_guid")
    /** Database column created_at  */
    val createdAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")
    /** Database column updated_by_guid  */
    val updatedByGuid: Column[UUID] = column[UUID]("updated_by_guid")
    /** Database column updated_at  */
    val updatedAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("updated_at")
    /** Database column deleted_by_guid  */
    val deletedByGuid: Column[Option[UUID]] = column[Option[UUID]]("deleted_by_guid")
    /** Database column deleted_at  */
    val deletedAt: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("deleted_at")

    /** Foreign key referencing Organizations (database name alert_organization_fk) */
    lazy val organizationsFk = foreignKey("alert_organization_fk", organizationId, Organizations)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing Teams (database name alert_team_fk) */
    lazy val teamsFk = foreignKey("alert_team_fk", teamId, Teams)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }

  /** Collection-like TableQuery object for table Alerts */
  lazy val Alerts = new TableQuery(tag => new AlertsTable(tag))

  /** Entity class storing rows of table Organizations
    * @param id Database column id AutoInc, PrimaryKey
    * @param name Database column name
    * @param email Database column email
    * @param notificationUrl Database column notification_url
    * @param createdByGuid Database column created_by_guid
    * @param createdAt Database column created_at
    * @param updatedByGuid Database column updated_by_guid
    * @param updatedAt Database column updated_at
    * @param deletedByGuid Database column deleted_by_guid
    * @param deletedAt Database column deleted_at  */
  case class OrganizationsRow(id: Long, name: String, email: String, notificationUrl: String, cluster: Option[String], createdByGuid: UUID, createdAt: java.sql.Timestamp, updatedByGuid: UUID, updatedAt: java.sql.Timestamp, deletedByGuid: Option[UUID], deletedAt: Option[java.sql.Timestamp])

  /** Table description of table organizations. Objects of this class serve as prototypes for rows in queries. */
  class OrganizationsTable(tag: Tag) extends Table[OrganizationsRow](tag, "organizations") {
    def * = (id, name, email, notificationUrl, cluster, createdByGuid, createdAt, updatedByGuid, updatedAt, deletedByGuid, deletedAt) <>(OrganizationsRow.tupled, OrganizationsRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, email.?, notificationUrl.?, cluster, createdByGuid.?, createdAt.?, updatedByGuid.?, updatedAt.?, deletedByGuid, deletedAt).shaped.<>({ r => import r._; _1.map(_ => OrganizationsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8.get, _9.get, _10, _11)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name  */
    val name: Column[String] = column[String]("name")
    /** Database column email  */
    val email: Column[String] = column[String]("email")
    /** Database column notification_url  */
    val notificationUrl: Column[String] = column[String]("notification_url")
    /** The Influx cluster, if overriden */
    val cluster = column[Option[String]]("cluster")
    /** Database column created_by_guid  */
    val createdByGuid: Column[UUID] = column[UUID]("created_by_guid")
    /** Database column created_at  */
    val createdAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")
    /** Database column updated_by_guid  */
    val updatedByGuid: Column[UUID] = column[UUID]("updated_by_guid")
    /** Database column updated_at  */
    val updatedAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("updated_at")
    /** Database column deleted_by_guid  */
    val deletedByGuid: Column[Option[UUID]] = column[Option[UUID]]("deleted_by_guid")
    /** Database column deleted_at  */
    val deletedAt: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("deleted_at")

    /** Uniqueness Index over (name) (database name organizations_name_not_deleted_un_idx) */
    val index1 = index("organizations_name_not_deleted_un_idx", name, unique = true)
  }

  /** Collection-like TableQuery object for table Organizations */
  lazy val Organizations = new TableQuery(tag => new OrganizationsTable(tag))

  /** Entity class storing rows of table Queries
    * @param id Database column id AutoInc, PrimaryKey
    * @param name Database column name
    * @param createdByGuid Database column created_by_guid
    * @param createdAt Database column created_at
    * @param updatedByGuid Database column updated_by_guid
    * @param updatedAt Database column updated_at
    * @param deletedByGuid Database column deleted_by_guid
    * @param deletedAt Database column deleted_at  */
  case class QueriesRow(id: Long, name: String, createdByGuid: UUID, createdAt: java.sql.Timestamp, updatedByGuid: UUID, updatedAt: java.sql.Timestamp, deletedByGuid: Option[UUID], deletedAt: Option[java.sql.Timestamp])

  /** Table description of table queries. Objects of this class serve as prototypes for rows in queries. */
  class QueriesTable(tag: Tag) extends Table[QueriesRow](tag, "queries") {
    def * = (id, name, createdByGuid, createdAt, updatedByGuid, updatedAt, deletedByGuid, deletedAt) <>(QueriesRow.tupled, QueriesRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, createdByGuid.?, createdAt.?, updatedByGuid.?, updatedAt.?, deletedByGuid, deletedAt).shaped.<>({ r => import r._; _1.map(_ => QueriesRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7, _8)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name  */
    val name: Column[String] = column[String]("name")
    /** Database column created_by_guid  */
    val createdByGuid: Column[UUID] = column[UUID]("created_by_guid")
    /** Database column created_at  */
    val createdAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")
    /** Database column updated_by_guid  */
    val updatedByGuid: Column[UUID] = column[UUID]("updated_by_guid")
    /** Database column updated_at  */
    val updatedAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("updated_at")
    /** Database column deleted_by_guid  */
    val deletedByGuid: Column[Option[UUID]] = column[Option[UUID]]("deleted_by_guid")
    /** Database column deleted_at  */
    val deletedAt: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("deleted_at")

    /** Uniqueness Index over (name) (database name queries_name_not_deleted_un_idx) */
    val index1 = index("queries_name_not_deleted_un_idx", name, unique = true)
  }

  /** Collection-like TableQuery object for table Queries */
  lazy val Queries = new TableQuery(tag => new QueriesTable(tag))

  /** Entity class storing rows of table Teams
    * @param id Database column id AutoInc, PrimaryKey
    * @param organizationId Database column organization_id
    * @param name Database column name
    * @param createdByGuid Database column created_by_guid
    * @param createdAt Database column created_at
    * @param updatedByGuid Database column updated_by_guid
    * @param updatedAt Database column updated_at
    * @param deletedByGuid Database column deleted_by_guid
    * @param deletedAt Database column deleted_at  */
  case class TeamsRow(id: Long, organizationId: Long, name: String, cluster: Option[String], createdByGuid: UUID, createdAt: java.sql.Timestamp, updatedByGuid: UUID, updatedAt: java.sql.Timestamp, deletedByGuid: Option[UUID], deletedAt: Option[java.sql.Timestamp])

  /** Table description of table teams. Objects of this class serve as prototypes for rows in queries. */
  class TeamsTable(tag: Tag) extends Table[TeamsRow](tag, "teams") {
    def * = (id, organizationId, name, cluster, createdByGuid, createdAt, updatedByGuid, updatedAt, deletedByGuid, deletedAt) <>(TeamsRow.tupled, TeamsRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, organizationId.?, name.?, cluster, createdByGuid.?, createdAt.?, updatedByGuid.?, updatedAt.?, deletedByGuid, deletedAt).shaped.<>({ r => import r._; _1.map(_ => TeamsRow.tupled((_1.get, _2.get, _3.get, _4, _5.get, _6.get, _7.get, _8.get, _9, _10)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column organization_id  */
    val organizationId: Column[Long] = column[Long]("organization_id")
    /** Database column name  */
    val name: Column[String] = column[String]("name")
    /** Influx cluster name, if overriden  */
    val cluster: Column[Option[String]] = column[Option[String]]("cluster")
    /** Database column created_by_guid  */
    val createdByGuid: Column[UUID] = column[UUID]("created_by_guid")
    /** Database column created_at  */
    val createdAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")
    /** Database column updated_by_guid  */
    val updatedByGuid: Column[UUID] = column[UUID]("updated_by_guid")
    /** Database column updated_at  */
    val updatedAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("updated_at")
    /** Database column deleted_by_guid  */
    val deletedByGuid: Column[Option[UUID]] = column[Option[UUID]]("deleted_by_guid")
    /** Database column deleted_at  */
    val deletedAt: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("deleted_at")

    /** Foreign key referencing Organizations (database name team_organization_fk) */
    lazy val organizationsFk = foreignKey("team_organization_fk", organizationId, Organizations)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (name) (database name teams_name_not_deleted_un_idx) */
    val index1 = index("teams_name_not_deleted_un_idx", name, unique = true)
  }

  /** Collection-like TableQuery object for table Teams */
  lazy val Teams = new TableQuery(tag => new TeamsTable(tag))

  /** Entity class storing rows of table Tokens
    * @param id Database column id AutoInc, PrimaryKey
    * @param organizationId Database column organization_id
    * @param teamId Database column team_id
    * @param description Database column description
    * @param value Database column value
    * @param createdByGuid Database column created_by_guid
    * @param createdAt Database column created_at
    * @param updatedByGuid Database column updated_by_guid
    * @param updatedAt Database column updated_at
    * @param deletedByGuid Database column deleted_by_guid
    * @param deletedAt Database column deleted_at  */
  case class TokensRow(id: Long, organizationId: Long, teamId: Option[Long], description: String, value: String, createdByGuid: UUID, createdAt: java.sql.Timestamp, updatedByGuid: UUID, updatedAt: java.sql.Timestamp, deletedByGuid: Option[UUID], deletedAt: Option[java.sql.Timestamp])

  /** Table description of table tokens. Objects of this class serve as prototypes for rows in queries. */
  class TokensTable(tag: Tag) extends Table[TokensRow](tag, "tokens") {
    def * = (id, organizationId, teamId, description, value, createdByGuid, createdAt, updatedByGuid, updatedAt, deletedByGuid, deletedAt) <>(TokensRow.tupled, TokensRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, organizationId.?, teamId, description.?, value.?, createdByGuid.?, createdAt.?, updatedByGuid.?, updatedAt.?, deletedByGuid, deletedAt).shaped.<>({ r => import r._; _1.map(_ => TokensRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10, _11)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column organization_id  */
    val organizationId: Column[Long] = column[Long]("organization_id")
    /** Database column team_id  */
    val teamId: Column[Option[Long]] = column[Option[Long]]("team_id")
    /** Database column description  */
    val description: Column[String] = column[String]("description")
    /** Database column value  */
    val value: Column[String] = column[String]("value")
    /** Database column created_by_guid  */
    val createdByGuid: Column[UUID] = column[UUID]("created_by_guid")
    /** Database column created_at  */
    val createdAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")
    /** Database column updated_by_guid  */
    val updatedByGuid: Column[UUID] = column[UUID]("updated_by_guid")
    /** Database column updated_at  */
    val updatedAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("updated_at")
    /** Database column deleted_by_guid  */
    val deletedByGuid: Column[Option[UUID]] = column[Option[UUID]]("deleted_by_guid")
    /** Database column deleted_at  */
    val deletedAt: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("deleted_at")

    /** Foreign key referencing Organizations (database name token_organization_fk) */
    lazy val organizationsFk = foreignKey("token_organization_fk", organizationId, Organizations)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing Teams (database name token_team_fk) */
    lazy val teamsFk = foreignKey("token_team_fk", teamId, Teams)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }

  /** Collection-like TableQuery object for table Tokens */
  lazy val Tokens = new TableQuery(tag => new TokensTable(tag))

  case class UsersRow(id: Long,
                      firstName: String,
                      lastName: String,
                      email: String,
                      password: String,
                      salt: Option[String])

  class UsersTable(tag: Tag) extends Table[UsersRow](tag, "users") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def email = column[String]("email")
    def password = column[String]("password")
    def salt = column[Option[String]]("salt")

    def * = (id, firstName, lastName, email, password, salt) <>(UsersRow.tupled, UsersRow.unapply)
  }

  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new UsersTable(tag))

  /** Entity class storing rows of table OrganizationUsers
    * @param id              The ID of this row
    * @param organizationId  The ID of the organization
    * @param userId          The ID of the user
    * @param role            The role the user plays in this Organization
    */
  case class OrganizationUsersRow(id: Long, organizationId: Long, userId: Long, role: String)

  /** Table description of table organization_users. Objects of this class serve as prototypes for rows in queries. */
  class OrganizationUsersTable(tag: Tag) extends Table[OrganizationUsersRow](tag, "organization_users") {
    def * = (id, organizationId, userId, role) <>(OrganizationUsersRow.tupled, OrganizationUsersRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, organizationId.?, userId.?, role.?).shaped.<>({ r => import r._; _1.map(_ => OrganizationUsersRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    val organizationId: Column[Long] = column[Long]("organization_id")
    val userId: Column[Long] = column[Long]("user_id")
    val role: Column[String] = column[String]("role")

    /** Foreign key referencing Organizations (database name organization_user_organization_fk) */
    lazy val organizationsFk = foreignKey("organization_user_organization_fk", organizationId, Organizations)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Foreign key referencing Organizations (database name organization_user_user_fk) */
    lazy val usersFk = foreignKey("organization_user_user_fk", userId, Users)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (organizationId, userId) (database name organization_users_organization_id_user_id_un_idx) */
    val index1 = index("organization_users_organization_id_user_id_un_idx", (organizationId, userId), unique = true)
  }

  /** Collection-like TableQuery object for table OrganizationUsers */
  lazy val OrganizationUsers = new TableQuery(tag => new OrganizationUsersTable(tag))


  /** Entity class storing rows of table TeamUsers
    * @param id              The ID of this row
    * @param teamId          The ID of the team
    * @param userId          The ID of the user
    * @param role            The role the user plays in this Team
    */
  case class TeamUsersRow(id: Long, teamId: Long, userId: Long, role: String)

  /** Table description of table team_users. Objects of this class serve as prototypes for rows in queries. */
  class TeamUsersTable(tag: Tag) extends Table[TeamUsersRow](tag, "team_users") {
    def * = (id, teamId, userId, role) <>(TeamUsersRow.tupled, TeamUsersRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, teamId.?, userId.?, role.?).shaped.<>({ r => import r._; _1.map(_ => TeamUsersRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    val teamId: Column[Long] = column[Long]("team_id")
    val userId: Column[Long] = column[Long]("user_id")
    val role: Column[String] = column[String]("role")

    /** Foreign key referencing Organizations (database name team_user_team_fk) */
    lazy val teamsFk = foreignKey("team_user_team_fk", teamId, Teams)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Foreign key referencing Organizations (database name team_user_user_fk) */
    lazy val usersFk = foreignKey("team_user_user_fk", userId, Users)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (teamId, userId) (database name team_users_team_id_user_id_un_idx) */
    val index1 = index("team_users_team_id_user_id_un_idx", (teamId, userId), unique = true)
  }

  /** Collection-like TableQuery object for table OrganizationUsers */
  lazy val TeamUsers = new TableQuery(tag => new TeamUsersTable(tag))


  /**
   * A session token is an authorization string that a user obtains through login and uses for subsequent calls
   *
   * @param id                the row id
   * @param userId            the user this token belongs to
   * @param token             the actual token
   * @param creationTime      when the token was created
   * @param expirationTime    when the token will expire
   */
  case class SessionTokensRow(id: Long, userId: Long, token: String, creationTime: java.sql.Timestamp, expirationTime: java.sql.Timestamp)

  /** Table description of table session_tokens. Objects of this class serve as prototypes for rows in queries. */
  class SessionTokensTable(tag: Tag) extends Table[SessionTokensRow](tag, "session_tokens") {
    def * = (id, userId, token, creationTime, expirationTime) <>(SessionTokensRow.tupled, SessionTokensRow.unapply)

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id  */
    val userId: Column[Long] = column[Long]("user_id")
    /** Database column token  */
    val token: Column[String] = column[String]("token")
    /** Database column creation_time  */
    val creationTime: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("creation_time")
    /** Database column expiration_time  */
    val expirationTime: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("expiration_time")

    /** Foreign key referencing Users (database name session_tokens_users_fk) */
    lazy val usersFk = foreignKey("session_tokens_users_fk", userId, Users)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }

  /** Collection-like TableQuery object for table Tokens */
  lazy val SessionTokens = new TableQuery(tag => new SessionTokensTable(tag))


  /**
   * A confirmation token is the authorization string sent to an email address for confirmation.
   *
   * @param id                the row id
   * @param uuid              the actual token
   * @param email             the email where the token is sent
   * @param creationTime      when the token was created
   * @param expirationTime    when the token will expire
   * @param isSignUp          true if this token is for a sign up; false otherwise
   */
  case class ConfirmationTokensRow(id: Long, uuid: String, email: String, creationTime: java.sql.Timestamp, expirationTime: java.sql.Timestamp, isSignUp: Boolean)

  /** Table description of table confirmation_tokens. Objects of this class serve as prototypes for rows in queries. */
  class ConfirmationTokensTable(tag: Tag) extends Table[ConfirmationTokensRow](tag, "confirmation_tokens") {
    def * = (id, uuid, email, creationTime, expirationTime, isSignUp) <>(ConfirmationTokensRow.tupled, ConfirmationTokensRow.unapply)

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column uuid  */
    val uuid: Column[String] = column[String]("uuid")
    /** Database column email  */
    val email: Column[String] = column[String]("email")
    /** Database column creation_time  */
    val creationTime: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("creation_time")
    /** Database column expiration_time  */
    val expirationTime: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("expiration_time")
    /** Database column is_sign_up  */
    val isSignUp: Column[Boolean] = column[Boolean]("is_sign_up")
  }

  /** Collection-like TableQuery object for table Tokens */
  lazy val ConfirmationTokens = new TableQuery(tag => new ConfirmationTokensTable(tag))

  /** Entity class storing rows of table Schedulers
    * @param id Database column id AutoInc, PrimaryKey
    * @param name Database column description
    * @param createdAt Database column created_at
    */
  case class SchedulersRow(id: Long, name: String, createdAt: java.sql.Timestamp)

  implicit val getSchedulersResult = GetResult(r => new SchedulersRow(r.nextInt(), r.nextString(), r.nextTimestamp()))

  /** Table description of table tokens. Objects of this class serve as prototypes for rows in queries. */
  class SchedulersTable(tag: Tag) extends Table[SchedulersRow](tag, "schedulers") {
    def * = (id, name, createdAt) <>(SchedulersRow.tupled, SchedulersRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name, createdAt).shaped.<>({ r => import r._; _1.map(_ => SchedulersRow.tupled((_1.get, _2, _3)))}, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    val name: Column[String] = column[String]("name")
    /** Database column created_at  */
    val createdAt: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("created_at")
  }

  /** Collection-like TableQuery object for table Schedulers */
  lazy val Schedulers = new TableQuery(tag => new SchedulersTable(tag))

  case class StatusRow(id: Long, description: String, since: java.sql.Timestamp, until: Option[java.sql.Timestamp])

  class StatusTable(tag: Tag) extends Table[StatusRow](tag, "status") {
    def * = (id, description, since, until) <>(StatusRow.tupled, StatusRow.unapply)

    val id: Column[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    val description: Column[String] = column[String]("description")
    val since: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("since")
    val until: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("until")
  }

  lazy val StatusData = new TableQuery(tag => new StatusTable(tag))
}