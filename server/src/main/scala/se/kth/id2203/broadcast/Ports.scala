package se.kth.id2203.broadcast

import se.kth.id2203.networking._;
import se.sics.kompics.KompicsEvent
import se.sics.kompics.network._
import se.sics.kompics.sl._




case class PL_Deliver(src: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class PL_Send(dest: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class PL_Forward(src: NetAddress, dest: NetAddress, payload: KompicsEvent) extends KompicsEvent;

object PerfectLink extends Port {
  indication[PL_Deliver];
  request[PL_Send];
  request[PL_Forward];
}

case class Suspect(src: NetAddress) extends KompicsEvent;
case class Restore(src: NetAddress) extends KompicsEvent;

object EventuallyPerfectFailureDetector extends Port {
  indication[Suspect];
  indication[Restore];
}

case class BEB_Deliver(src: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class BEB_Broadcast_All(payload: KompicsEvent) extends KompicsEvent;
case class BEB_Broadcast_Group(payload: KompicsEvent) extends KompicsEvent;

object BestEffortBroadcast extends Port {
  indication[BEB_Deliver];
  request[BEB_Broadcast_All];
  request[BEB_Broadcast_Group];
}

case class RB_Deliver(src: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class RB_Broadcast(payload: KompicsEvent) extends KompicsEvent;

object ReliableBroadcast extends Port {
  indication[RB_Deliver];
  request[RB_Broadcast];
}

case class CRB_Deliver(src: NetAddress, payload: KompicsEvent) extends KompicsEvent;
case class CRB_Broadcast(payload: KompicsEvent) extends KompicsEvent;

object CausalOrderReliableBroadcast extends Port {
  indication[CRB_Deliver];
  request[CRB_Broadcast];
}


case class AR_Read_Request() extends KompicsEvent
case class AR_Read_Response(value: Option[Any]) extends KompicsEvent
case class AR_Write_Request(value: Any) extends KompicsEvent
case class AR_Write_Response() extends KompicsEvent

object AtomicRegister extends Port {
  request[AR_Read_Request]
  request[AR_Write_Request]
  indication[AR_Read_Response]
  indication[AR_Write_Response]
}

case class C_Decide(value: Any) extends KompicsEvent;
case class C_Propose(value: Any) extends KompicsEvent;

object Consensus extends Port{
  request[C_Propose];
  indication[C_Decide];
}
