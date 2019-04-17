package se.kth.id2203.overlay

import se.kth.id2203.networking.NetAddress
import se.sics.kompics.sl._;
import se.sics.kompics.KompicsEvent

object Routing extends Port {
  request[RouteMsg];
}

case class UpdateLut(leader: Option[NetAddress], range: (Int,Int), n: Long) extends KompicsEvent;

