package se.kth.id2203.leaderelection;

import se.kth.id2203.networking._;
import se.sics.kompics.KompicsEvent
import se.sics.kompics.network._
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}


case class BLE_Leader(leader: NetAddress, ballot: Long) extends KompicsEvent;

object BallotLeaderElection extends Port {
  indication[BLE_Leader];
}

case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout);

case class HeartbeatReq(round: Long, highestBallot: Long) extends KompicsEvent;

case class HeartbeatResp(round: Long, ballot: Long) extends KompicsEvent;

