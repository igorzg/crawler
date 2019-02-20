package crawler.actors.datastores

import akka.actor.Actor
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import crawler.datastores.entities.Crawler
import play.Logger
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ElasticSearchActor(wsClient: WSClient, configuration: Configuration)(implicit mt: ActorMaterializer) extends Actor {

  private val logger = Logger.of(classOf[ElasticSearchActor])
  implicit val crawlerFormat: OFormat[Crawler] = Json.format[Crawler]
  implicit val ec: ExecutionContext = context.dispatcher

  case class SphereReindex(index: String, item: SphereIndexMapping)

  case class SphereIndexMapping(v1: Option[JsValue], v2: Option[JsValue]) {
    def getCurrentMapping(index: String): JsValue = {
      if (v1.isDefined && hasAlias(v1.get, index)) {
        (v1.get \ "mappings").as[JsValue]
      } else if (v2.isDefined && hasAlias(v2.get, index)) {
        (v2.get \ "mappings").as[JsValue]
      } else JsNull
    }

    def shouldReindex(index: String): Boolean = {
      if (v1.isDefined && hasAlias(v1.get, index)) {
        true
      } else if (v2.isDefined && hasAlias(v2.get, index)) {
        true
      } else false
    }

    def whoNeedsToBeUpdated(index: String): String = {
      if (v1.isDefined && hasAlias(v1.get, index)) {
        "v1"
      } else if (v2.isDefined && hasAlias(v2.get, index)) {
        "v2"
      } else "v1"
    }

    def whoNeedsToBeMapped(index: String): String = {
      if (v1.isDefined && hasAlias(v1.get, index)) {
        "v2"
      } else "v1"
    }

    private def hasAlias(value: JsValue, index: String): Boolean = {
      try {
        ((value \ "aliases").as[JsValue] \ index).asOpt[JsValue].isDefined
      } catch {
        case e: Throwable => false
      }
    }
  }

  private def loadMappingFile(name: String): JsValue = {
    try {
      Json.parse(
        scala.io.Source.fromInputStream(
          classOf[ElasticSearchActor].getClassLoader.getResourceAsStream("es/" + name)
        ).mkString
      )
    } catch {
      case e: Throwable =>
        logger.error("Cannot parse mapping file", e)
        JsNull
    }
  }

  private def getIndexMapping(index: String, response: WSResponse): Option[JsValue] = {
    try {
      (response.json \ index).asOpt[JsValue]
    } catch {
      case e: Throwable =>
        logger.info("Can't find index mapping {}", e.getCause.toString)
        None
    }
  }

  private def getMappings(index: String): Future[SphereIndexMapping] = {
    val sphereIndexSink: Sink[WSResponse, Future[SphereIndexMapping]] = Sink.fold[SphereIndexMapping, WSResponse](
      SphereIndexMapping(None, None)
    )(
      (mapping, response) => {
        mapping.copy(
          if (mapping.v1.isDefined) {
            mapping.v1
          } else getIndexMapping(index + "_v1", response),
          if (mapping.v2.isDefined) {
            mapping.v2
          } else getIndexMapping(index + "_v2", response)
        )
      }
    )
    val v1 = Source.fromFuture(client("/" + index + "_v1").get).runWith(sphereIndexSink)
    val v2 = Source.fromFuture(client("/" + index + "_v2").get).runWith(sphereIndexSink)
    v1.zip(v2).map {
      case (m1: SphereIndexMapping, m2: SphereIndexMapping) =>
        m1.copy(
          m1.v1,
          m2.v2
        )
    }
  }

  private def swapAliases(nIndex: String, alias: String, oIndex: Option[String] = None): Future[WSResponse] = {
    var actions: List[JsValue] = List(
      Json.obj(
        "add" -> Json.obj(
          "index" -> nIndex,
          "alias" -> alias
        )
      )
    )
    if (oIndex.isDefined) {
      actions = actions :+ Json.obj(
        "remove" -> Json.obj(
          "index" -> oIndex.get,
          "alias" -> alias
        )
      )
    }
    logger.debug("Aliases {}", actions)
    client("/_aliases").post(Json.obj(
      "actions" -> Json.toJson(actions)
    ))
  }

  def areMappingsEqual(nMapping: JsValue, oMapping: JsValue): Boolean = (oMapping \ "mappings").as[JsValue].equals(nMapping)

  private def createIndex(index: String) = {
    val mapping: JsValue = loadMappingFile(index + ".json")
    getMappings(index)
      .map {
        item: SphereIndexMapping =>
          val nIndex = index + "_" + item.whoNeedsToBeMapped(index)
          val reReindexFrom = index + "_" + item.whoNeedsToBeUpdated(index)
          val shouldReindex = item.shouldReindex(index)
          val cMapping = item.getCurrentMapping(index)
          if (cMapping.equals(JsNull)) {
            logger.warn("Processing mapping to index {}, shouldReindex {}, newAlias {}", nIndex, shouldReindex.toString, reReindexFrom)
            client("/" + nIndex).put(mapping)
              .map {
                response: WSResponse =>
                  logger.warn("Creating index {} status {} body {}", nIndex, response.status.toString, response.body)
                  if (response.status.equals(200)) {
                    swapAliases(nIndex, index, None)
                      .map {
                        response: WSResponse =>
                          logger.warn("Adding alias {} to index {} status {} body {}", nIndex, index, response.status.toString, response.body)
                      }
                  }
              }
          } else if (!areMappingsEqual(cMapping, mapping)) {
            logger.warn("Processing mapping to index {}, shouldReindex {}, newAlias {}", nIndex, shouldReindex.toString, reReindexFrom)
            client("/" + nIndex).delete()
              .flatMap {
                response: WSResponse =>
                  logger.warn("Deleting old index {} status {} body {}", nIndex, response.status.toString, response.body)
                  client("/" + nIndex).put(mapping)
              }
              .map {
                response: WSResponse =>
                  logger.warn("Creating index {} status {} body {}", nIndex, response.status.toString, response.body)
                  if (shouldReindex && response.status.equals(200)) {
                    client("/_reindex").post(
                      Json.obj(
                        "source" -> Json.obj("index" -> reReindexFrom),
                        "dest" -> Json.obj("index" -> nIndex)
                      )
                    )
                      .map {
                        reindexResponse: WSResponse =>
                          logger.warn(
                            "Do reindex from {}, into {}, status {} body {}",
                            reReindexFrom,
                            nIndex,
                            reindexResponse.status.toString,
                            reindexResponse.body
                          )
                          self ! SphereReindex(index, item)
                      }
                  }
              }
          } else {
            logger.warn("Mappings are equal no change required!")
          }
      }
  }


  context.system.scheduler.scheduleOnce(0 millis, new Runnable {
    override def run(): Unit = createIndex("crawler")
  })

  private def client(suffix: String): WSRequest = wsClient.url(configuration.get[String]("es.connection") + suffix)

  private def index(item: Crawler): Future[WSResponse] = client("/crawler/_doc/").post(
    (Json.toJson(item).as[JsObject] - "text") - "body"
  )


  override def receive: PartialFunction[Any, Unit] = {
    // @Todo finish reindex feedback as long poling status info and update alias once reindexing is done
    // until reindexing is done we show old copy
    case SphereReindex(index: String, mapping: SphereIndexMapping) =>
      val cMapping = mapping.getCurrentMapping(index)
      val nIndex = index + "_" + mapping.whoNeedsToBeMapped(index)
      val reReindexFrom = index + "_" + mapping.whoNeedsToBeUpdated(index)
      swapAliases(nIndex, index, Some(reReindexFrom))

    case item: Crawler =>
      index(item)
        .map {
          response => {
            if (response.status >= 400) {
              logger.error(response.body)
            }
          }
        }
  }


}
