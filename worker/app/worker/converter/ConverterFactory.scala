package worker.converter

import com.typesafe.config.Config
import scala.collection.JavaConverters._

class ConverterFactory(config: Config) {

  private[converter] val converters = config.getStringList("list").asScala map makeConverter
  private[converter] val default = makeConverter(config.getString("default"))

  private[this] def makeConverter(clazz: String) = Class.forName(clazz).newInstance().asInstanceOf[CheckConverter]


  def getConverter(url: String): CheckConverter = converters.find(_.matchesUrl(url)) getOrElse default
}
