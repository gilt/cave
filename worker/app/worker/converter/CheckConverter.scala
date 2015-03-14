package worker.converter

import com.cave.metrics.data.Check
import play.api.libs.json.JsValue

trait CheckConverter {
  def matchesUrl(url: String): Boolean
  def convert(check: Check): Option[JsValue]
}
