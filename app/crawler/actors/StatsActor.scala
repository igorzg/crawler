package crawler.actors

import java.net.URL

import akka.actor.Actor
import crawler.actors.entities._
import play.Logger

class StatsActor extends Actor {
  private val logger = Logger.of(classOf[StatsActor])
  var total = 0
  var success = 0
  var failed = 0
  var ignored = 0
  var timeout = 0
  var redirect = 0
  var totalSites = 0
  var pendingSites = 0
  var duration: Long = 0
  var bytes: Long = 0
  var site: String = ""
  val start: Long = System.currentTimeMillis()
  var listeners = Seq.empty[String]
  var lastTick = CrawlStatistics(0, 0, 0, 0, 0, 0, 0, System.currentTimeMillis(), 0, 0)
  var stopping = false

  override def receive: PartialFunction[Any, Unit] = {
    case CrawlResult(task: Task, status, time, size, tasks: Seq[Task], headers: Map[String, Seq[String]], body: Option[String]) =>
      if (site.isEmpty) {
        val url = new URL(task.url)
        site = url.getProtocol + "://" + url.getHost
      }
      total += 1
      bytes += size
      duration += time
      status match {
        case CrawlHttpStatus(200 | 202 | 204) => success += 1
        case CrawlHttpStatus(301 | 302 | 307 | 308) =>
          logger.debug("url {} is redirected to {}", task.url, tasks.head.url)
          redirect += 1
        case CrawlHttpStatus(st) if st >= 400 =>
          failed += 1
          logger.error("Crawler HTTP error {} at {} \n from {}", st.toString, task.url, task.parent.getOrElse(""))
        case SkippedContentType(contentType) =>
          ignored += 1
          logger.info("Skipped content type {}", contentType)
        case CrawlTimeout() =>
          timeout += 1
          logger.error("Timout received {}", task.url)
        case _ =>
          failed += 1
          logger.error("Status needing handling {}", status)
      }
    case CrawlPending(size) =>
      pendingSites = size
    case Stop(task) =>
      logger.debug("Stopping stats actor {}", task.url)
      logger.debug(statsHtml)
      stopping = true
      context.stop(self)
    case CrawlStatisticsRequest() =>
      if (!stopping) {
        logger.debug("Received statistics request")
        logger.debug(statsHtml)
        lastTick = CrawlStatistics(
          total,
          success,
          failed,
          ignored,
          timeout,
          redirect,
          duration,
          System.currentTimeMillis,
          bytes,
          pendingSites
        )
      }
  }

  def statsHtml: String = {
    val fromStart = (System.currentTimeMillis() - start) / 1000.0
    val fromLast = (System.currentTimeMillis() - lastTick.running) / 1000.0
    val crawlsInPeriod = total - lastTick.total
    val bytesInPeriod = bytes - lastTick.bytes
    val durationInPeriod = duration - lastTick.duration
    ("Domain: " + site + " -> total sites: active %d, pending %d\ncrawls: total %d, success %d, failure %d, ignored %d, " +
      "redirect %d, timeout %d, running for %.2fs kB %.2f\n"
      format(totalSites, pendingSites, total, success, failed, ignored, redirect, timeout, fromStart, bytes / 1024.0)) +
      ("from start:  total %.2f 1/s, %.2f kBs, avg response %.2fms, avg page size %.2fkB\n"
        format(
        total / fromStart, bytes / fromStart / 1024.0,
        if (total != 0) duration / total / 1.0 else 0.0,
        if (total != 0) 1.0 * bytes / total else 0.0)) +
      ("last period: total %.2f 1/s, %.2f kBs, avg response %.2fms, avg page size %.2fkB"
        format(crawlsInPeriod / fromLast,
        bytesInPeriod / fromLast / 1024.0,
        if (crawlsInPeriod != 0) durationInPeriod / crawlsInPeriod / 1.0 else 0.0,
        if (crawlsInPeriod != 0) 1.0 * bytesInPeriod / crawlsInPeriod else 0.0))
  }
}
