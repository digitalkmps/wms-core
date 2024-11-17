package wmscore

import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.LoggerOps
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.cluster.sharding.typed.scaladsl.ClusterSharding
import org.apache.pekko.cluster.sharding.typed.scaladsl.Entity
import org.apache.pekko.cluster.sharding.typed.scaladsl.EntityTypeKey
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize
//import com.fasterxml.jackson.databind.annotation.JsonSerialize

private[wmscore] object WmsStation {

  val TypeKey: EntityTypeKey[WmsStation.Command] =
    EntityTypeKey[WmsStation.Command]("WmsStation")

  def initSharding(system: ActorSystem[_]): Unit =
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      WmsStation(entityContext.entityId)
    })

  // actor commands and responses
  sealed trait Command extends CborSerializable

  final case class Record(data: Data, processingTimestamp: Long, replyTo: ActorRef[DataRecorded]) extends Command
  final case class DataRecorded(wsid: String) extends CborSerializable

  final case class Query(replyTo: ActorRef[QueryResult]) extends Command
  final case class QueryResult(wsid: String, readings: Int, value: Vector[Data]) extends CborSerializable
  /**
   * Simplified measurement data, actual data event comprises many more data points.
   *
   * @param eventTime unix timestamp when collected
   * @param temperature data value
   * @param humidity data value
   * @param distance data value
   * @param light data value
   */
  final case class Data(eventTime: Long, temperature: Double, humidity: Double, distance: Double, light: Boolean)

  def apply(wsid: String): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Starting weather station {}", wsid)

    running(context, wsid, Vector.empty)
  }

  private def running(context: ActorContext[Command], wsid: String, values: Vector[Data]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case Record(data, received, replyTo) =>
        val updated = values :+ data
        if (context.log.isDebugEnabled) {
          context.log.debugN(
            "{} total readings from station {}, diff: processingTime - eventTime: {} ms",
            updated.size,
            wsid,
            received - data.eventTime)
        }
        replyTo ! DataRecorded(wsid)
        running(context, wsid, updated) // store

      case Query(replyTo) =>
        val valuesForType = values
        val queryResult: Vector[Data] =
          if (valuesForType.isEmpty) Vector.empty
          else
            Vector(valuesForType.lastOption.get)

        replyTo ! QueryResult(wsid, valuesForType.size, queryResult)
        Behaviors.same
    }.receiveSignal {
      case (_, PostStop) =>
        context.log.info("Stopping, losing all recorded state for station {}", wsid)
        Behaviors.same
    }
}
