package controllers

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

case class IssueData(description: String, since: DateTime, until: Option[DateTime])

class Status extends AbstractCaveController {

  def status = caveAsyncAction { implicit request =>
    withCaveClient { client =>
      client.Statuses.getStatus() map {
        case Some(status) =>
          val currentIssues = status.current.map(issue => IssueData(issue.description, issue.since, issue.until))
          val recentIssues = status.recent.map(issue => IssueData(issue.description, issue.since, issue.until))
          Ok(views.html.status.statusFragment(currentIssues, recentIssues))
        case None =>
          Ok(views.html.status.statusFragment(Seq.empty, Seq.empty))
      }
    }
  }
}
