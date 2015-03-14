package data

import com.cave.metrics.data._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.http.HeaderNames.AUTHORIZATION

trait TestJsonData {

  val BaseUrl = "example.com"
  /**
   * An organization for testing the API
   */
  val GiltName = "gilt"
  val GiltEmail = "cave@gilt.com"
  val GiltNotificationUrl = "https://notifications.gilt.com/alert"

  val GiltUpdatedEmail = "new.cave@gilt.com"
  val GiltUpdatedNotificationUrl = "https://new-notifications.gilt.com/alert"

  val GiltOrgTokenDescription = "gilt-token"
  val GiltOrgTokenId = "Gilt12345"
  val GiltOrgToken = tokenWithId(GiltOrgTokenId, Token.createToken(GiltOrgTokenDescription))
  val GiltOrgTokenJson = toJson(GiltOrgToken)
  val GiltOrgTokenLocation = s"$BaseUrl/organizations/$GiltName/tokens/${GiltOrgToken.id.get}"
  val GiltOrgAuth = makeAuth(GiltOrgToken)
  val GiltOrgBasicAuth = makeBasicAuth(GiltOrgToken)
  val GiltOrgBadAuth = makeBadAuth(GiltOrgToken)

  val GiltOrg = new Organization(None, GiltName, GiltEmail, GiltNotificationUrl, Some(List(GiltOrgToken)))
  val GiltOrgJson = toJson(GiltOrg)
  val GiltOrgLocation = s"$BaseUrl/organizations/$GiltName"

  val GiltUpdatedOrg = new Organization(None, GiltName, GiltUpdatedEmail, GiltUpdatedNotificationUrl, Some(List(GiltOrgToken)))
  val GiltUpdatedOrgJson = toJson(GiltUpdatedOrg)
  val GiltSameUrlOrg = new Organization(None, GiltName, GiltUpdatedEmail, GiltNotificationUrl, Some(List(GiltOrgToken)))
  val GiltSameUrlOrgJson = toJson(GiltSameUrlOrg)

  val GiltTeamName = "customer-tx"
  val GiltTeamTokenDescription = "gilt-team-token"
  val GiltTeamTokenId = "GiltCTX12345"
  val GiltTeamToken = tokenWithId(GiltTeamTokenId, Token.createToken(GiltTeamTokenDescription))
  val GiltTeamTokenJson = toJson(GiltTeamToken)
  val GiltTeamTokenLocation = s"$BaseUrl/organizations/$GiltName/teams/$GiltTeamName/tokens/${GiltTeamToken.id.get}"
  val GiltTeamAuth = makeAuth(GiltTeamToken)
  val GiltTeamBasicAuth = makeBasicAuth(GiltTeamToken)
  val GiltTeamBadAuth = makeBadAuth(GiltTeamToken)

  val GiltTeam = new Team(None, GiltTeamName, Some(List(GiltTeamToken)))
  val GiltTeamJson = toJson(GiltTeam)
  val GiltTeamLocation = s"$BaseUrl/organizations/$GiltName/teams/$GiltTeamName"

  final val ResponseTime5Mp99    = "response-time [service: svc-important, env: production].p99.5m"
  final val ResponseTimeDailyAvg = "response-time [service: svc-important, env: production].mean.1d"

  final val AlertId = "12345"
  final val AlertDescription = "ResponseTime.p99.too.high"
  final val AlertEnabled = true
  final val AlertPeriod = "5m"
  final val AlertCondition = s"$ResponseTime5Mp99 > $ResponseTimeDailyAvg"
  final val AlertHandbookUrl = "https://www.alerts.com/1"
  final val AlertRouting = Map("key" -> "Something extra")

