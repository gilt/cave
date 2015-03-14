package worker.web

import java.util.concurrent.Executor

import com.cave.metrics.data.Check
import com.ning.http.client.AsyncHttpClient
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import play.api.libs.json.{JsValue, Json}
import worker.converter.ConverterFactory

import scala.concurrent._

trait NotificationSender {
  def send(notification: Check)(implicit exec: Executor): Future[Boolean]
  def shutdown(): Unit
}

case class BadStatus(status: Int) extends RuntimeException

class AsyncNotificationSender(converterFactory: ConverterFactory) extends NotificationSender {

  private val client = new AsyncHttpClient

  override def send(notification: Check)(implicit exec: Executor): Future[Boolean] = {

    def sendJson(url: String, json: JsValue): Future[Boolean] = {
      val f = client.preparePost(url)
        .addHeader(CONTENT_TYPE, "application/json")
        .setBody(Json.stringify(json))
        .execute()

      val p = Promise[Boolean]()

      f.addListener(new Runnable {

        override def run(): Unit = {
          val response = f.get
          if (response.getStatusCode < 400) p.success(true)
          else p.failure(BadStatus(response.getStatusCode))
        }
      }, exec)
      p.future
    }

    val url = notification.schedule.notificationUrl
    converterFactory.getConverter(url).convert(notification) match {
      case Some(json) =>
        println("JSON: " + json)
        sendJson(url, json)
      case None =>
        println("Failed to convert notification to JSON. Entity was " + notification)
        Future.successful(false)
    }
  }

  override def shutdown(): Unit = client.close()
}