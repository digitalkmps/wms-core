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
 * 1. Receiving data from remote weather stations
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

  private def query(wsid: Long, dataType: WmsStation.DataType, function: WmsStation.Function)
      : Future[WmsStation.QueryResult] = {
    val ref = sharding.entityRefFor(WmsStation.TypeKey, wsid.toString)
    ref.ask(WmsStation.Query(dataType, function, _))
  }

  // unmarshallers for the query parameters
  private val funcsFromName =
    WmsStation.Function.All.map(function => function.toString.toLowerCase -> function).toMap
  private implicit val functionTypeUnmarshaller: Unmarshaller[String, WmsStation.Function] =
    Unmarshaller.strict[String, WmsStation.Function](text => funcsFromName(text.toLowerCase))

  private val dataTypesFromNames =
    WmsStation.DataType.All.map(dataType => dataType.toString.toLowerCase -> dataType).toMap
  private implicit val dataTypeUnmarshaller: Unmarshaller[String, WmsStation.DataType] =
    Unmarshaller.strict[String, WmsStation.DataType](text => dataTypesFromNames(text.toLowerCase))

  // imports needed for the routes and entity json marshalling
  import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import org.apache.pekko.http.scaladsl.server.Directives._
  import JsonFormats._

  val weather: Route =
    path("weather" / LongNumber) { wsid =>
      concat(
        get {
          parameters("type".as[WmsStation.DataType], "function".as[WmsStation.Function]) {
            (dataType, function) =>
              complete(query(wsid, dataType, function))
          }
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
