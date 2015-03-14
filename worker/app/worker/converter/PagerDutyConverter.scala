package worker.converter

import com.cave.metrics.data.Check
import play.api.libs.json.{Json, JsValue}

class PagerDutyConverter extends CheckConverter {
  override def matchesUrl(url: String): Boolean = url.contains("pagerduty.com")

  override def convert(check: Check): Option[JsValue] =
    check.schedule.alert.routing.getOrElse(Map.empty[String, String]).get("pagerduty_service_api_key") map { key =>
      val org = s"organizations/${check.schedule.orgName}"
      val team = check.schedule.teamName map { name => s"/teams/$name" } getOrElse ""

      Json.obj(
        "service_key" -> key,
        "incident_key" -> check.schedule.alert.id.get,
        "event_type" -> "trigger",
        "description" -> check.schedule.alert.description,
        "client" -> "CAVE",
        "client_url" -> s"https://www.cavellc.io/$org$team/alerts/${check.schedule.alert.id.get}",
        "details" -> Json.toJson(check)
      )
    }
}
