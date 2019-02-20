package crawler

import play.api.Configuration
import play.api.libs.json.{JsValue, Json, OFormat}

/**
  * @author igorzg on 2019-01-16.
  * @since 1.0
  */
case class ServiceStatus(message: String,
                         service: Option[String],
                         environment: Option[String],
                         version: Option[String])

object ServiceStatus {

  implicit val serviceStatusFormat: OFormat[ServiceStatus] = Json.format[ServiceStatus]

  def apply(message: String, config: Configuration): ServiceStatus = {
    ServiceStatus(
      message,
      config.getOptional[String]("service"),
      config.getOptional[String]("environment"),
      config.getOptional[String]("version")
    )
  }

  def asJson(message: String, config: Configuration): JsValue = Json.toJson(ServiceStatus(message, config))


  def apply(message: String): ServiceStatus = {
    ServiceStatus(
      message,
      None,
      None,
      None
    )
  }

  def asJson(message: String): JsValue = Json.toJson(ServiceStatus(message))


  def asJson(message: String, data: JsValue): JsValue = Json.obj(
    "message" -> message,
    "entity" -> data
  )
}