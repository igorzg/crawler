package crawler.utils

case class RobotsExclusion(forbidden: Seq[String])

object RobotsExclusion {
  val CommentPattern = """#.*""".r
  val Disallow = """Disallow:\s*(.*?)\s*""".r
  val UserAgent = """User\-agent:\s*(.*?)\s*""".r

  def apply(text: String, agent: String): RobotsExclusion = {
    var capture = true
    new RobotsExclusion(text.split("\n") flatMap { l =>
      CommentPattern.replaceFirstIn(l, "") match {
        case UserAgent(ua: String) =>
          capture = ua.equals("*") || ua.contains(agent)
          None
        case Disallow(path) =>
          if (capture)
            if (path.equals("")) None else Some(path)
          else
            None
        case _ => None
      }
    })
  }
}