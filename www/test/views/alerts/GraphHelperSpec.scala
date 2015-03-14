package views.alerts

import org.scalatest.{FunSpec, Matchers}

import com.gilt.cavellc.models.Aggregator.Sum
import com.gilt.cavellc.models.AlertMetric

/**
 *
 * Author: jkenny
 * Date: 04/03/2015
 */
class GraphHelperSpec extends FunSpec with Matchers {

  describe("generateLink") {
    it("should generate the correct link if there is no team and no aggregator, period or tags") {
      val expected = "/organizations/myOrg/metrics/graph/my_metric?aggregator=count&period=1&tags="
      GraphHelper.generateLink("myOrg", None, AlertMetric("my_metric", Map.empty[String, String], None, None)) should be(expected)
    }

    it("should generate the correct link if there is an aggregator, period and tags") {
      val expected = "/organizations/myOrg/metrics/graph/my_metric?aggregator=sum&period=5&tags=foo:bar,boom:bang"
      GraphHelper.generateLink("myOrg", None, AlertMetric("my_metric", Map("foo" -> "bar", "boom" -> "bang"), Some(Sum), Some(300))) should be(expected)
    }

    it("should generate a link even if the period isn't an exact match") {
      val expected = "/organizations/myOrg/metrics/graph/my_metric?aggregator=sum&period=5&tags=foo:bar,boom:bang"
      GraphHelper.generateLink("myOrg", None, AlertMetric("my_metric", Map("foo" -> "bar", "boom" -> "bang"), Some(Sum), Some(280))) should be(expected)
    }

    it("should generate a link even if the period is far too big") {
      val expected = "/organizations/myOrg/metrics/graph/my_metric?aggregator=sum&period=1440&tags=foo:bar,boom:bang"
      GraphHelper.generateLink("myOrg", None, AlertMetric("my_metric", Map("foo" -> "bar", "boom" -> "bang"), Some(Sum), Some(100000))) should be(expected)
    }
  }
}
