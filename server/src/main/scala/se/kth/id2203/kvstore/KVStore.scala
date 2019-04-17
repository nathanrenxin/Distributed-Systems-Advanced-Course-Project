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
package se.kth.id2203.kvstore;

import se.kth.id2203.bootstrapping.{BootedKV, Bootstrapping}
import se.kth.id2203.broadcast._
import se.kth.id2203.leaderelection._
import se.kth.id2203.networking._
import se.kth.id2203.overlay._
import se.kth.id2203.paxos._
import se.sics.kompics.sl._
import se.sics.kompics.network.Network;
import scala.collection.mutable.Map

import State._
import Role._


class KVService extends ComponentDefinition {

  //******* Ports ******
  val plink = requires(PerfectLink);
  val route = requires(Routing);
  val boot = requires(Bootstrapping);
  val beb = requires(BestEffortBroadcast);
  val sc = requires(SeqCons);
  val ble = requires(BallotLeaderElection);

  //******* Fields ******
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  var KVStore = Map.empty[Int, String];

  def compareAndSwap(id: Int, compareValue: String, newValue: String) : String =
  {
    var memoryValue = read(id)

    if(compareValue == memoryValue)
    {
      memoryValue = newValue // Put(variable, newValue)
      val success = write(id, memoryValue)
      return compareValue.toString()
    }
    return memoryValue.toString()

  }
  def read(id: Int) : String =
  {
      return KVStore(id)
  }
  def write(id: Int, newValue: String) : String =
  {
      KVStore(id) = newValue
      return newValue
  }

  boot uponEvent {
    case BootedKV(assignment: LookupTable) => handle {
      log.info("Got NodeAssignment, preload KV store.");
      val lut = assignment;
      for (range<-lut.partitions.keySet){
        if (lut.partitions(range).contains(self))
        {
          log.info("SELF {}",self)
          log.info("TOP RANGE {}! ", range._1+1000);
          log.info("BOT RANGE {}! ", range._2);

          KVStore+=(range._1+1000->(range._1+1000).toString);
          KVStore+=(range._2->(range._2).toString);
        }
      }
    }
  }
  //******* Handlers ******
  // Place holder if we want to switch back to legacy to test operations
  sc uponEvent
  {
    case SC_Decide(RSM(src, op, isFastRead, id), leader: NetAddress) => handle{
         //logger.info(s"$self receives SC Decide on $op and fast read $isFastRead and leader $leader");
          if(self==leader && cfg.getValue[Boolean]("id2203.project.doNetPart")){log.info(s"$self decides operation $op!");}
          var shouldSend = (self == leader)

          val key = op.key.toInt
          if(op.request == "GET")
          {
            // Make sure key exists
            if(KVStore.exists(_._1 == key))
            {
              val value = read(key)
              if(shouldSend)
              {
                if(isFastRead){
                  trigger(PL_Send(src, op.response(OpCode.Ok,"Fast-"+value)) -> plink);
                }
                else{
                  trigger(PL_Send(src, op.response(OpCode.Ok,value)) -> plink);
                }
              }
            }
            else
            {
              val notFoundMsg = op.request + "-" + op.key;
              if(shouldSend)
              {
                if(isFastRead){
                  trigger(PL_Send(src, op.response(OpCode.NotFound,"Fast-"+notFoundMsg)) -> plink);
                }
                else{
                  trigger(PL_Send(src, op.response(OpCode.NotFound,notFoundMsg)) -> plink);
                }
              }
            }
          }
          else if(op.request == "PUT")
          {
            // TODO: Move this to promise and accept phase for writing value in kvstore
            val newValue = op.putV.get;
            val value = write(key, newValue)
            if(shouldSend)
            {
              trigger(PL_Send(src, op.response(OpCode.Ok,value)) -> plink);
            }

          }
          else if(op.request == "CAS")
          {
            // TODO: Also this
            val compareValue = op.casV.get;
            val newValue = op.putV.get;
            // The key must also exists so it can be read...
            // What happens in CAS if you read a key with no value?
            if(KVStore.exists(_._1 == key)) // Another placeholder
            {
              val value = compareAndSwap(key, compareValue,newValue)
              if(shouldSend)
                trigger(PL_Send(src, op.response(OpCode.Ok,value)) -> plink);
            }
            else
            {
              val notFoundMsg = op.request + "-" + op.key;
              if(shouldSend)
                trigger(PL_Send(src, op.response(OpCode.NotFound,notFoundMsg)) -> plink);
            }
          }
          else
          {
            if(shouldSend)
              trigger(PL_Send(src, op.response(OpCode.NotImplemented,"none")) -> plink);
          }
        }
  }

  val paxos = true
  plink uponEvent
  {
    case PL_Deliver(src, op: Op) => handle {
      if(cfg.getValue[Boolean]("id2203.project.doNetPart")){
        log.info(s"$self Got operation $op!")
      };
      val toRsm = RSM(src, op)
      trigger(SC_Propose(toRsm) -> sc)
    }
  }
}
