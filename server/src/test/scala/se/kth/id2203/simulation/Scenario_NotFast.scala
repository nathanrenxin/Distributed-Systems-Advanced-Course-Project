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

import java.util.UUID;
import se.kth.id2203.kvstore._;
import se.kth.id2203.networking._;
import se.kth.id2203.overlay.RouteMsg;
import se.sics.kompics.sl._
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.sl.simulator.SimulationResult;
import collection.mutable;
import se.sics.kompics.simulator.core.impl.P2pSimulator;

class Scenario_NotFast extends ComponentDefinition {

  //******* Ports ******
  val net = requires[Network];
  val timer = requires[Timer];
  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  val key = cfg.getValue[Int]("id2203.project.key");
  val server = cfg.getValue[NetAddress]("id2203.project.bootstrap-address");
  private val pending = mutable.Map.empty[UUID, String];

  private val operations = mutable.Map(
    1 -> Op(0, key.toString(),"GET"),
    2 -> Op(0, key.toString(),"PUT",Some(key.toString())),
  );


  //******* Handlers ******
  ctrl uponEvent {
    case _: Start => handle
    {
        var op = operations(1);
        trigger(NetMessage(self, server, RouteMsg(op.key, op)) -> net)
        var pkey = op.request+ "-" +op.key;
        pending += (op.id -> pkey);
        logger.info("Sending {}", op)
        SimulationResult += (pkey -> "Sent")

      op = operations(2);
      trigger(NetMessage(self, server, RouteMsg(op.key, op)) -> net)
      pkey = op.request+ "-" +op.key;
      pending += (op.id -> pkey);
      logger.info("Sending {}", op)
      SimulationResult += (pkey -> "Sent")

      var counter =1;
      while(counter<=1) {
        val op = Op(0, key.toString(), "GET")
        trigger(NetMessage(self, server, RouteMsg(op.key, op)) -> net)
        var sKey = op.request + "-" + op.key + "-" + counter.toString();
        pending += (op.id -> sKey);
        logger.info("Sending {}", op)
        SimulationResult += (sKey -> "Sent")
        counter += 1;
      }
    }
  }

  net uponEvent {
    case NetMessage(header, or @ OpResponse(id, status, value)) => handle {
      logger.info(s"Got OpResponse: $or");
      pending.remove(id) match {
        case Some(key) => {
          SimulationResult += (key -> (status.toString() + "-" + value.toString()));
        };
        case None      => logger.warn("ID $id was not pending! Ignoring response.");
      }
    }
  }
}
