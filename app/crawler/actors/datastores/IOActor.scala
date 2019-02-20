package crawler.actors.datastores

import java.nio.file.{Files, Path, Paths}

import akka.actor.Actor
import crawler.datastores.entities.Crawler
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.Logger
import play.api.Configuration

class IOActor(configuration: Configuration) extends Actor {

  private val logger = Logger.of(classOf[IOActor])

  override def receive: PartialFunction[Any, Unit] = {
    case item: Crawler =>
      if (item.status.equals(200)) {
        val storagePath = configuration.get[String]("crawler.storage.path").split(":").toSeq
        val dir = System.getProperty("user.dir")
        val separator = System.getProperty("file.separator")
        val dirPath: Path = Paths.get(
          (Seq(dir) ++ storagePath ++ Seq("%s")).mkString(separator) format item.uid.toString
        ).normalize()
        Files.createDirectories(dirPath)
        val filePath = Paths.get(dirPath.toString, item.fileName)
        logger.debug("writing into current path {}", filePath.toString)
        val doc = parseBody(Some(item.body))
        if (doc.isDefined) {
          Files.write(filePath, doc.get.html().getBytes("UTF-8"))
        } else
          Files.write(filePath, item.body.getBytes("UTF-8"))
      }
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
}
