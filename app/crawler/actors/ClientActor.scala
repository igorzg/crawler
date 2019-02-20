package crawler.actors


import java.net.URL

import akka.actor.{Actor, ActorRef}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import crawler.actors.entities._
import crawler.utils.{Link, RobotsExclusion}
import play.Logger
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}

class ClientActor(client: WSClient, configuration: Configuration)(implicit mt: ActorMaterializer) extends Actor {
  private val logger = Logger.of(classOf[ClientActor])

  private val headersConf = ConfigFactory.load("bots.conf")


  override def receive: PartialFunction[Any, Unit] = {
    case FetchRobotsJob(task: Task) =>
      val url = Link.baseUrl(task.url) + "/robots.txt"
      var value = new RobotsExclusion(Seq.empty)
      try {
        implicit val ec: ExecutionContext = context.dispatcher
        value = Await.result(client.url(url).get.map {
          response: WSResponse => RobotsExclusion(response.body, "SphereBot")
        }, 5 seconds)
      } catch {
        case e: Throwable =>
          logger.error("Cannot fetch robots.txt", e)
      }
      sender ! DownloadedRobots(task, value)

    case CrawlRequest(task: Task) => doCrawlRequest(sender, task)
  }


  case class ResponseDetails(body: String, headers: Map[String, Seq[String]], status: Int, size: Int, links: Seq[URL])

  private def doCrawlRequest(sender: ActorRef, task: Task) = {
    val start = System.currentTimeMillis()
    implicit val ec: ExecutionContext = context.dispatcher
    val lang = """.*\.([a-zA-z]+)$""".r.replaceAllIn(new URL(task.url).getHost, """$1""")
    var headers = Seq("User-Agent" -> headersConf.getString("user_agent"))
    headers = headers :+ "Accept" -> headersConf.getString("accept_header")
    headers = headers :+ "Accept-Encoding" -> headersConf.getString("accept_encoding")
    try {
      headers = headers :+ "Accept-Language" -> headersConf.getString(lang)
    } catch {
      case e: Throwable => headers = headers :+ "Accept-Language" -> headersConf.getString("en")
    }
    headers = headers :+ "Cache-Control" -> headersConf.getString("cache_control")
    try {
      client
        .url(task.url.toLowerCase)
        .addHttpHeaders(headers: _*)
        .withFollowRedirects(false)
        .get
        .map((response: WSResponse) => {
          val toLineByte = Flow[ByteString].map(bs => bs.utf8String)
          var byteSink: Sink[String, Future[ResponseDetails]] = Sink.fold[ResponseDetails, String](
            ResponseDetails("", response.headers, response.status, 0, Seq.empty))(
            (details, lines) => {
              details.copy(
                body = lines,
                links = details.links ++ Link.extract(lines, new URL(task.url)),
                size = details.size + lines.length
              )
            }
          )
          response.status match {
            case 301 | 302 | 307 | 308 =>
              val links = response.headers.get("Location").collect {
                case Seq(u: String) => Seq(new URL(new URL(task.url), u))
              }.getOrElse(Seq.empty[URL])
              byteSink = Sink.fold[ResponseDetails, String](ResponseDetails("", response.headers, response.status, 0, links))((details, links) => details)
            case status: Int if status >= 400 =>
              throw CrawlStatusError(status)
            case _ =>
          }
          val contentType: String = response.header("Content-Type").get
          val contentTypes = Seq("text/html", "application/xhtml+xml")
          if (!contentTypes.exists(p => contentType.startsWith(p))) {
            logger.debug("Ignoring {} as the content type is {}", task.url, contentType)
            throw UnsupportedContentType(contentType)
          }
          response.bodyAsSource.via(toLineByte).toMat(byteSink)(Keep.right).run
        })
        .map(_.map {
          details => {
            sender ! CrawlResult(
              task.copy(),
              CrawlHttpStatus(details.status),
              System.currentTimeMillis - start,
              details.size,
              details.links.map(u => Task.fromParentTask(u.toString, task.copy())),
              details.headers,
              Some(details.body)
            )
          }
        })
        .recover {
          case e: CrawlStatusError =>
            sender ! CrawlResult(task, CrawlHttpStatus(e.status), System.currentTimeMillis - start, 0, Seq.empty, Map.empty)
          case e: UnsupportedContentType =>
            sender ! CrawlResult(task, SkippedContentType(e.contentType), System.currentTimeMillis - start, 0, Seq.empty, Map.empty)
          case e: TimeoutException =>
            sender ! CrawlResult(task, CrawlTimeout(), System.currentTimeMillis - start, 0, Seq.empty, Map.empty)
          case e: Throwable =>
            sender ! CrawlResult(task, CrawlException(e), System.currentTimeMillis - start, 0, Seq.empty, Map.empty)
        }
    } catch {
      case e: Throwable =>
        sender ! CrawlResult(task, CrawlException(e), System.currentTimeMillis - start, 0, Seq.empty, Map.empty)
    }
  }
}
