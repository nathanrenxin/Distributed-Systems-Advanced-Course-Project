package se.kth.id2203.broadcast

import java.net.{InetAddress, InetSocketAddress}

import se.kth.id2203.broadcast._
import se.kth.id2203.leaderelection.{BLE_Leader, BallotLeaderElection}
import se.kth.id2203.networking._
import se.sics.kompics.{Init, KompicsEvent, Start}
import se.sics.kompics.network.{Address, Network, Transport}
import se.sics.kompics.sl.{ComponentDefinition, _}
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}

class PerfectP2PLink extends ComponentDefinition {

    val pLink = provides(PerfectLink);
    val network = requires[Network];
    val timer = requires[Timer]; //for network partition test
    val ble = requires(BallotLeaderElection); //for network partition test

    val self = cfg.getValue[NetAddress]("id2203.project.address");

    //for network partition test
    var counter =0;
    val doNetPart = cfg.getValue[Boolean]("id2203.project.doNetPart");
    val startPartitionTimer = cfg.getValue[Long]("id2203.project.startPartitionTimer");
    val stopPartitionTimer = cfg.getValue[Long]("id2203.project.stopPartitionTimer");
    var isNetPart = false;
    var leader: Option[NetAddress] = None;
    case class startPartition(timeout: ScheduleTimeout) extends Timeout(timeout);
    case class stopPartition(timeout: ScheduleTimeout) extends Timeout(timeout);
    private def startPartitionTimer(delay: Long): Unit = {
        val scheduledTimeout = new ScheduleTimeout(delay);
        scheduledTimeout.setTimeoutEvent(startPartition(scheduledTimeout));
        trigger(scheduledTimeout -> timer);
    }
    private def stopPartitionTimer(delay: Long): Unit = {
        val scheduledTimeout = new ScheduleTimeout(delay);
        scheduledTimeout.setTimeoutEvent(stopPartition(scheduledTimeout));
        trigger(scheduledTimeout -> timer);
    }

    pLink uponEvent {
        case PL_Send(dest, payload) => handle {
            if(isNetPart==false || (isNetPart==true && leader.get!=dest)||dest==self){//for network partition test
                trigger(NetMessage(self, dest, payload) -> network);
                //log.info(s"Sending message from $self to $dest when isNetPart $isNetPart and leader $leader");
            }
        }
        case PL_Forward(src, dest, payload) => handle {
            if(isNetPart==false || (isNetPart==true && leader.get!=dest)||dest==self){//for network partition test
                trigger(NetMessage(src, dest, payload) -> network);
                //log.info(s"Sending message from $self to $dest when isNetPart $isNetPart and leader $leader");
            }
        }
    }

    network uponEvent {
        case NetMessage(header, payload) => handle {
            if(isNetPart==false || (isNetPart==true && leader.get!=header.src)||header.src==self){//for network partition test
                trigger(PL_Deliver(header.src, payload) -> pLink);
            }
        }
    }

    //for network partition test
    ble uponEvent {
        case BLE_Leader(l, n) => handle {
            if(counter==0){
                leader=Some(l);
                if(doNetPart){
                    startPartitionTimer(startPartitionTimer);
                }
                counter+=1;
            }
        }
    }

    timer uponEvent {
        case startPartition(_) => handle {
            isNetPart = true;
            stopPartitionTimer(stopPartitionTimer);
            log.info(s"Process $self start network partitioning");
        };
        case stopPartition(_) => handle {
            isNetPart = false;
            log.info(s"Process $self stop network partitioning");
        };
    }

}