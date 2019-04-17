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
package se.kth.id2203.simulation

import org.scalatest._
import se.kth.id2203.ParentComponent
import se.kth.id2203.networking._
import se.sics.kompics.network.Address
import java.net.{InetAddress, UnknownHostException}

import se.sics.kompics.sl._
import se.sics.kompics.sl.simulator._
import se.sics.kompics.simulator.{SimulationScenario => JSimulationScenario}
import se.sics.kompics.simulator.run.LauncherComp
import se.sics.kompics.simulator.result.SimulationResultSingleton
import se.sics.kompics.simulator.events.system.KillNodeEvent
import se.sics.kompics.simulator.network.{NetworkModel, PartitionMapper}
import se.sics.kompics.simulator.network.identifier.IdentifierExtractor
import se.sics.kompics.simulator.network.impl._

import scala.concurrent.duration._

class ClientTest extends FlatSpec with Matchers {

  private val nMessages = 10;

  private val runOperations = true;

  "Basic operations" should "work out" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario(9);
      //val res = SimulationResultSingleton.getInstance();
      //SimulationResult += ("messages" -> nMessages);
      simpleBootScenario.simulate(classOf[LauncherComp]);

      SimulationResult.get[String]("PUT-3007") should be (Some("Ok-gurk"));
      SimulationResult.get[String]("PUT-1100") should be (Some("Ok-mja"));
      SimulationResult.get[String]("PUT-3009") should be (Some("Ok-lurk"));
      SimulationResult.get[String]("PUT-3050") should be (Some("Ok-durk"));
      SimulationResult.get[String]("GET-3000") should be (Some("Ok-Fast-3000"));
      SimulationResult.get[String]("GET-3007") should be (Some("Ok-Fast-gurk"));
      SimulationResult.get[String]("GET-3009") should be (Some("Ok-Fast-lurk"));
      SimulationResult.get[String]("GET-3050") should be (Some("Ok-Fast-durk")) ;
      SimulationResult.get[String]("GET-1100") should be (Some("Ok-Fast-mja")) ;
      SimulationResult.get[String]("CAS-3050") should be (Some("Ok-durk"));
    }
  }

  "Old leader and new leader" should "be different(KV should be the same)" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_killLeader(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);

      SimulationResult.get[String]("PUT-6000-1") should be (Some("Ok-Kill_Leader_Test"));
      SimulationResult.get[String]("GET-6000-1") should be (Some("Ok-Kill_Leader_Test"));
      SimulationResult.get[String]("GET-6000-2") should be (Some("Ok-Fast-Kill_Leader_Test"));
      var leader1= SimulationResult.get[String]("LEADER_BEFORE");
      var leader2= SimulationResult.get[String]("LEADER_AFTER");
      leader1 should not be leader2;

    }
  }
  "All clients connected to different servers" should "get reply from same leader(fast read)" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_readFromSameLeader(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);

      // Different clients all read value from the same node (leader)
      SimulationResult.get[String]("GET-5000-1") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-2") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-3") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-4") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-5") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-6") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-7") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-8") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("GET-5000-9") should be (Some("Ok-Fast-5000"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.2"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.3"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.4"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.5"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.6"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.7"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.8"));
      SimulationResult.get[String]("ReadFromLeader_/192.193.1.1") should be (SimulationResult.get[String]("ReadFromLeader_/192.193.1.9"));

    }
  }

  "All operations sent in order" should "be linearisable" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_linearisablity(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);

      SimulationResult.get[String]("PUT-3000") should be (Some("Ok-1"));
      SimulationResult.get[String]("PUT-4000") should be (Some("Ok-2"));
      SimulationResult.get[String]("GET-3000") should be (Some("Ok-Fast-1"));
      SimulationResult.get[String]("CAS-4000") should be (Some("Ok-2"));
      SimulationResult.get[String]("CAS-4000-1") should be (Some("Ok-3"));
      SimulationResult.get[String]("GET-4000") should be (Some("Ok-Fast-3"));
      SimulationResult.get[String]("PUT-4000-1") should be (Some("Ok-2"));
      SimulationResult.get[String]("GET-4000-1") should be (Some("Ok-Fast-2")) ;

    }
  }

  "After majority nodes die, operations" should "timeout" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_timeout(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);

      SimulationResult.get[String]("PUT-6000-3") should be (Some("Ok-Kill_Leader_Test"));
      SimulationResult.get[String]("GET-6000-3") should be (Some("Ok-Kill_Leader_Test"));
      SimulationResult.get[String]("GET-6000-4") should be (Some("Ok-Fast-Kill_Leader_Test"));
      SimulationResult.get[String]("GET-6000-5") should be (Some("Timeout"));
      var leader1= SimulationResult.get[String]("LEADER_BEFORE");
      var leader2= SimulationResult.get[String]("LEADER_AFTER");
      leader1 should not be leader2;

    }
  }

  "Read following Write" should " not be fast" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_NotFast(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);
      SimulationResult.get[String]("GET-5201") should be (Some("NotFound-Fast-GET-5201"));
      SimulationResult.get[String]("PUT-5201") should be (Some("Ok-5201"));
      SimulationResult.get[String]("GET-5201-1") should be (Some("Ok-5201"));
    }
  }

  "Network partition" should "not impact linearisablity" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_networkPartition(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);
    }
  }

  "Benchmark: load testing" should "work with lease" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_benchmark_lease(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);
    }
  }

  "Benchmark: load testing" should "work without lease" in {
    if(runOperations)
    {
      val seed = 123l;
      JSimulationScenario.setSeed(seed);
      val simpleBootScenario = OperationScenario.scenario_benchmark_nolease(9);
      simpleBootScenario.simulate(classOf[LauncherComp]);
    }
  }
}

