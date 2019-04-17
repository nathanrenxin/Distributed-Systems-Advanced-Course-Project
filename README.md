# ID2203 – Distributed Systems, Advanced course
# Partitioned, distributed in-memory key-value store with linearisable operation semantics.

## Abstract

In most moderns system, data must be highly available, scalable and responsive. This is often
done by using distributed key stores engineered in such a way that they are linearizable,
meaning that servers in the same partition should agree on a sequence of operations. This final
project is about developing a system that can handle multiple servers that are split into groups
of specific range for a key value store, being either a leader or a replica. The system is fault
tolerant when majority of nodes in each partition is alive, defined as N/2 + 1. The advanced
algorithm sequence paxos (sequence consensus) is used for each partition, to make
linearizability achievable. The system does not handle reconfiguring of nodes, but this would
undoubtedly increase the availability of the system.

## Introduction

For this project, the programming language Scala was used rather than Java. We already had a
lot of experience in Java so picking Scala seemed like a good opportunity to learn a new
language, plus we had done exercises from the course in the same language. The project
template seemed like a good way to start from, which is structured and built in Kompics by Lars
Kroll. GitHub was used for source control and the code can be found here1.

### Infrastructure
The template was a nice way of starting the project but for our implementation we needed to
tweak the source code a little bit, by changing the structure of the operations, responses, lookup
table and more. The project was built with passive replication in mind because from the basic
course we knew that the leader in the passive replication could keep linearizability and handle
the fast reads, which gave us a hint on how to implement one of the advanced tasks. We let our
system split the key value store by range, creating three separate partitions that handled key
values from 1 to 6000. Thus, each partition took care of 2000 keys, starting with two predefined
values in its keystore. The replication degree was set to two so each partition was a set of one
leader and two replicas, setting our boot threshold to nine.

To make our implementation reliable we used several topics learned from the course such as
Ballot Leader Election, Best Effort Broadcast, Perfect Link and Eventually Perfect Failure
Detector.

### KV store

The KV store was a simple Map that stored stringed values to given keys of integers. Clients
could operate on these stores with three simple operations named GET (read), PUT (write) and
CAS (compare-and-swap). If a node received a request about performing one of those
operations it would forward it to the leader where he would perform it, but the biggest challenge
was to make sure that the changes made to the KV store would also be performed in the
replicas. We tackled this problem with the Sequence Paxos algorithm that we learned in the
course.

### Advanced task - leader leases

For extra points, we had the options of upgrading our project so it could be reconfigurable with
nodes leaving or joining the system or implementing a leader lease to optimize the system for
GET operations. Since we already did the reconfiguration in the previous Distributed Systems
course, trying out the leader lease seemed like a proper learning experience. Picking that option
was also viable for our solution since we already planned the project for passive replication,
having the leader lease in mind. We implemented a safe lease so that only one leader in a
group can have lease by considering the asynchronous network and the clock drift of each
server. An extension of the lease was also implemented. Since it was impossible to get the
clock drift for each server in the project simulation (except taking the hardware apart) each node
had a fixed value for their drift. Fast read was also implemented and is allowed when the leader
has lease and there is no outstanding write operations in the operation queue to be handled by
the leader.

## System Behavior and Algorithm Selection Reasoning

A bootstrap server is started first and then 8 servers are started and connected to the bootstrap
server. When all 9 servers are ready, the bootstrap server assigns all the servers to a range
partition group and create a same lookup table, which contains mapping from range to servers,
for each server and send it to each server. Each server gets the information about its own
range, its group members and all the servers in the network from the lookup table. Clients can
connect to any server in the network, but connection is rejected before all 9 servers are ready.

Ballot Leader Election (BLE) starts upon receiving the lookup table in each group. We chose
BLE because we do not know the upbound of the time that needed to process a message and
transport a message, and the Eventually Perfect Failure Detector in BLE can eventually help us
find the upbound and detect node failure. BLE also guarantees that eventually every correct
node elects the same correct node as leader. When a leader is elected, the leader would
broadcast a message with its ballot to all the servers in the network so that each server can
update the lookup table so that the lookup table contains(changes to) mapping from range to
leader. The update is only done if the ballot in the message is larger than the stored leader
ballot for the range in the lookup table to avoid that in the situation(which can happen if replica
degree is bigger than or equal to 4) where a leader dies and immediately the new leader dies
too. Then the 2nd new leader is elected, but the message from the 1st new leader arrives at
each server later than the message from the 2nd new leader does, and then all the servers
keep the dead 1st new leader in the lookup table. Monotonic unique ballots in BLE makes sure
that the ballot of new leader is bigger than previous leader.

