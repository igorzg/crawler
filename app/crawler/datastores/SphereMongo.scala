package crawler.datastores

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SphereMongo @Inject()(applicationLifecycle: ApplicationLifecycle, configuration: Configuration)(implicit ex: ExecutionContext) {

  private lazy val driver = MongoDriver()

  private lazy val connection: MongoConnection = driver.connection(List(configuration.get[String]("mongo.connection")))

  applicationLifecycle.addStopHook(() => Future(driver.close()))

  def collection(name: String): Future[JSONCollection] = {
    connection.database(configuration.get[String]("mongo.dbname")).map(_.collection[JSONCollection](name))
  }
}
