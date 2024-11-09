package wmscore

import scala.util.{ Failure, Success }
import scala.concurrent.duration._

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.{ actor => classic, Done }
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route

private[wmscore] object WmsHttpServer {

  /**
   * Logic to bind the given routes to a HTTP port and add some logging around it
   */
  def start(routes: Route, port: Int, system: ActorSystem[_]): Unit = {
    import org.apache.pekko.actor.typed.scaladsl.adapter._
    implicit val classicSystem: classic.ActorSystem = system.toClassic
    val shutdown = CoordinatedShutdown(classicSystem)

    import system.executionContext

    Http().bindAndHandle(routes, "0.0.0.0", port).onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "WeatherServer online at http://{}:{}/",
          address.getHostString,
          address.getPort)

        shutdown.addTask(
          CoordinatedShutdown.PhaseServiceRequestsDone,
          "http-graceful-terminate") { () =>
          binding.terminate(10.seconds).map { _ =>
            system.log.info(
              "WeatherServer http://{}:{}/ graceful shutdown completed",
              address.getHostString,
              address.getPort)
            Done
          }
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

}
