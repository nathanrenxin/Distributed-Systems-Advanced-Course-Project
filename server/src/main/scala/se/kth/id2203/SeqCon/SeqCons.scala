package se.kth.id2203.paxos;


import se.kth.id2203.networking._;
import se.sics.kompics.KompicsEvent
import se.sics.kompics.network._
import se.sics.kompics.sl._
import se.kth.id2203.broadcast._
import se.kth.id2203.kvstore._;


object SeqCons extends Port{
  request[SC_Propose]
  indication[SC_Decide]
}

case class SC_Propose(value: RSM) extends KompicsEvent;
case class SC_Decide(value: RSM, leader: NetAddress) extends KompicsEvent;
