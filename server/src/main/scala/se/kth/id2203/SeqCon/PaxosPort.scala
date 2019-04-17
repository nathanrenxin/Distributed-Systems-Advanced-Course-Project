package se.kth.id2203.paxos;

import java.util.UUID;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.network.Network;
import se.kth.id2203.networking._
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}

import se.kth.id2203.kvstore._;

trait RSM_Command
{
  def id: UUID;
  def src: NetAddress;
  def operation: Op;
  def isFastRead: Boolean;
}

@SerialVersionUID(0xfacc6612da2139eaL)
case class RSM(src: NetAddress, operation: Op, isFastRead: Boolean = false, id: UUID = UUID.randomUUID()) extends RSM_Command with Serializable {
}

case class Prepare(nL: Long, ld: Int, na: Long) extends KompicsEvent;
case class Promise(nL: Long, na: Long, suffix: List[RSM], ld: Int) extends KompicsEvent;
case class AcceptSync(nL: Long, suffix: List[RSM], ld: Int) extends KompicsEvent;
case class Accept(nL: Long, c: RSM) extends KompicsEvent;
case class Accepted(nL: Long, m: Int) extends KompicsEvent;
case class Decide(ld: Int, nL: Long) extends KompicsEvent;
case class Nack(time: Int) extends KompicsEvent;


case class Request_Lease(time: Long) extends KompicsEvent;
case class Accept_Lease(time: Long) extends KompicsEvent;

case class Request_Extension(time: Long) extends KompicsEvent;
case class Accept_Extension(time: Long) extends KompicsEvent;


case class AskExtension(timeout: ScheduleTimeout) extends Timeout(timeout);
case class EndRequest(timeout: ScheduleTimeout) extends Timeout(timeout);
case class EndPromise(timeout: ScheduleTimeout) extends Timeout(timeout);
case class EndLease(timeout: ScheduleTimeout) extends Timeout(timeout);


object State extends Enumeration {
    type State = Value;
    val PREPARE, ACCEPT, UNKOWN = Value;
}

object Role extends Enumeration {
    type Role = Value;
    val LEADER, FOLLOWER = Value;
}
