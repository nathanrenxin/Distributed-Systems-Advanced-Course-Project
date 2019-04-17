/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.bootstrapping;

import java.util.UUID

import se.kth.id2203.networking._
import se.kth.id2203.broadcast._
import se.kth.id2203.overlay.LookupTable
import se.sics.kompics.sl._
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.timer._

import collection.mutable;

object BootstrapServer {
  sealed trait State;
  case object Collecting extends State;
  case object Seeding extends State;
  case object Done extends State;

}

class BootstrapServer extends ComponentDefinition {
  import BootstrapServer._;

  //******* Ports ******
  val boot = provides(Bootstrapping);
  val plink = requires(PerfectLink);
  val timer = requires[Timer];
  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  val bootThreshold = cfg.getValue[Int]("id2203.project.bootThreshold");
  private var state: State = Collecting;
  private var timeoutId: Option[UUID] = None;
  private val active = mutable.HashSet.empty[NetAddress];
  private val ready = mutable.HashSet.empty[NetAddress];
  private var initialAssignment: Option[Set[NetAddress]] = None;
  //******* Handlers ******
  ctrl uponEvent {
    case _: Start => handle {
      log.info("Starting bootstrap server on {}, waiting for {} nodes...", self, bootThreshold);
      val timeout: Long = (cfg.getValue[Long]("id2203.project.keepAlivePeriod") * 2l);
      val spt = new SchedulePeriodicTimeout(timeout, timeout);
      spt.setTimeoutEvent(BSTimeout(spt));
      trigger (spt -> timer);
      timeoutId = Some(spt.getTimeoutEvent().getTimeoutId());
      active += self;
    }
  }

  timer uponEvent {
    case BSTimeout(_) => handle {
      state match {
        case Collecting => {
          log.info("{} hosts in active set.", active.size);
          if (active.size >= bootThreshold) {
            bootUp();
          }
        }
        case Seeding => {
          log.info("{} hosts in ready set.", ready.size);
          if (ready.size >= bootThreshold) {
            log.info("Finished seeding. Bootstrapping complete.");
            initialAssignment match {
              case Some(assignment) => {
                state = Done;
              }
              case None => {
                logger.error(s"No initial assignment received at bootThreshold. Ready nodes: $ready");
                suicide();
              }
            }
          }
        }
        case Done => {
          suicide();
        }
      }
    }
  }

  boot uponEvent {
    case InitialAssignments(nodes) => handle {
      initialAssignment = Some(nodes);
      log.info("Seeding assignments...");
      active foreach { node =>
        val assignment = LookupTable.generate(nodes);
        trigger(PL_Send(node, Boot(assignment)) -> plink);
        if(self==node){
          trigger(BootedOV(assignment) -> boot);
          trigger(BootedKV(assignment) -> boot);
          trigger(BootedB(assignment) -> boot);
          trigger(BootedBLE(assignment) -> boot);
          trigger(BootedPAX(assignment) -> boot);
        }
      }
      ready += self;
    }
  }

  plink uponEvent {
    case PL_Deliver(src, CheckIn) => handle {
      active += src;
    }
    case PL_Deliver(src, Ready) => handle {
      ready += src;
    }
  }

  override def tearDown(): Unit = {
    timeoutId match {
      case Some(tid) => trigger(new CancelPeriodicTimeout(tid) -> timer);
      case None      => // nothing to clean up
    }
  }

  private def bootUp(): Unit = {
    log.info("Threshold reached. Generating assignments...");
    state = Seeding;
    trigger(GetInitialAssignments(active.toSet) -> boot);
  }
}
