package crawler.actors.entities

case class CrawlStatistics(total: Int,
                           success: Int,
                           failed: Int,
                           ignored: Int,
                           redirect: Int,
                           timeout: Int,
                           duration: Long,
                           running: Long,
                           bytes: Long,
                           pendingSites: Int)