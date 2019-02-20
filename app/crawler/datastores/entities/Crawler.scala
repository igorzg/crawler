package crawler.datastores.entities

import java.util.UUID

case class Crawler(uid: UUID,
                   created: Long,
                   url: String,
                   host: String,
                   links: Seq[String],
                   status: Int,
                   duration: Long,
                   size: Long,
                   isHttps: Boolean,
                   contentType: String,
                   description: String,
                   title: String,
                   index: Boolean,
                   follow: Boolean,
                   text: String,
                   body: String,
                   fileName: String)
