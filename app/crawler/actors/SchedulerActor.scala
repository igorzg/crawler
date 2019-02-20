package crawler.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.stream.ActorMaterializer
import crawler.actors.entities._
import crawler.utils.RobotsExclusion
import play.Logger
import play.api.Configuration

import scala.collection.immutable.HashMap

class SchedulerActor(dataStoreActor: ActorRef,
                     clientActor: ActorRef,
                     configuration: Configuration)(implicit mt: ActorMaterializer) extends Actor {

  private val logger = Logger.of(classOf[SchedulerActor])

  private var jobs: HashMap[String, ActorJob] = HashMap.empty
  private var robots: HashMap[String, RobotsExclusion] = HashMap.empty

  override def receive: PartialFunction[Any, Unit] = {
    case task: Task =>
      jobs.get(task.domain()) match {
        case Some(job: ActorJob) =>
          logger.debug("Job in progress {}, include {}, exclude {} ", job.current.url, job.current.include.toString, job.current.exclude.toString)
        case None =>
          val nTask = Task.initDefaults(task, configuration)
          logger.warn(
            "Staring job using config: throttle {}, concurrency {}, withIndexThrottle {}, withStripOtherQueries {}",
            nTask.config.get.throttle.get.toString,
            nTask.config.get.concurrency.get.toString,
            nTask.config.get.withIndexThrottle.get.toString,
            nTask.config.get.withStripOtherQueries.get.toString
          )
          self ! StartJob(nTask)

      }

    case StopJob(task: Task) =>
      val url = task.domain()
      if (jobs.contains(url)) {
        logger.info("Stopping task for " + url)
        val job = jobs(url)
        jobs = jobs - url
        robots = robots - url
        job.actor ! Stop(task)
        job.stats ! Stop(task)
      } else {
        logger.info("Job {} is not scheduled", url)
      }

    case DownloadedRobots(task: Task, value: RobotsExclusion) =>
      robots = robots + (task.domain() -> value)
      startJobActor(task, value)

    case CrawlFinished(task: Task) =>
      logger.info("Finished site crawl for {}", task.domain())
      self ! StopJob(task)

    case StartJob(task: Task) =>
      val url = task.domain()
      robots.get(url) match {
        case Some(exclusion: RobotsExclusion) =>
          startJobActor(task, exclusion)
        case None =>
          clientActor ! FetchRobotsJob(task)
      }

    case CrawlStatisticsRequest() =>
      for ((_, job) <- jobs) {
        job.stats ! CrawlStatisticsRequest()
      }

  }

  def startJobActor(task: Task, robots: RobotsExclusion): Unit = {
    logger.info("Schedule job  %s with %s exclude" format(task.url, task.exclude.toString))
    if (robots.forbidden.isEmpty) {
      logger.warn("No robots.txt found on page {}", task.url)
    }
    val statsActor: ActorRef = context.actorOf(
      Props(new StatsActor()).withDispatcher("akka.dispatchers.statistics")
    )
    val jobActor: ActorRef = context.actorOf(
      Props(
        new JobActor(
          dataStoreActor,
          statsActor,
          clientActor,
          robots
        )
      ).withDispatcher("akka.dispatchers.crawler")
    )
    val url = task.domain()
    jobs = jobs + (url -> ActorJob(jobActor, statsActor, task))
    jobActor ! StartJob(task: Task)
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    logger.error("Scheduler got restarted due %s with message %s" format(reason, message))
    super.preRestart(reason, message)
  }


}
