import com.google.inject.AbstractModule
import crawler.datastores.SphereMongo
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}

/**
  * @author igorzg on 2019-01-16.
  * @since 1.0
  */
class Application(environment: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    bind(classOf[SphereMongo]).asEagerSingleton()
  }

}
