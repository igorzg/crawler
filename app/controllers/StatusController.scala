package controllers

import crawler.ServiceStatus
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}
/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  *
  * @author igorzg on 04.01.18.
  * @since 1.0
  */
@Singleton
class StatusController @Inject()(
                                  controllerComponents: ControllerComponents,
                                  configuration: Configuration
                                ) extends AbstractController(controllerComponents) {

  def index() = Action {
    Ok(ServiceStatus.asJson("Service is up an running", configuration))
  }


}