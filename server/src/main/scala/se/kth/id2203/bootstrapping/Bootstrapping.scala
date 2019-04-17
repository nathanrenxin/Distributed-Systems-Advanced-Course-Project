package se.kth.id2203.bootstrapping

import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl._;
import se.kth.id2203.networking.NetAddress;

object Bootstrapping extends Port {
  indication[GetInitialAssignments];
  indication[BootedOV];
  indication[BootedKV];
  indication[BootedB];
  indication[BootedBLE];
  indication[BootedPAX];
  request[InitialAssignments];
}

case class GetInitialAssignments(nodes: Set[NetAddress]) extends KompicsEvent;
case class BootedOV(assignment: NodeAssignment) extends KompicsEvent;
case class BootedKV(assignment: NodeAssignment) extends KompicsEvent;
case class BootedB(assignment: NodeAssignment) extends KompicsEvent;
case class BootedBLE(assignment: NodeAssignment) extends KompicsEvent;
case class BootedPAX(assignment: NodeAssignment) extends KompicsEvent;
case class InitialAssignments(nodes: Set[NetAddress]) extends KompicsEvent;
