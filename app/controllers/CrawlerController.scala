package controllers


import akka.actor.{Actor, ActorRef, ActorSystem, DeadLetter, Props}
import akka.stream.ActorMaterializer
import crawler.ServiceStatus
import crawler.actors.entities.{CrawlStatisticsRequest, StopJob, Task}
import crawler.actors._
import crawler.actors.datastores.{ElasticSearchActor, IOActor, MongoActor}
import crawler.datastores.services.CrawlerMongoService
import javax.inject.{Inject, Singleton}
import play.Logger
import play.api.Configuration
import play.api.libs.json.{JsNull, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class CrawlerController @Inject()(system: ActorSystem,
                                  controllerComponents: ControllerComponents,
                                  crawlerMongoService: CrawlerMongoService,
                                  client: WSClient,
                                  configuration: Configuration) extends AbstractController(controllerComponents) {

  private val logger = Logger.of(classOf[CrawlerController])

  implicit val mt: ActorMaterializer = ActorMaterializer()(system)

  implicit val ec: ExecutionContext = system.dispatcher

  lazy val elasticSearchActor: ActorRef = system.actorOf(
    Props(
      new ElasticSearchActor(
        client,
        configuration
      )
    )
      .withDispatcher("akka.dispatchers.elasticsearch"),
    "elasticsearch"
  )


  lazy val mongoActorRef: ActorRef = system.actorOf(
    Props(
      new MongoActor(
        crawlerMongoService
      )
    )
      .withDispatcher("akka.dispatchers.mongo"),
    "mongo"
  )

   lazy val ioActorRef: ActorRef = system.actorOf(
    Props(
      new IOActor(configuration)
    )
      .withDispatcher("akka.dispatchers.io"),
    "io"
  )

  lazy val crawlerStoreActor: ActorRef = system.actorOf(
    Props(
      new DataStoreActor(
        ioActorRef,
        elasticSearchActor,
        mongoActorRef
      )
    )
      .withDispatcher("akka.dispatchers.datastore"),
    "datastore"
  )


  lazy val clientActorRef: ActorRef = system.actorOf(
    Props(
      new ClientActor(
        client,
        configuration
      )
    )
      .withDispatcher("akka.dispatchers.client"),
    "client"
  )

  lazy val schedulerRef: ActorRef = system.actorOf(
    Props(
      new SchedulerActor(
        crawlerStoreActor,
        clientActorRef,
        configuration
      )
    ).withDispatcher("akka.dispatchers.scheduler"),
    "scheduler"
  )


  system.scheduler.schedule(0 seconds, 5 seconds, schedulerRef, CrawlStatisticsRequest())


  def index(): Action[AnyContent] = Action { request =>
    val isDelete: Boolean = request.method == "DELETE"
    var message = "Schedule"
    if (isDelete) {
      message = "Kill"
    }
    request.body.asJson match {
      case Some(json) =>
        json.asOpt[List[Task]] match {
          case Some(tasks: List[Task]) =>
            tasks.foreach(task => if (isDelete) {
              schedulerRef ! StopJob(task)
            } else schedulerRef ! task)
            Ok(
              ServiceStatus.asJson(
                message + " crawler list job",
                Json.toJson(tasks)
              )
            )

          case None =>
            json.asOpt[Task] match {
              case Some(task: Task) =>
                if (isDelete) {
                  schedulerRef ! StopJob(task)
                } else {
                  schedulerRef ! task
                }
                Ok(
                  ServiceStatus.asJson(
                    message + " crawler job",
                    Json.toJson(task)
                  )
                )
              case None => BadRequest(ServiceStatus.asJson(
                "Json is not ScheduleJob instance",
                JsNull
              ))
            }
        }
      case _ => BadRequest(ServiceStatus.asJson("Invalid json provided", JsNull))
    }
  }


  val listener: ActorRef = system.actorOf(Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case deadLetter: DeadLetter => logger.error(
        "Dead letter send from: %s receiver: %s" format(deadLetter.sender.toString(), deadLetter.recipient.toString())
      )
    }
  }))

  system.eventStream.subscribe(listener, classOf[DeadLetter])
}
