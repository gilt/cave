import filters.HttpsAndWwwRedirectForElbFilter
import init.Init
import org.apache.commons.logging.LogFactory
import play.api._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

object Global extends WithFilters(HttpsAndWwwRedirectForElbFilter) with GlobalSettings {

  private[this] final val Log = LogFactory.getLog(this.getClass)

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(views.html.errorpages.pageNotFound(request.path)))
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error(ex.toString, ex)
    Future.successful(InternalServerError(views.html.errorpages.errorPage(ex.getMessage)))
  }

  override def onStart(app: Application) {
    Init.init
  }

  override def onStop(app: Application) {
    Init.shutdown()
  }
}