Each server in the network receives operations from clients and forwards each operation to the
range leader for the range that contains the key of the operation by looking up the lookup table,
which guarantees that only the leader in a group can propose operations. All the operations are
replied by the leader directly to the clients.

Sequence Paxos starts when new leader is elected in a group. It guarantees that all the servers
in the same group execute the same sequence of operations. Since all the operations are
forwarded to the leader and then proposed by the leader to the group, all the operations are
executed in the order that they are received by the leader, which guarantees linearizability.

With leader lease implemented in the Sequence Paxos, the system can handle fast read which
means when the leader receives a read operation, it replies directly without contacting majority.
But this is only allowed when the leader has lease and there is no write operation for the same
key waiting for execution. The leader requests to group members to extend lease before the
current lease ends. When a new leader is elected, it requests lease from the group member
every few seconds until it gets lease. When network partition happens, the old leader with lease
in one partition can handle fast read and the new leader without lease in another partition can not execute any operation until it gets lease (the new leader behaves the same when the old
leader who has the lease dies).

PerfectPointToPointLink and Best Effort Broadcast which was implemented on
PerfectPointToPointLink were used, because we want that when both sender and receiver are
alive, the message is eventually delivered exactly once and no message is delivered unless it is
sent.

The Network in Kompics is used as PerfectPointToPointLink with TCP which also has FIFO
property needed in Sequence Paxos.

## For test results, please refer to the report.

# ID2203 Project 2018 Starter Code for Kompics Scala

This project contains some code to get you started with the project.
You are encouraged to create your own forks and work on them, modifying everything and anything as you desire it.

## Overview

The project is split into 3 sub parts:

- A common library shared between servers and clients, containing mostly messages and similar shared types
- A server library that manages bootstrapping and membership
- A client library with a simple CLI to interact with a cluster of servers

The bootstrapping procedure for the servers, requires one server to be marked as a bootstrap server, which the other servers (bootstrap clients) check in with, before the system starts up. The bootstrap server also assigns initial partitions.

## Getting Started

`git clone` (your fork of) the repository to your local machine and `cd` into that folder.

Make sure you have [sbt](https://www.scala-sbt.org/) installed.

### Building

Start sbt with

```bash
sbt
```

In the sbt REPL build the project with

```bash
compile
```

You can run the test suite (which includes simulations) with

```bash
test
```

Before running the project you need to create assembly files for the server and client:

```bash
server/assembly
client/assembly
```

### Running

#### Bootstrap Server Node
To run a bootstrap server node execute:

```
java -jar server/target/scala-2.12/server.jar -p 45678
```

This will start the bootstrap server on localhost:45678.

#### Normal Server Node
After you started a bootstrap server on `<bsip>:<bsport>`, again from the `server` directory execute:

```
java -jar server/target/scala-2.12/server.jar -p 45679 -s <bsip>:<bsport>
```
This will start the bootstrap server on localhost:45679, and ask it to connect to the bootstrap server at `<bsip>:<bsport>`.
Make sure you start every node on a different port if they are all running directly on the local machine.

By default you need 3 nodes (including the bootstrap server), before the system will actually generate a lookup table and allow you to interact with it.
The number can be changed in the configuration file (cf. [Kompics docs](http://kompics.sics.se/current/tutorial/networking/basic/basic.html#cleanup-config-files-classmatchers-and-assembly) for background on Kompics configurations).

#### Clients
To start a client (after the cluster is properly running) execute:

```
java -jar client/target/scala-2.12/client.jar -p 56787 -b <bsip>:<bsport>
```

Again, make sure not to double allocate ports on the same machine.

The client will attempt to contact the bootstrap server and give you a small command promt if successful. Type `help` to see the available commands.

## Issues
If you find a bug please create an issue on git, or create a pull request with a fix.

If there are other questions, try to talk to the other students (e.g., in Canvas forums) and only if that doesn't help write me an email at <lkroll@kth.se>. Or, of course, ask at a lab session.
