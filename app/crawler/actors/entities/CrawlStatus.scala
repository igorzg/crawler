package crawler.actors.entities

trait CrawlStatus

case class CrawlHttpStatus(status: Int) extends CrawlStatus

case class SkippedContentType(contentType: String) extends CrawlStatus

case class CrawlTimeout() extends CrawlStatus

case class CrawlStatusError(status: Int) extends Exception

case class CrawlException(exception: Throwable) extends CrawlStatus
