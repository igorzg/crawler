package crawler.actors.entities

import java.util.UUID

import crawler.utils.Link
import play.api.Configuration
import play.api.libs.json._

object TaskType extends Enumeration {
  type TaskType = Value
  val DOWNLOAD, CRAWL = Value
}

case class Config(throttle: Option[Int] = Some(1000),
                  concurrency: Option[Int] = Some(2),
                  withIndexThrottle: Option[Boolean] = Some(false),
                  withStripOtherQueries: Option[Boolean] = Some(false))

case class Task(url: String,
                var include: Option[Seq[String]] = Some(Seq("/")),
                exclude: Option[Seq[String]] = None,
                var config: Option[Config],
                var taskType: Option[TaskType.TaskType] = Some(TaskType.CRAWL),
                var origin: Option[String] = None,
                var uid: Option[UUID] = Option(UUID.randomUUID()),
                var parent: Option[String] = None) {
  def baseUrl(): String = Link.baseUrl(url)

  def domain(): String = Link.domain(url)
}

object Task {
  implicit val taskTypeFormat = new Format[TaskType.TaskType] {
    def reads(json: JsValue) = JsSuccess(TaskType.withName(json.as[String]))

    def writes(myEnum: TaskType.TaskType) = JsString(myEnum.toString)
  }
  implicit val taskConfigFormat = Json.format[Config]
  implicit val taskFormat = Json.format[Task]

  def fromParentTask(url: String, c: Task): Task = Task(
    url, c.include, c.exclude, c.config, c.taskType, c.origin, c.uid, Some(c.url)
  )


  def initDefaults(task: Task, configuration: Configuration): Task = {
    if (task.config.isEmpty) {
      task.config = Some(
        Config(
          Some(configuration.get[Int]("crawler.throttle")),
          Some(configuration.get[Int]("crawler.jobs")),
          Some(false),
          Some(false)
        )
      )
    } else {
      task.uid = Some(UUID.randomUUID())
      var config = task.config.get
      if (config.throttle.isEmpty) {
        config = config.copy(
          throttle = Some(configuration.get[Int]("crawler.throttle"))
        )
      }
      if (config.concurrency.isEmpty) {
        config = config.copy(
          concurrency = Some(configuration.get[Int]("crawler.jobs"))
        )
      }
      if (config.withStripOtherQueries.isEmpty) {
        config = config.copy(
          withIndexThrottle = Some(false)
        )
      }
      if (config.withStripOtherQueries.isEmpty) {
        config = config.copy(
          withStripOtherQueries = Some(false)
        )
      }
      task.config = Some(config)
    }
    task
  }
}