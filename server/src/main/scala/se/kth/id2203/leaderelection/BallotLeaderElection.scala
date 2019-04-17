package se.kth.id2203.leaderelection;

import se.kth.id2203.bootstrapping._
import se.kth.id2203.broadcast._
import se.kth.id2203.networking._
import se.kth.id2203.overlay._
import se.sics.kompics.network._
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}
import se.sics.kompics.{KompicsEvent, Start}

import scala.collection.mutable;

class GossipLeaderElection extends ComponentDefinition {

  val ble = provides(BallotLeaderElection);
  val pl = requires(PerfectLink);
  val timer = requires[Timer];
  val boot = requires(Bootstrapping);

  private val ballotOne = 0x0100000000l;

  def ballotFromNAddress(n: Int, adr: NetAddress): Long = {
    val nBytes = com.google.common.primitives.Ints.toByteArray(n);
    val addrBytes = com.google.common.primitives.Ints.toByteArray(adr.hashCode());
    val bytes = nBytes ++ addrBytes;
    val r = com.google.common.primitives.Longs.fromByteArray(bytes);
    assert(r > 0); // should not produce negative numbers!
    r
  }

  def incrementBallotBy(ballot: Long, inc: Int): Long = {
    ballot + inc.toLong * ballotOne
  }

  val self = cfg.getValue[NetAddress]("id2203.project.address");

  var group = Set.empty[NetAddress];
  var allNodes =Set.empty[NetAddress];

  val delta = cfg.getValue[Long]("id2203.project.bleDelay");
  var majority = (group.size / 2) + 1;

  private var period = cfg.getValue[Long]("id2203.project.bleDelay");
  private var ballots = mutable.Map.empty[NetAddress, Long];

  private var round = 0l;
  private var ballot = ballotFromNAddress(0, self);

  private var leader: Option[(Long, NetAddress)] = None;
  private var highestBallot: Long = ballot;

  private def startTimer(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(delay);
    scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }

  private def checkLeader() {
    var topProcess=self;
    var topBallot: Long = 0;
    for ((p,b)<-ballots+(self->ballot)){
      if (b>topBallot) {
        topBallot = b;
        topProcess =p;
      }
    }
    var top: Option[(Long, NetAddress)] = Some((topBallot, topProcess));
    if(topBallot<highestBallot){
      while(ballot<=highestBallot){
        ballot=incrementBallotBy(ballot,1);
      }
      leader= None;
    }
    else{
      if(top!=leader){
        highestBallot=topBallot;
        leader = top;
        trigger(BLE_Leader(topProcess, topBallot) -> ble);
        //log.info(s"New leader $topProcess elected with ballot $topBallot");
      }
    }
  }

  boot uponEvent {
    case BootedBLE(assignment: LookupTable) => handle {
      //log.info("Got NodeAssignment, start leader election.");
      val lut = assignment;
      for (range<-lut.partitions.keySet){
        if (lut.partitions(range).contains(self)){
          group ++= lut.partitions(range);
        }
        allNodes ++= lut.partitions(range);
      }
      majority = (group.size / 2) + 1;
      log.info(s"Group member of $self is $group");
      log.info(s"All nodes of $self is $allNodes");
      //log.info(s"Process $self timer $period");
      startTimer(period);
    }
  }

  timer uponEvent {
    case CheckTimeout(_) => handle {
      if (ballots.size+1 >= majority){
        checkLeader();
        //log.info(s"Process $self checking leader");
      }
      ballots = mutable.Map.empty[NetAddress, Long];
      round = round +1;
      for (p <- group){
        if (p!=self){
          trigger(PL_Send(p,HeartbeatReq(round, highestBallot)) -> pl);
        }
      }
      //log.info(s"Process $self sent HB request at round $round with highestB $highestBallot");
      startTimer(period);
    }
  }

  pl uponEvent {
    case PL_Deliver(src, HeartbeatReq(r, hb)) => handle {
      if(hb>highestBallot) {
        highestBallot=hb;
      }
      trigger(PL_Send(src,HeartbeatResp(r, ballot)) -> pl);
      //log.info(s"Process $self sent HB response at round $r with ballot $ballot");
    }
    case PL_Deliver(src, HeartbeatResp(r, b)) => handle {
      if(r==round){
        ballots = ballots + (src->b)
        //log.info(s"Process $self ballots $ballots");
      } else {
        period = period+delta;
      }
    }
  }
}
