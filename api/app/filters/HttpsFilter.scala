package filters

import play.api.mvc.{Filter, RequestHeader, Result, Results}

import scala.concurrent.Future

object HttpsFilter extends Filter {

  override def apply(nextFilter: (RequestHeader) => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] =

    //play uses lower case headers
    requestHeader.headers.get("x-forwarded-proto") match {
      case Some(header) if header == "http" =>
        Future.successful(Results.Forbidden("Send your requests to https://" + requestHeader.host))

      case _ => nextFilter(requestHeader)
    }
}
