package crawler.actors

import java.net.{URL, URLEncoder}
import java.nio.charset.StandardCharsets

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import crawler.actors.entities._
import crawler.utils.RobotsExclusion
import org.apache.http.client.utils.URLEncodedUtils
import play.Logger
import scala.collection._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.collection.JavaConverters._

class JobActor(dataStoreActor: ActorRef,
               statsActor: ActorRef,
               clientActor: ActorRef,
               robots: RobotsExclusion)(implicit mt: ActorMaterializer) extends Actor {
  private val logger = Logger.of(classOf[JobActor])

  var active = Seq.empty[String]
  var pending = Seq.empty[Task]
  var crawled = Seq.empty[String]
  var pathsCrawled = 0
  var stopping = false
  var notified = false
  var scheduled = false


  override def receive: PartialFunction[Any, Unit] = {
    case StartJob(task: Task) =>
      logger.debug("Starting job {} robots {}", task.url, robots.forbidden.isEmpty.toString)
      doCrawl(task, 0)

    case Stop(task: Task) =>
      logger.info("Received Stop signal for {}", task.url)
      stopping = true
      logger.debug(
        "Active {}, Crawled {}, Pending {} Iterations {}",
        active.size.toString,
        crawled.size.toString,
        pending.size.toString,
        pathsCrawled.toString
      )
      active = Seq.empty
      pending = Seq.empty
      crawled = Seq.empty
      context.stop(self)
    case CrawlResult(cTask, status, duration, bytes, tasks, headers, body) =>
      val task = stripOtherQueries(cTask)
      statsActor ! CrawlResult(task, status, duration, bytes, tasks, headers, body)
      dataStoreActor ! CrawlResult(task, status, duration, bytes, tasks, headers, body)
      active = active.filterNot(i => i.equals(task.url))
      if (crawled.contains(task.url)) {
        logger.warn("URL: {} was already crawled", task.url)
      }
      crawled = crawled :+ task.url
      pathsCrawled += 1
      logger.debug("CrawlResult {} status {} duration {} pending {} active {}",
        task.url,
        status.toString,
        duration.toString,
        pending.size.toString,
        active.size.toString
      )

      pending = pending ++ tasks.map(stripOtherQueries).filterNot(fTask => {
        pending.exists(_.url == fTask.url) || active.exists(_.equals(fTask.url)) || !shouldCrawl(fTask)
      })
      statsActor ! CrawlPending(pending.size)
      doPending(task)
  }

  /**
    * Do pending
    */
  def doPending(nsTask: Task): Unit = {
    val task = stripOtherQueries(nsTask)
    var done = false
    var i = pending.size
    logger.debug("pending {} active {}", pending.size.toString, active.size.toString)
    while (!done && pending.nonEmpty) {
      val hTask = pending.head
      pending = pending.filterNot(_ equals hTask)
      if (active.size < hTask.config.get.concurrency.get) {
        val shouldSphereCrawl = shouldCrawl(hTask)
        logger.debug("{} shouldCrawl {} active {}", hTask.url, shouldSphereCrawl.toString, active.size.toString)
        if (shouldSphereCrawl) {
          doCrawl(hTask, i)
          i -= 1
        }
      } else {
        done = true
        pending = pending :+ hTask
      }
    }
    if (active.isEmpty && pending.isEmpty) {
      if (stopping) {
        context.stop(self)
      } else if (!notified) {
        logger.info("Notifying manager that site crawl for %s is finished" format task.url)
        context.parent ! CrawlFinished(task)
        notified = true
      }
    }
  }

  def doCrawl(nsTask: Task, nIndex: Int) {
    val task = stripOtherQueries(nsTask)
    var index = nIndex
    active = active :+ task.url
    if (task.config.get.throttle.get > 0) {
      implicit val ec: ExecutionContext = context.dispatcher
      val config = task.config.get
      val throttle = config.throttle.get
      if (config.withIndexThrottle.isDefined && !config.withIndexThrottle.get) {
        index = 1
      }
      logger.debug("Throttle %s exclude %s in throttle %d ms index %d th %d" format(task.url, task.exclude.toString, throttle * index, index, throttle))
      context.system.scheduler.scheduleOnce(
        (throttle * index) millis,
        () => {
          if (!stopping) {
            logger.debug("Do throttled request {}", task.url)
            clientActor ! CrawlRequest(task)
          }
        }
      )
    } else {
      logger.debug("Crawling %s exclude %s" format(task.url, task.exclude.toString))
      clientActor ! CrawlRequest(task)
    }
  }

  def stripOtherQueries(task: Task): Task = {
    val config = task.config.get
    if (config.withStripOtherQueries.isDefined && config.withStripOtherQueries.get && task.include.isDefined) {
      val url = new URL(task.url)
      val pattern = task.include.get.find(i => i.startsWith(url.getPath))
      if (pattern.isDefined) {
        val params = URLEncodedUtils.parse(url.getQuery, StandardCharsets.UTF_8).asScala
        val pUrl = new URL(new URL(task.url), pattern.get)
        val keys = URLEncodedUtils.parse(pUrl.getQuery, StandardCharsets.UTF_8).asScala.map(i => i.getName)
        val paramsMap = params
          .filter(i => keys.contains(i.getName))
          .map(i =>
            i.getName + "=" + URLEncoder.encode(i.getValue, StandardCharsets.UTF_8)
          )
        if (keys.size.equals(paramsMap.size)) {
          task.copy(
            url = Uri(task.url).withRawQueryString(paramsMap.mkString("&")).toString()
          )
        } else task
      } else task
    } else task
  }

  def shouldCrawl(task: Task): Boolean = {
    val url = new URL(task.url)
    var isSameDomain = true
    if (task.parent.isDefined) {
      val parent = new URL(task.parent.get)
      isSameDomain = parent.getHost.equals(url.getHost)
    }
    val allowed = isForbidden(url.getPath)
    val isIncluded = shouldInclude(task.include, url)
    val isExcluded = shouldExclude(task.exclude, url)
    val isActive = active.exists(_.equals(task.url))
    val isCrawled = crawled.contains(task.url)
    !isActive && !isCrawled && allowed && isIncluded && !isExcluded && isSameDomain
  }

  def shouldExclude(paths: Option[Seq[String]], cUrl: URL): Boolean = {
    if (paths.isDefined && paths.nonEmpty) {
      paths.exists(_.startsWith(cUrl.getPath))
    } else false
  }

  def shouldInclude(paths: Option[Seq[String]], cUrl: URL): Boolean = {
    if (paths.isDefined && paths.nonEmpty) {
      var urlMap = paths.get.map(i => new URL(cUrl, i))
      var isIncluded = false
      while (!isIncluded && urlMap.nonEmpty) {
        val hUrl = urlMap.head
        urlMap = urlMap.filterNot(_ equals hUrl)
        val k2 = URLEncodedUtils.parse(hUrl.getQuery, StandardCharsets.UTF_8).asScala.map(i => i.getName -> i.getValue).toMap

        if (k2.isEmpty) {
          isIncluded = cUrl.getPath.startsWith(hUrl.getPath)
        } else {
          val k1 = URLEncodedUtils.parse(cUrl.getQuery, StandardCharsets.UTF_8).asScala
            .map(i => i.getName -> i.getValue)
            .toMap
            .filter(t => {
              if (k2.get(t._1).nonEmpty) {
                val value = k2(t._1)
                value.equals(t._2) || value.equals("*")
              } else false
            })
          val isMatchingKeys = k1.size.equals(k2.size)
          isIncluded = isMatchingKeys && cUrl.getPath.startsWith(hUrl.getPath)
        }
      }
      isIncluded
    } else true
  }

  def isForbidden(path: String): Boolean = {
    val p = if (path.isEmpty) "/" else path
    !robots.forbidden.exists(p.startsWith)
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    logger.error("Actor was restarted", reason)
    logger.debug("Restarted actor message {}", message.toString)
    super.preRestart(reason, message)
  }
}