  val SomeAlert = Alert(Some(AlertId), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting))
  val SomeAlert2 = Alert(Some(AlertId + 1), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting))
  val SomeAlert3 = Alert(Some(AlertId + 2), AlertDescription, AlertEnabled, AlertPeriod, AlertCondition, Some(AlertHandbookUrl), Some(AlertRouting))

  val GiltOrgAlertLocation = s"$BaseUrl/organizations/$GiltName/alerts/$AlertId"
  val GiltTeamAlertLocation = s"$BaseUrl/organizations/$GiltName/teams/$GiltTeamName/alerts/$AlertId"

  val SomeNewAlertJson = Json.obj(
    "description" -> AlertDescription,
    "enabled" -> AlertEnabled,
    "period" -> AlertPeriod,
    "condition" -> AlertCondition,
    "handbook_url" -> AlertHandbookUrl,
    "routing" -> AlertRouting
  )

  val SomeOldAlertJson = Json.obj(
    "id" -> AlertId,
    "description" -> AlertDescription,
    "enabled" -> AlertEnabled,
    "period" -> AlertPeriod,
    "condition" -> AlertCondition,
    "handbook_url" -> AlertHandbookUrl,
    "routing" -> AlertRouting
  )

  final val OtherAlertId = "54321"
  final val OtherAlertDescription = "ResponseTime.p99.too.low"
  final val OtherAlertEnabled = false
  final val OtherAlertPeriod = "15m"
  final val OtherAlertCondition = s"$ResponseTime5Mp99 < $ResponseTimeDailyAvg"
  final val OtherAlertHandbookUrl = "https://www.alerts.com/2"
  final val OtherAlertRouting = Map("key" -> "Something else too")

  val SomeOtherAlert = Alert(Some(OtherAlertId), OtherAlertDescription, OtherAlertEnabled, OtherAlertPeriod, OtherAlertCondition, Some(OtherAlertHandbookUrl), Some(OtherAlertRouting))

  val SomeOtherAlertJson = Json.obj(
    "id" -> OtherAlertId,
    "description" -> OtherAlertDescription,
    "enabled" -> OtherAlertEnabled,
    "period" -> OtherAlertPeriod,
    "condition" -> OtherAlertCondition,
    "handbook_url" -> OtherAlertHandbookUrl,
    "routing" -> OtherAlertRouting
  )

  val OtherAlertDescriptionJson = Json.obj(
    "description" -> OtherAlertDescription
  )
  val OtherAlertEnabledJson = Json.obj(
    "enabled" -> OtherAlertEnabled
  )
  val OtherAlertPeriodJson = Json.obj(
    "period" -> OtherAlertPeriod
  )
  val OtherAlertConditionJson = Json.obj(
    "condition" -> OtherAlertCondition
  )
  val OtherAlertHandbookUrlJson = Json.obj(
    "handbook_url" -> OtherAlertHandbookUrl
  )
  val OtherAlertRoutingJson = Json.obj(
    "routing" -> OtherAlertRouting
  )

  val SomeIncompleteAlertJson = Json.obj(
    "description" -> AlertDescription,
    "enabled" -> AlertEnabled,
    "condition" -> AlertCondition,
    "handbook_url" -> AlertHandbookUrl,
    "routing" -> AlertRouting
  )
  val ErrorIncompleteAlertJson = "Cannot parse alert configuration: List((/period,List(ValidationError(error.path.missing,WrappedArray()))))"

  /**
   * Another organization for testing the API
   */
  val TestName = "test"
  val TestEmail = "cave@test.com"
  val TestNotificationUrl = "http://www.mytest.com/alert"

  val TestOrgTokenDescription = "test-token"
  val TestOrgTokenId = "Test12345"
  val TestOrgToken = tokenWithId(TestOrgTokenId, Token.createToken(TestOrgTokenDescription))
  val TestOrgTokenJson = toJson(TestOrgToken)
  val TestOrgAuth = makeAuth(TestOrgToken)
  val TestOrgBasicAuth = makeBasicAuth(TestOrgToken)
  val TestOrgBadAuth = makeBadAuth(TestOrgToken)
  val TestOrgToken2 = Token.createToken("test-org-token2")
  val TestOrgToken3 = Token.createToken("test-org-token3")

  val TestTeamName = "back-office"
  val TestTeamTokenId = "TestBO12345"
  val TestTeamToken = tokenWithId(TestTeamTokenId, Token.createToken("test-team-token"))
  val TestTeamTokenJson = toJson(TestTeamToken)
  val TestTeamAuth = makeAuth(TestTeamToken)
  val TestTeamBasicAuth = makeBasicAuth(TestTeamToken)
  val TestTeamBadAuth = makeBadAuth(TestTeamToken)

  val TestOrg = new Organization(None, TestName, TestEmail, TestNotificationUrl, Some(List(TestOrgToken, TestOrgToken2, TestOrgToken3)))
  val TestOrgJson = toJson(TestOrg)

  val TestTeamToken2 = Token.createToken("test-team-token2")
  val TestTeamToken3 = Token.createToken("test-team-token3")
  val TestTeam = new Team(None, TestTeamName, Some(List(TestTeamToken, TestTeamToken2, TestTeamToken3)))
  val TestTeamJson = toJson(TestTeam)

  /**
   * Invalid JSON and Error messages
   */
  val BadSyntaxJson = """{ no json here }"""

  val InvalidOrgGiltMissingTokens = Json.stringify(Json.obj(
      "name" -> GiltName,
      "email" -> GiltEmail,
      "notification_url" -> GiltNotificationUrl
  ))

  val ErrorParsingOrgJson = "Cannot parse JSON as Organization."
  val ErrorParsingTeamJson = "Cannot parse JSON as Team."

  val BadName = "Team Awesome"
  val InvalidOrgBadName = Json.stringify(Json.obj(
    "name" -> BadName,
    "email" -> GiltEmail,
    "notification_url" -> GiltNotificationUrl
  ))
  val InvalidTeamBadName = Json.stringify(Json.obj(
    "name" -> BadName
  ))

  val InvalidOrgGiltMissingEmail = Json.stringify(Json.obj(
    "name" -> GiltName,
    "moo_url" -> GiltNotificationUrl
  ))

  private[this] def makeAuth(token: Token) = s"Bearer ${token.value}"
  private[this] def makeBasicAuth(token: Token) =
    "Basic " + new sun.misc.BASE64Encoder().encodeBuffer((token.value + ":").getBytes("UTF-8"))
  def makeBadAuth(token: Token) = s"Digest ${token.value}"

  private[this] def toJson(token: Token) = Json.stringify(Json.toJson(token))

  private[this] def toJson(team: Team) = Json.stringify(Json.obj(
      "name" -> team.name,
      "tokens" -> Json.toJson(team.tokens)
    ))

  private[this] def toJson(org: Organization) = Json.stringify(Json.obj(
      "name" -> org.name,
      "email" -> org.email,
      "notification_url" -> org.notificationUrl,
      "tokens" -> Json.toJson(org.tokens)
    ))

  protected def descriptionJson(description: String): String = s"""{"description": "$description"}"""
  protected def teamNameJson(name: String): String = s"""{"name": "$name"}"""

  protected def createOrgJson(name: String, email: String, notificationUrl: String): String =  Json.stringify(Json.obj(
      "name" -> name,
      "email" -> email,
      "notification_url" -> notificationUrl
    ))

  protected def updateOrgJson(email: Option[String], notificationUrl: Option[String]): JsValue =
    Json.toJson(OrganizationPatch(email, notificationUrl))

  private def tokenWithId(id: String, token: Token) =
    Token(Some(id), token.description, token.value, token.created)
}
