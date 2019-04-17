package se.kth.id2203.broadcast


import se.kth.id2203.bootstrapping._
import se.kth.id2203.broadcast._
import se.kth.id2203.networking._
import se.kth.id2203.overlay.LookupTable
import se.sics.kompics.network._
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{KompicsEvent, ComponentDefinition => _, Port => _}

import scala.collection.immutable.Set
import scala.collection.mutable.ListBuffer


class BasicBroadcast extends ComponentDefinition {

  //subscriptions
  val beb = provides(BestEffortBroadcast);
  val pLink = requires(PerfectLink);
  val boot = requires(Bootstrapping);

  //configuration
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  var group = Set.empty[NetAddress];
  var allNodes =Set.empty[NetAddress];
  //handlers

  boot uponEvent {
    case BootedB(assignment: LookupTable) => handle {
      log.info("Got NodeAssignment, preload KV store.");
      val lut = assignment;
      for (range<-lut.partitions.keySet){
        if (lut.partitions(range).contains(self)){
          group ++= lut.partitions(range);
        }
        allNodes ++= lut.partitions(range);
      }
    }
  }

  beb uponEvent {
    case x: BEB_Broadcast_All => handle {
     for (p <- allNodes) {
         trigger(PL_Send(p, x) -> pLink);
     }
    }
    case x: BEB_Broadcast_Group => handle {
      for (p <- group) {
        trigger(PL_Send(p, x) -> pLink);
      }
    }
  }

  pLink uponEvent {
    case PL_Deliver(src, BEB_Broadcast_All(payload)) => handle {
     trigger(BEB_Deliver(src, payload) -> beb);
    }
    case PL_Deliver(src, BEB_Broadcast_Group(payload)) => handle {
      trigger(BEB_Deliver(src, payload) -> beb);
    }
  }
}
