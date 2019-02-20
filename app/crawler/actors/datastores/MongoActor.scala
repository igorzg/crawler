package crawler.actors.datastores

import akka.actor.Actor
import crawler.datastores.entities.Crawler
import crawler.datastores.services.CrawlerMongoService
import play.Logger
import reactivemongo.api.commands.WriteResult

import scala.concurrent.ExecutionContext

class MongoActor(crawlerMongoService: CrawlerMongoService) extends Actor {
  private val logger = Logger.of(classOf[MongoActor])
  override def receive: Receive = {
    case item: Crawler => {
      implicit val ex: ExecutionContext = context.dispatcher
      crawlerMongoService.insert(item)
        .map {
          result: WriteResult =>
            if (!result.ok) {
              for (error <- result.writeErrors) {
                logger.error("MongoDBError: code %d, index %d, %s" format(error.code, error.index, error.errmsg))
              }
            }

        }
        .recover {
          case e: Throwable => logger.error("MongoCollection error", e)
        }
    }
  }
}
