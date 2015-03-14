package worker.converter

import com.cave.metrics.data.Check
import play.api.libs.json.{Json, JsValue}

class JsonConverter extends CheckConverter {
  override def convert(check: Check): Option[JsValue] = Some(Json.toJson(check))
  override def matchesUrl(url: String): Boolean = true
}