object OperationScenario {

  import Distributions._
  implicit val random = JSimulationScenario.getRandom();

  private def intToServerAddress(i: Int): Address = {
    try {
      NetAddress(InetAddress.getByName("192.193.0." + i), 45678);
    } catch {
      case ex: UnknownHostException => throw new RuntimeException(ex);
    }
  }
  private def intToClientAddress(i: Int): Address = {
    try {
      NetAddress(InetAddress.getByName("192.193.1." + i), 45678);
    } catch {
      case ex: UnknownHostException => throw new RuntimeException(ex);
    }
  }

  private def isBootstrap(self: Int): Boolean = self == 1;

  val startServerOp = Op { (self: Integer) =>

    val selfAddr = intToServerAddress(self)
    val conf = if (isBootstrap(self)) {
      // don't put this at the bootstrap server, or it will act as a bootstrap client
      Map("id2203.project.address" -> selfAddr)
    } else {
      Map(
        "id2203.project.address" -> selfAddr,
        "id2203.project.bootstrap-address" -> intToServerAddress(1))
    };
    StartNode(selfAddr, Init.none[ParentComponent], conf);
  };

  val startClientPUTOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1));
    StartNode(selfAddr, Init.none[ScenarioClientPut], conf);
  };
  val startClientREADop = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1));
    StartNode(selfAddr, Init.none[ScenarioClientRead], conf);
  };
  val startClientCASop = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1));
    StartNode(selfAddr, Init.none[ScenarioClientCas], conf);
  };

  val killNodeOp = Op { (self: Integer) =>
    KillNode(SimulationResult.get[NetAddress]("LEADER_ToKill").get);
  };

  val startClientREADFromNewLeaderOp_Before = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1),
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[ScenarioLeader_ReadBefore], conf);
  };
  val startClientREADFromNewLeaderOp_After = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1),
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[ScenarioLeader_ReadAfter], conf);
  };
  val startClientREADSameOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(self),
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[ScenarioLeader_ReadSame], conf);
  };

  val startClientLinearOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(self),
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_Linear], conf);
  };

  val startClientREADTimeoutOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(1),
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_Timeout], conf);
  };

  val startNotFastOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(self),
      "id2203.project.key" -> (5200+self));
    StartNode(selfAddr, Init.none[Scenario_NotFast], conf);
  };

  val startNetworkPartitionServerOp = Op { (self: Integer) =>

    val selfAddr = intToServerAddress(self)
    val conf = if (isBootstrap(self)) {
      Map("id2203.project.address" -> selfAddr,
        "id2203.project.doNetPart" -> true)
    } else {
      Map(
        "id2203.project.address" -> selfAddr,
        "id2203.project.doNetPart" -> true,
        "id2203.project.bootstrap-address" -> intToServerAddress(1))
    };
    StartNode(selfAddr, Init.none[ParentComponent], conf);
  };

  val startNetworkPartitionClient1Op = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(2),
      "id2203.project.times" -> 5,
      "id2203.project.key" -> 6000,
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_NetworkPartition], conf);
  };

  val startNetworkPartitionClient2Op = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(6),
      "id2203.project.times" -> 5,
      "id2203.project.key" -> 6000,
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_NetworkPartition], conf);
  };

  val startNetworkPartitionClient3Op = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(8),
      "id2203.project.times" -> 5,
      "id2203.project.key" -> 6000,
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_NetworkPartition], conf);
  };

  val startNetworkPartitionClient4Op = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(2),
      "id2203.project.times" -> 5,
      "id2203.project.key" -> 6000,
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_NetworkPartition], conf);
  };

  val startNetworkPartitionClient5Op = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(6),
      "id2203.project.times" -> 5,
      "id2203.project.key" -> 6000,
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_NetworkPartition], conf);
  };

  val startNetworkPartitionClient6Op = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(8),
      "id2203.project.times" -> 5,
      "id2203.project.key" -> 6000,
      "id2203.project.selfId" -> self);
    StartNode(selfAddr, Init.none[Scenario_NetworkPartition], conf);
  };

  val changeNetModelOp = Op { (self: Integer) =>
    ChangeNetwork(new PartitionedNetworkModel(new myIdentifierExtractor(),new UniformRandomModel(200,500),new myPartitionMapper()));
  };

  val startBenchmarkServerLeaseOp = Op { (self: Integer) =>

    val selfAddr = intToServerAddress(self)
    val conf = if (isBootstrap(self)) {
      Map("id2203.project.address" -> selfAddr,
        "id2203.project.useTimeLease" -> true)
    } else {
      Map(
        "id2203.project.address" -> selfAddr,
        "id2203.project.useTimeLease" -> true,
        "id2203.project.bootstrap-address" -> intToServerAddress(1))
    };
    StartNode(selfAddr, Init.none[ParentComponent], conf);
  };

  val startBenchmarkServerNoLeaseOp = Op { (self: Integer) =>

    val selfAddr = intToServerAddress(self)
    val conf = if (isBootstrap(self)) {
      Map("id2203.project.address" -> selfAddr,
        "id2203.project.useTimeLease" -> false)
    } else {
      Map(
        "id2203.project.address" -> selfAddr,
        "id2203.project.useTimeLease" -> false,
        "id2203.project.bootstrap-address" -> intToServerAddress(1))
    };
    StartNode(selfAddr, Init.none[ParentComponent], conf);
  };

  val startBenchmarkOp = Op { (self: Integer) =>
    val selfAddr = intToClientAddress(self)
    val conf = Map(
      "id2203.project.address" -> selfAddr,
      "id2203.project.bootstrap-address" -> intToServerAddress(self),
      "id2203.project.times" -> 10000,
      "id2203.project.key" -> 1000);
    StartNode(selfAddr, Init.none[Scenario_Benchmark], conf);
  };


  def scenario(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startServerOp, 1.toN).arrival(constant(5.second));
    val startClientPUT = raise(1, startClientPUTOp, 1.toN).arrival(constant(2.second));
    val startClientREAD = raise(1, startClientREADop, 2.toN).arrival(constant(5.second));
    val startClientCAS = raise(1, startClientCASop, 3.toN).arrival(constant(3.second));


    startCluster andThen
      10.seconds afterTermination startClientPUT andThen // Takes about 40 seconds to create table
      10.seconds afterTermination startClientREAD andThen
      10.seconds afterTermination startClientCAS andThen
      10.seconds afterTermination Terminate

  }

  def scenario_killLeader(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startServerOp, 1.toN).arrival(constant(5.second));
    val startClientREADFromNewLeader_Before = raise(1, startClientREADFromNewLeaderOp_Before, 1.toN).arrival(constant(2.second));
    val killNode5 = raise(1, killNodeOp, 1.toN).arrival(constant(2.second));
    val startClientREADFromNewLeader_After = raise(1, startClientREADFromNewLeaderOp_After, 2.toN).arrival(constant(2.second));

    startCluster andThen
      10.seconds afterTermination startClientREADFromNewLeader_Before andThen
      10.seconds afterTermination killNode5 andThen
      10.seconds afterTermination startClientREADFromNewLeader_After andThen
      10.seconds afterTermination Terminate

  }

  def scenario_readFromSameLeader(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startServerOp, 1.toN).arrival(constant(5.second));
    val startClientREADSame = raise(9, startClientREADSameOp, 1.toN).arrival(constant(2.second));

    startCluster andThen
      10.seconds afterTermination startClientREADSame andThen
      10.seconds afterTermination Terminate
  }

  def scenario_linearisablity(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startServerOp, 1.toN).arrival(constant(5.second));
    val startClientLinear = raise(8, startClientLinearOp, 1.toN).arrival(constant(1.second));

    startCluster andThen
      10.seconds afterTermination startClientLinear andThen
      10.seconds afterTermination Terminate
  }

  def scenario_timeout(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startServerOp, 1.toN).arrival(constant(5.second));
    val startClientREADFromNewLeader_Before = raise(1, startClientREADFromNewLeaderOp_Before, 3.toN).arrival(constant(2.second));
    val killNode1 = raise(1, killNodeOp, 1.toN).arrival(constant(2.second));
    val startClientREADFromNewLeader_After = raise(1, startClientREADFromNewLeaderOp_After, 4.toN).arrival(constant(2.second));
    val killNode2 = raise(1, killNodeOp, 1.toN).arrival(constant(2.second));
    val startClientREADTimeout = raise(1, startClientREADTimeoutOp, 5.toN).arrival(constant(2.second));

    startCluster andThen
      10.seconds afterTermination startClientREADFromNewLeader_Before andThen
      10.seconds afterTermination killNode1 andThen
      10.seconds afterTermination startClientREADFromNewLeader_After andThen
      10.seconds afterTermination killNode1 andThen
      10.seconds afterTermination startClientREADTimeout andThen
      20.seconds afterTermination Terminate
  }

  def scenario_NotFast(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startServerOp, 1.toN).arrival(constant(5.second));
    val startNotFast = raise(1, startNotFastOp, 1.toN).arrival(constant(1.second));

    startCluster andThen
      10.seconds afterTermination startNotFast andThen
      10.seconds afterTermination Terminate
  }

  def scenario_networkPartition(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startNetworkPartitionServerOp, 1.toN).arrival(constant(5.second));
    val startNetworkPartitionC1 = raise(1, startNetworkPartitionClient1Op, 1.toN).arrival(constant(1.second));
    val startNetworkPartitionC2 = raise(1, startNetworkPartitionClient2Op, 2.toN).arrival(constant(1.second));
    val startNetworkPartitionC3 = raise(1, startNetworkPartitionClient3Op, 3.toN).arrival(constant(1.second));
    val startNetworkPartitionC4 = raise(1, startNetworkPartitionClient4Op, 4.toN).arrival(constant(1.second));
    val startNetworkPartitionC5 = raise(1, startNetworkPartitionClient5Op, 5.toN).arrival(constant(1.second));
    val startNetworkPartitionC6 = raise(1, startNetworkPartitionClient6Op, 6.toN).arrival(constant(1.second));
    //val changeNetModel = raise(1, changeNetModelOp, 1.toN).arrival(constant(1.second));


    startCluster andThen
      10.seconds afterTermination startNetworkPartitionC1 andThen
      0.seconds afterStart startNetworkPartitionC2 andThen
      0.seconds afterStart startNetworkPartitionC3 andThen
      8.seconds afterTermination startNetworkPartitionC4 andThen
      0.seconds afterStart startNetworkPartitionC5 andThen
      0.seconds afterStart startNetworkPartitionC6 andThen
      20.seconds afterTermination Terminate
  }

  def scenario_benchmark_lease(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startBenchmarkServerLeaseOp, 1.toN).arrival(constant(5.second));
    val startBenchmark = raise(1, startBenchmarkOp, 1.toN).arrival(constant(1.second));

    startCluster andThen
      10.seconds afterTermination startBenchmark andThen
      10.seconds afterTermination Terminate
  }

  def scenario_benchmark_nolease(servers: Int): JSimulationScenario = {

    val startCluster = raise(servers, startBenchmarkServerNoLeaseOp, 1.toN).arrival(constant(5.second));
    val startBenchmark = raise(1, startBenchmarkOp, 1.toN).arrival(constant(1.second));

    startCluster andThen
      10.seconds afterTermination startBenchmark andThen
      10.seconds afterTermination Terminate
  }

}
