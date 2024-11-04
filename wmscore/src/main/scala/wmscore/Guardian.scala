package wmscore

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

/**
 * Root actor bootstrapping the application
 */
object Guardian {

  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    WmsStation.initSharding(context.system)

    val routes = new WmsRoutes(context.system)
    WmsHttpServer.start(routes.weather, httpPort, context.system)

    Behaviors.empty
  }

}
