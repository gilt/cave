package filters


import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future

object HttpsAndWwwRedirectForElbFilter extends Filter {

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    //play uses lower case headers.
    requestHeader.headers.get("x-forwarded-proto") match {
      case Some(header) => {
        if ("https" == header) {
          if (doWwwRedirect(requestHeader))
            Future.successful(Results.Redirect("https://www." + requestHeader.host + requestHeader.uri, 301))
          else
            nextFilter(requestHeader).map { result =>
              result.withHeaders(("Strict-Transport-Security", "max-age=31536000"))
            }
        } else {
          Future.successful(Results.Redirect("https://" + requestHeader.host + requestHeader.uri, 301))
        }
      }
      case None => nextFilter(requestHeader)
    }
  }

  def doWwwRedirect(requestHeader: RequestHeader): Boolean = {
    val redirectsEnabled = Play.configuration.getBoolean("cave.enableWwwRedirect").getOrElse(false)
    val wwwRedirectEnabledDomain = Play.configuration.getString("cave.wwwRedirectEnabledDomain").getOrElse("cavellc.io")

    redirectsEnabled && requestHeader.host.startsWith(wwwRedirectEnabledDomain) && !requestHeader.host.startsWith("www.")
  }
}