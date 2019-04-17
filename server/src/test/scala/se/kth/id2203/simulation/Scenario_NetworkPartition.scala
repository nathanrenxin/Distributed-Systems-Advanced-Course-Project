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
package se.kth.id2203.simulation;

import java.util.UUID

import se.kth.id2203.kvstore._
import se.kth.id2203.networking._
import se.kth.id2203.overlay.RouteMsg
import se.sics.kompics.sl._
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}
import se.sics.kompics.sl.simulator.SimulationResult

import collection.mutable
import se.sics.kompics.simulator.core.impl.P2pSimulator;

class Scenario_NetworkPartition extends ComponentDefinition {

  //******* Ports ******
  val net = requires[Network];
  val timer = requires[Timer];
  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  val key = cfg.getValue[Int]("id2203.project.key");
  val times = cfg.getValue[Int]("id2203.project.times");
  val server = cfg.getValue[NetAddress]("id2203.project.bootstrap-address");
  val selfId = cfg.getValue[Int]("id2203.project.selfId");
  private val pending = mutable.Map.empty[UUID, String];
  private val operations = mutable.Map(
    1 -> Op(0,key.toString(),"GET"),
    2 -> Op(0,key.toString(),"PUT",Some((key+selfId).toString())),
    3 -> Op(0,key.toString(),"GET")
  );
  private val operations_again = mutable.Map(
    1 -> Op(0,key.toString(),"GET"),
    2 -> Op(0,key.toString(),"PUT",Some((key+selfId*2).toString())),
    3 -> Op(0,key.toString(),"GET")
  );

/*case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout);

private def startTimer(delay: Long): Unit = {
  val scheduledTimeout = new ScheduleTimeout(delay);
  scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout));
  trigger(scheduledTimeout -> timer);
}*/

//******* Handlers ******
ctrl uponEvent {
  case _: Start => handle
  {
    var n=1;
    while(n<=3){
      val op = operations(n);
      trigger(NetMessage(self, server, RouteMsg(op.key, op)) -> net)
      var key= op.request+ "-" +op.key;
      pending += (op.id -> key);
      logger.info(s"$self Sending $op")
      n+=1;
      //SimulationResult += (key -> "Sent")
      //startTimer(1000);
    }
  }
}

net uponEvent {
  case NetMessage(header, or @ OpResponse(id, status, value)) => handle {
    val from=header.src;
    logger.info(s"$self Got OpResponse: $or from $from");
    pending.remove(id) match {
      case Some(key) => {
        //SimulationResult += (key -> (status.toString() + "-" + value.toString()));
      };
        if(pending.isEmpty){
          logger.info(s"network partition testing self $self done")
        }
      case None      => logger.warn("ID $id was not pending! Ignoring response.");
    }
  }
}

/*timer uponEvent {
  case CheckTimeout(_) => handle {
    operations_again.foreach{ x:(Int, Op) =>
      val op = x._2
      trigger(NetMessage(self, server, RouteMsg(op.key, op)) -> net)
      var key= op.request+ "-" +op.key;
      pending += (op.id -> key);
      logger.info(s"$self Sending $op")
      //SimulationResult += (key -> "Sent")
    }
  }
}*/
}
