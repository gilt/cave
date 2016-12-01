package worker.converter

import com.typesafe.config.Config
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import scala.collection.JavaConverters._

class ConverterFactorySpec extends FlatSpec with MockitoSugar with Matchers {

  val mockConfig = mock[Config]

  "A converter factory" should "build from configuration" in {
    when(mockConfig.getStringList("list")).thenReturn(List("worker.converter.PagerDutyConverter").asJava)
    when(mockConfig.getString("default")).thenReturn("worker.converter.JsonConverter")

    val converterFactory = new ConverterFactory(mockConfig)

    converterFactory.converters.toList.size should be(1)
    converterFactory.converters.head.isInstanceOf[PagerDutyConverter] should be(true)
    converterFactory.default.isInstanceOf[JsonConverter] should be(true)
  }
}
