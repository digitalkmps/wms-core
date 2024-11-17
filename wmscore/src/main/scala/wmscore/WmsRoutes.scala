package wmscore

import scala.concurrent.Future
import scala.concurrent.duration._
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.ClusterSharding
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller
import org.apache.pekko.util.Timeout

/**
 * HTTP API for
 * 1. Receiving data from remote stations
 * 2. Receiving and responding to queries
 */
private[wmscore] final class WmsRoutes(system: ActorSystem[_]) {

  private val sharding = ClusterSharding(system)

  // timeout used for asking the actor
  private implicit val timeout: Timeout =
    system.settings.config.getDuration("wmscore.routes.ask-timeout").toMillis.millis

  private def recordData(wsid: Long, data: WmsStation.Data): Future[WmsStation.DataRecorded] = {
    val ref = sharding.entityRefFor(WmsStation.TypeKey, wsid.toString)
    ref.ask(WmsStation.Record(data, System.currentTimeMillis, _))
  }

  private def query(wsid: Long)
      : Future[WmsStation.QueryResult] = {
    val ref = sharding.entityRefFor(WmsStation.TypeKey, wsid.toString)
    ref.ask(replyTo => WmsStation.Query(replyTo))
  }

  // imports needed for the routes and entity json marshalling
  import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import org.apache.pekko.http.scaladsl.server.Directives._
  import JsonFormats._

  val weather: Route =
    path("data" / LongNumber) { wsid =>
      concat(
        get {
          complete(query(wsid))
        },
        post {
          entity(as[WmsStation.Data]) { data =>
            onSuccess(recordData(wsid, data)) { performed =>
              complete(StatusCodes.Accepted -> s"$performed from event time: ${data.eventTime}")
            }
          }
        })
    }

}
