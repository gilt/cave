import scala.concurrent.Future

import init.Init
import org.apache.commons.logging.LogFactory
import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  private[this] final val Log = LogFactory.getLog(this.getClass)

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error(ex.toString, ex)
    Future.successful(InternalServerError(ex.toString))
  }

  override def onStart(app: Application) {
    Init.init()
  }

  override def onStop(app: Application) {
    Init.shutdown()
  }
}

