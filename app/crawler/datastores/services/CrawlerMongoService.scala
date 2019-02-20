package crawler.datastores.services

import crawler.datastores.SphereMongo
import crawler.datastores.entities.Crawler
import javax.inject.Inject
import play.api.libs.json.{Json, OFormat}
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}




class CrawlerMongoService @Inject()(sphereMongo: SphereMongo) {

  private lazy val collection: JSONCollection = Await.result(sphereMongo.collection("crawler"), 5 seconds)

  implicit val crawlerFormat: OFormat[Crawler] = Json.format[Crawler]

  def insert(item: Crawler)(implicit ex: ExecutionContext): Future[WriteResult] = collection.insert.one(item)
}