package crawler.actors

import java.net.URL

import akka.actor.{Actor, ActorRef}
import crawler.actors.entities.{CrawlHttpStatus, CrawlResult, Task}
import crawler.datastores.entities.Crawler
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.Logger

class DataStoreActor(
                      ioActor: ActorRef,
                      elasticSearchActor: ActorRef,
                      mongoActor: ActorRef
                    ) extends Actor {
  private val logger = Logger.of(classOf[DataStoreActor])

  override def receive: PartialFunction[Any, Unit] = {
    case CrawlResult(task: Task, cs: CrawlHttpStatus, duration: Long, bytes: Long, tasks: Seq[Task], headers: Map[String, Seq[String]], body: Option[String]) =>
      val document = parseBody(body)
      val url = new URL(task.url)
      val fileName = """[/\.]+""".r.replaceAllIn(url.getHost + url.getPath, """_""")
      val crawler = Crawler(
        task.uid.get,
        System.currentTimeMillis,
        task.url,
        url.getHost,
        tasks.map(i => i.url),
        cs.status,
        duration,
        bytes,
        task.url.contains("https://"),
        getHeader(headers, "Content-Type", "application/octet-stream"),
        extractDescription(document, task.url),
        extractTitle(document, task.url),
        extractRobots(document, "noindex", task.url),
        extractRobots(document, "nofollow", task.url),
        pureText(document),
        body.getOrElse(""),
        fileName + ".html"
      )
      elasticSearchActor ! crawler
      mongoActor ! crawler
      ioActor ! crawler

  }

  def getHeader(headers: Map[String, Seq[String]], name: String, default: String): String = {
    if (headers.contains(name)) {
      headers(name).head
    } else default
  }

  def parseBody(response: Option[String]): Option[Document] = {
    if (response.isDefined) {
      try {
        val body = response.get
        Some(Jsoup.parse(body, "UTF-8"))
      } catch {
        case e: Throwable =>
          logger.error("Error parsing html doc", e)
          None
      }
    } else
      None
  }

  def pureText(doc: Option[Document]): String = {
    if (doc.isDefined) {
      try {
        val document = doc.get
        document.select("a").remove()
        return document.text()
      } catch {
        case e: Throwable =>
          logger.error("Cannot clean document", e)
      }

    }
    ""
  }

  def extractRobots(doc: Option[Document], value: String, url: String): Boolean = {
    if (doc.isDefined) {
      try {
        val robots = doc.get.select("meta[name=robots]").get(0).attr("content")
        if (robots.contains(value)) {
          return false
        } else {
          return true
        }
      } catch {
        case e: Throwable =>
          logger.info("No robots found on page {}", url)
      }
    }
    true
  }

  def extractDescription(doc: Option[Document], url: String): String = {
    if (doc.isDefined) {
      try {
        return doc.get.select("meta[name=description]").get(0).attr("content")
      } catch {
        case e: Throwable =>
          logger.warn("Error extracting meta description on page {}", url)
      }
    }
    ""
  }

  def extractTitle(doc: Option[Document], url: String): String = {
    if (doc.isDefined) {
      try {
        return doc.get.select("head title").get(0).ownText()
      } catch {
        case e: Throwable =>
          logger.warn("Error extracting title on page {}", url)
      }
    }
    ""
  }
}
