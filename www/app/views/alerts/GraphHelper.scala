package views.alerts

import com.gilt.cavellc.models.AlertMetric
import scala.collection.SortedMap

/**
 *
 * Author: jkenny
 * Date: 04/03/2015
 */
object GraphHelper {

  val ValidPeriods = SortedMap(
    1 -> "1m", 5 -> "5m", 10 -> "10m", 30 -> "30m",
    1 * 60 -> "1h", 3 * 60 -> "3h", 6 * 60 -> "6h", 12 * 60 -> "12h", 24 * 60 -> "24h"
  )

  def generateLink(organizationName: String, teamName: Option[String], alertMetric: AlertMetric): String = {
    val teamUrl = teamName.fold("")(t => s"/teams/$t")

    // Default to 'count' if there is no aggregator
    val aggregatorQuery = alertMetric.aggregator.fold("count")(_.toString)

    // Find the best match from the valid periods - default to 1 if there is none provided
    val periodMinutes = alertMetric.periodSeconds.fold(1.0)(_ / 60)
    val periodQuery = ValidPeriods.collectFirst {
      case (value, _) if value >= periodMinutes => value
    }.getOrElse(ValidPeriods.lastKey)

    val tagsQuery = alertMetric.tags.map(t => t._1 + ":" + t._2).mkString(",")

    s"/organizations/$organizationName$teamUrl/metrics/graph/${alertMetric.name}?aggregator=$aggregatorQuery&period=$periodQuery&tags=$tagsQuery"
  }
}
