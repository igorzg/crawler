package crawler.actors.entities

case class CrawlResult(task: Task,
                       status: CrawlStatus,
                       duration: Long,
                       bytes: Long,
                       tasks: Seq[Task],
                       headers: Map[String, Seq[String]],
                       response: Option[String] = None)
