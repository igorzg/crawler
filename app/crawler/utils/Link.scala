package crawler.utils

import java.net.{MalformedURLException, URL}


object Link {
  val AnchorPattern = """(?s)(<a.*?>)""".r
  val NoFollow = """.*\brel=['"]?nofollow['"]?.*""".r
  val HRef = """.*\bhref\s*=\s*(?:"([^"\s]+)"|'([^'\s]+)'|([^"'\s]+)).*""".r
  val ValidURL = """[\w\d:#@%/;$()~_?\+-=\\\.&]+""".r
  val Fragment = """#.*""".r
  val Protocol = """https?""".r

  def getPath(url: URL) = {
    val path = url.getPath
    if ("" equals path)
      "/"
    else path
  }

  def extract(document: String, baseURL: URL): Seq[URL] = {
    val response = AnchorPattern.findAllIn(document).flatMap {
      case NoFollow() => None
      case HRef(url1, url2, url3) =>
        val url =
          if (url1 != null) url1
          else if (url2 != null) url2
          else url3
        url match {
          case ValidURL() =>
            val trimmedUrl = Fragment.replaceFirstIn(url, "")
            try {
              val computedUrl = new URL(baseURL, trimmedUrl)
              if (Protocol.findFirstIn(computedUrl.getProtocol).isDefined) {
                Some(computedUrl)
              } else
                None
            } catch {
              case e: MalformedURLException => None
            }
          case _ => None
        }
      case _ => None
    }
    response.toList.filter(
      url => baseURL.getHost.equals(url.getHost)
    )
  }

  def domain(url: String): String = new URL(url).getHost
  def baseUrl(url: String): String = baseUrl(new URL(url))
  def baseUrl(url: URL): String = {
    val port = url.getPort
    val builder = new StringBuilder(url.getProtocol)
    builder.append("://")
    builder.append(url.getHost)
    if (port != -1 && port != url.getDefaultPort) {
      builder.append(":")
      builder.append(port.toString)
    }
    builder.toString
  }

}