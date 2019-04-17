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
package se.kth.id2203.overlay;

import com.larskroll.common.collections._;
import java.util.Collection;
import se.kth.id2203.bootstrapping.NodeAssignment;
import se.kth.id2203.networking.NetAddress;
import scala.collection.mutable.{Map};

@SerialVersionUID(0x57bdfad1eceeeaaeL)
class LookupTable extends NodeAssignment with Serializable {

  val partitions = TreeSetMultiMap.empty[(Int,Int), NetAddress];
  val ballots = Map.empty[(Int,Int), Long];

  def lookup(key: String): Iterable[NetAddress] = {
    val keyInt = key.toInt;
    var range=(0,0);
    for (k<-partitions.keySet){
      if(k._1<keyInt && k._2 >=keyInt){
        range = k;
      }
    }
    return partitions(range);
  }

  def getNodes(): Set[NetAddress] = partitions.foldLeft(Set.empty[NetAddress]) {
    case (acc, kv) => acc ++ kv._2
  }

  def update(range:(Int,Int), leader:Option[NetAddress], ballot:Long): TreeSetMultiMap[(Int,Int), NetAddress] =  {
    if(ballot>ballots(range)){
      for (p<-partitions(range)){
        partitions.remove(range,p);
      }
      partitions++= (range -> Set(leader.get));
      ballots(range)=ballot;
    }
    return partitions;
  }

  override def toString(): String = {
    val sb = new StringBuilder();
    sb.append("LookupTable(\n");
    sb.append(partitions.mkString(","));
    sb.append(")");
    return sb.toString();
  }

}

object LookupTable {
  def generate(nodes: Set[NetAddress]): LookupTable = {
    val lut = new LookupTable();
    lut.partitions ++= ((0,2000) -> nodes.take(3));
    lut.partitions ++= ((2000,4000) -> nodes.takeRight(6).take(3));
    lut.partitions ++= ((4000,6000) -> nodes.takeRight(3));
    lut.ballots += ((0,2000) -> 0l);
    lut.ballots += ((2000,4000) -> 0l);
    lut.ballots += ((4000,6000) -> 0l);
    lut
  }
}

