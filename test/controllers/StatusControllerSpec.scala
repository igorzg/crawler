package controllers

import java.io.File

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.{Configuration, Environment}
import play.api.test._
import play.api.test.Helpers._


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class StatusControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "HomeController GET" should {

    "render the index page from a new instance of controller" in {
      val file = new File("conf/application.conf")
      val controller = new StatusController(stubControllerComponents(), Configuration.load(Environment.simple(file)))
      val home = controller.index().apply(FakeRequest(GET, "/"))

      status(home) mustBe OK
      contentType(home) mustBe Some("application/json")
    }
  }
}
