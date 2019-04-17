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
package se.kth.id2203;

import se.kth.id2203.bootstrapping._
import se.kth.id2203.leaderelection._
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.paxos._;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay._
import se.kth.id2203.broadcast._
import se.sics.kompics.sl._
import se.sics.kompics.Init;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

class ParentComponent extends ComponentDefinition {

  //******* Ports ******
  val net = requires[Network];
  val timer = requires[Timer];
  //******* Children ******
  val plink=create(classOf[PerfectP2PLink], Init.NONE);
  val beb = create(classOf[BasicBroadcast], Init.NONE);
  val ble = create(classOf[GossipLeaderElection], Init.NONE);
  val overlay = create(classOf[VSOverlayManager], Init.NONE);
  val kv = create(classOf[KVService], Init.NONE);
  val pax = create(classOf[PaxosService], Init.NONE);

  val boot = cfg.readValue[NetAddress]("id2203.project.bootstrap-address") match {
    case Some(_) => create(classOf[BootstrapClient], Init.NONE); // start in client mode
    case None    => create(classOf[BootstrapServer], Init.NONE); // start in server mode
  }

  {
    //PerfectLink
    connect[Network](net -> plink);
    connect(BallotLeaderElection)(ble -> plink); //for network partition test
    connect[Timer](timer -> plink); //for network partition test
    //BestEffortBroadcast
    connect(PerfectLink)(plink -> beb);
    connect(Bootstrapping)(boot -> beb);
    //Bootstrap
    connect[Timer](timer -> boot);
    connect(PerfectLink)(plink -> boot);
    // Overlay
    connect(Bootstrapping)(boot -> overlay);
    connect(PerfectLink)(plink -> overlay);
    connect(BestEffortBroadcast)(beb -> overlay);
    connect(BallotLeaderElection)(ble -> overlay);
    // LeaderElection
    connect(Bootstrapping)(boot -> ble);
    connect(PerfectLink)(plink -> ble);
    connect[Timer](timer -> ble);
    // KV
    connect(Bootstrapping)(boot -> kv);
    connect(Routing)(overlay -> kv);
    connect(PerfectLink)(plink -> kv);
    connect(BestEffortBroadcast)(beb -> kv);
    connect(SeqCons)(pax -> kv);
    // Pax
    connect(PerfectLink)(plink -> pax);
    connect(BestEffortBroadcast)(beb -> pax);
    connect(Bootstrapping)(boot -> pax);
    connect(BallotLeaderElection)(ble -> pax);
    connect[Timer](timer -> pax);

  }
}
