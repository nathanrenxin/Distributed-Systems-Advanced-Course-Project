package se.kth.id2203.broadcast

import se.kth.id2203.broadcast.Ports._
import se.kth.id2203.networking._;
import se.kth.id2203.broadcast.BasicBroadcast
import se.sics.kompics.network._
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{ComponentDefinition=>_, Port=>_, KompicsEvent}
import scala.collection.immutable.Set
import scala.collection.mutable.ListBuffer


case class OriginatedData(src: NetAddress, payload: KompicsEvent) extends KompicsEvent;

class EagerReliableBroadcast extends ComponentDefinition {

  //EagerReliableBroadcast Subscriptions
  val beb = requires[BestEffortBroadcast];
  val rb = provides[ReliableBroadcast];

  //EagerReliableBroadcast Component State and Initialization
  val self = cfg.getValue[NetAddress]("id2203.project.address");

  var delivered = collection.mutable.Set[KompicsEvent]();

  //EagerReliableBroadcast Event Handlers
  rb uponEvent {
    case x@RB_Broadcast(payload) => handle {

      /* WRITE YOUR CODE HERE  */
      trigger(BEB_Broadcast(OriginatedData(self, payload)) -> beb);
    }
  }

  beb uponEvent {
    case BEB_Deliver(_, data@OriginatedData(origin, payload)) => handle {

      /* WRITE YOUR CODE HERE  */
      if(!delivered.contains(payload)){
        delivered=delivered+payload;
        trigger(RB_Deliver(origin, payload) -> rb);
        trigger(BEB_Broadcast(OriginatedData(origin, payload)) -> beb);
      }
    }
  }
}