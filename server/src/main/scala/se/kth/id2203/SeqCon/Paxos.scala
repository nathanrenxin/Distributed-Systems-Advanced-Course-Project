package se.kth.id2203.paxos;

import se.kth.id2203.bootstrapping._
import se.kth.id2203.broadcast._
import se.kth.id2203.leaderelection._
import se.kth.id2203.networking._
import se.kth.id2203.overlay._
import se.sics.kompics.sl._
import se.sics.kompics.network.Network
import se.sics.kompics.timer.{ScheduleTimeout, Timeout, Timer}
import java.lang.System._
import java.util.UUID;
import scala.util.Try;
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer

import se.sics.kompics.network.Network
import se.kth.id2203.kvstore._
import State._
import Role._


class PaxosService extends ComponentDefinition {

  val sc = provides(SeqCons);
  val plink = requires(PerfectLink);
  val route = requires(Routing);
  val beb = requires(BestEffortBroadcast);
  val ble = requires(BallotLeaderElection);
  val boot = requires(Bootstrapping);
  val timer = requires[Timer];

  // TODO: Set
  val self = cfg.getValue[NetAddress]("id2203.project.address");
  val LEASE_OPTION = cfg.getValue[Boolean]("id2203.project.useTimeLease");
  var pi : Set[NetAddress] @unchecked = Set.empty
  var others : Set[NetAddress] @unchecked = Set.empty
  // Placeholder
  var majority = (pi.size / 2) + 1;

  var state = (FOLLOWER, UNKOWN);
  var nL = 0l;
  var nProm = 0l;
  var leader: Option[NetAddress] = None;
  var na = 0l;
  var va = List.empty[RSM];
  var ld = 0;

  // Leasing
  private val fastReadOperations = Map.empty[UUID, String];

  // Acceptor
  var lastPromise : Long = -100000000;
  // Proposer
  var askPromise : Long = 0;

  var leasePromiseCount = 0
  var extensionPromiseCount = 0;

  var promiseExtension = false
  var promiseLease = false
  var hasLease = false
  var hasExtension = false

  val timeForLease = cfg.getValue[Long]("id2203.project.timeForLease");
  val requestTimeLease = cfg.getValue[Long]("id2203.project.requestTimeLease");

  // This is p, average is around 10 or 20
  val clockDriftRate = 0.1;//10;

  // leader state
  var propCmds = List.empty[RSM];
  var las = Map.empty[NetAddress, Int];
  var lds = Map.empty[NetAddress, Int];
  var lc = 0;
  var acks = Map.empty[NetAddress, (Long, List[RSM])];

  def suffix(s: List[RSM], l: Int): List[RSM] = {
      s.drop(l)
  }

  def prefix(s: List[RSM], l: Int): List[RSM] = {
      s.take(l)
  }

  boot uponEvent {
    case BootedPAX(assignment: LookupTable) => handle {
      log.info("Got NodeAssignment, start paxos.");
      val lut = assignment;
      for (range<-lut.partitions.keySet){
        if (lut.partitions(range).contains(self)){
          pi ++= lut.partitions(range);
        }
      }
      others = pi-self;
      majority = (pi.size / 2) + 1;

      //clockDriftRate // TODO: Add random small number here to simulate different drift rate?
    }
  }

  private def startTimer_PromiseLease(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(delay);
    scheduledTimeout.setTimeoutEvent(EndPromise(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }
  private def startTimer_HasLease(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(delay);
    scheduledTimeout.setTimeoutEvent(EndLease(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }
  private def startTimer_startExtension(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(delay);
    scheduledTimeout.setTimeoutEvent(AskExtension(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }
  private def startTimer_RequestLease(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(delay);
    scheduledTimeout.setTimeoutEvent(EndRequest(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }

  private def canQuickRead(seq: List[RSM],len:Int, key:String): Boolean = {
    if(seq.size==len){
      return true;
    }
    else
      {
        var canQR=true;
        var leng=len;
        while(leng<seq.size){
          if(seq(leng).operation.key==key &&seq(leng).operation.request!="GET"){
            canQR=false;
          }
          leng+=1;
        }
        return canQR;
      }
  }

  timer uponEvent {
    case EndPromise(_) => handle
    {
      // Drop lease promise
      //logger.warn(s"node $self stops promise")
      if(promiseExtension)
      {
        promiseExtension = false
      }
      else
      {
        promiseLease = false
      }
    }
    case AskExtension(_) => handle
    {
      //logger.info(s"$self requesting for extension")
      extensionPromiseCount = 0
      askPromise = currentTimeMillis()
      //startTimer_RequestLease(timeForLease)
      for (p<-others)
      {
        // Ask for extension
        trigger(PL_Send(p,Request_Extension(500)) -> plink);
      }
    }

    case EndRequest(_) => handle
    {
      // If leader hasn't gotten the lease by majority, request again
      if(hasLease == false)
      {
        leasePromiseCount = 0
        startTimer_RequestLease(requestTimeLease)
        askPromise = currentTimeMillis()
        for (p<-others)
        {
          // Ask for lease
          trigger(PL_Send(p,Request_Lease(requestTimeLease)) -> plink);
        }
      }
    }
    case EndLease(_) => handle
    {
        // Request for lease again
        //logger.warn(s"leader $self asks group lease")
        if(LEASE_OPTION)
        {
          if(hasExtension)
          {
            hasExtension = false
          }
          else
          {
            //logger.info(s"$self lost lease")
            askForLease()
          }
        }
    }
  }

  def askForLease()
  {
    //logger.info(s"$self requesting for lease")
    leasePromiseCount = 0
    hasLease = false
    askPromise = currentTimeMillis()
    //println(s"$self ask promise $askPromise")
    startTimer_RequestLease(requestTimeLease)
    for (p<-others)
    {
      //println(s"leader $self asks $p for lease");
      trigger(PL_Send(p,Request_Lease(timeForLease)) -> plink);
    }
  }
  def giveLease()
  {
    // Set timeout
    //logger.warn(s"leader $self has lease")
    //var g = currentTimeMillis()
    //logger.info(s"$self got lease/extension")
    var timeSinceRequest = currentTimeMillis() - askPromise

    var safeTimeLease : Long = timeForLease// - timeSinceRequest
    //println(s"g $g")
    //println(s"tsr $timeSinceRequest")
    //println(s"ap $askPromise")
    //println(s"safeTimeLease $safeTimeLease")
    startTimer_HasLease(timeForLease)
    startTimer_startExtension(safeTimeLease-requestTimeLease) // Request extension 2 secs before expiring
    hasLease = true;

  }

  plink uponEvent
  {
    // Leader lease
    case PL_Deliver(p, Request_Lease(time)) => handle {
      // Do check
      // Do we need to make a check if the node that is asking for lease is the considered leader? not sure
      // If we use the "or" there, this can be thought of as an leader asking for extension
      var canGrantPromise =  currentTimeMillis() - lastPromise > (timeForLease*(1+clockDriftRate)).toLong
      if(promiseLease == false && canGrantPromise)
      {
        lastPromise = currentTimeMillis() // Needed for async version

        promiseLease = true
        val myDriftRate : Int = 50 // Not used, just a placeholder
        // Set timeout
        //logger.warn(s"node $self promises lease to $p")
        var time = (timeForLease*(1+clockDriftRate)).toInt
        startTimer_PromiseLease(time)
        trigger(PL_Send(p,Accept_Lease(myDriftRate)) -> plink);
      }
      else
      {
        // Respond with Nack.. but why?
        trigger(PL_Send(p,Nack(0)) -> plink);
      }
    }

    case PL_Deliver(p, Request_Extension(time)) => handle
    {
        if(p == leader.get)
        {
          lastPromise = currentTimeMillis() // Needed for async version
          promiseExtension = true
          var time = (timeForLease*(1+clockDriftRate)).toInt
          startTimer_PromiseLease(time)
          trigger(PL_Send(p,Accept_Extension(0)) -> plink);
        }
    }
    case PL_Deliver(p, Accept_Extension(time)) => handle
    {
      extensionPromiseCount += 1
      if(extensionPromiseCount >= majority-1)
      {
        if(hasExtension == false){
          hasExtension = true;
          giveLease();
        }
      }
    }
    case PL_Deliver(p, Nack(time)) => handle {
        // Nack received
    }
    case PL_Deliver(p, Accept_Lease(driftTime)) => handle {
      leasePromiseCount += 1
      if(leasePromiseCount >= majority-1)
      {
        if(hasLease == false)
        {
          giveLease()
          if (state == (LEADER, PREPARE))
          {
            for (p<-others){
              trigger(PL_Send(p,Prepare(nL, ld, na)) -> plink);
              //log.info(s"Process $self sends Prepare $nL $ld $na to $p");
            }
          }
        }
      }
    }
    //  SequencePaxos
    case PL_Deliver(p, Prepare(np, ldp, n)) => handle {
      if(nProm<np){
          nProm =np;
          state=(FOLLOWER,PREPARE);
        //log.info(s"Process $self becomes follower prepare");
          var sfx=List.empty[RSM];
          if(na>=n){
              sfx=suffix(va,ldp);
          }
          trigger(PL_Send(p,Promise(np, na, sfx, ld)) -> plink);
        //log.info(s"Process $self sends Promise $np $na $sfx $ld to $p");
      }
    }
    case PL_Deliver(a, Promise(n, na, sfxa, lda)) => handle {
      if ((n == nL) && (state == (LEADER, PREPARE))) {
        acks(a)=(na,sfxa);
        lds(a)=lda;
        var pro: Set[NetAddress] = pi.filter(acks.contains(_));
        if (pro.size==majority){
            var k=0l;
            var sfx = List.empty[RSM];
            for ((k1,sfx1)<-acks.values){
                if(k1>k|| (k1==k && sfx1.size>sfx.size)){
                    k=k1;
                    sfx=sfx1;
                }
            }
            va=prefix(va,ld)++sfx++propCmds;
            las(self)=va.size;
            propCmds = List.empty[RSM];
            state=(LEADER, ACCEPT);
          //log.info(s"Process $self becomes leader accept");
            for (p<-others){
                if(lds.contains(p)){
                    val sfxp=suffix(va,lds(p));
                    trigger(PL_Send(p,AcceptSync(nL, sfxp, lds(p))) -> plink);
                  //log.info(s"Process $self sends AcceptSync $nL $sfxp $lds(p) to $p");
                }
            }
        }
      } else if ((n == nL) && (state == (LEADER, ACCEPT))) {
        lds(a)=lda;
        val sfxp=suffix(va,lds(a));
        trigger(PL_Send(a,AcceptSync(nL, sfxp, lds(a))) -> plink);
        //log.info(s"Process $self sends AcceptSync $nL $sfxp $lds(a) to $a");
        if(lc!=0){
            trigger(PL_Send(a,Decide(ld, nL)) -> plink);
          //log.info(s"Process $self sends Decide $ld $nL to $a");
        }
      }
    }
    case PL_Deliver(p, AcceptSync(nL, sfx, ldp)) => handle {
      if ((nProm == nL) && (state == (FOLLOWER, PREPARE))) {
         na=nL;
         va=prefix(va,ldp)++sfx;
         trigger(PL_Send(p,Accepted(nL, va.size)) -> plink);
        //log.info(s"Process $self sends Accepted $nL $va.size to $p");
         state=(FOLLOWER,ACCEPT);
        //log.info(s"Process $self becomes follower accept");
      }
    }
    case PL_Deliver(p, Accept(nL, c)) => handle {
      if ((nProm == nL) && (state == (FOLLOWER, ACCEPT))) {
         va=va:+c;
         trigger(PL_Send(p,Accepted(nL, va.size)) -> plink);
        //log.info(s"Process $self sends Accepted $nL $va.size to $p");
      }
    }
    case PL_Deliver(_, Decide(l, nL)) => handle {
       if(nProm == nL){
           while(ld<l)
           {
             // TODO: Maybe just skip this fastread option and let the client discard it
             if(va(ld).isFastRead == false) // Skip it if it has been fastread
             {
               trigger(SC_Decide(va(ld),leader.get) -> sc);
               //log.info(s"Process $self Decide $va $ld ");
             }
               ld=ld+1;
           }
       }
    }
    case PL_Deliver(a, Accepted(n, m)) => handle {
        //println(s"Process $self receives Accepted $n $nL $m $state");
      if ((n == nL) && (state == (LEADER, ACCEPT))) {
          las(a)=m;
          var pro = las.filter(t => t._2 >=m);
          //println(s"Process $self pro $pro");
          if(lc<m && pro.size>=majority){
              lc=m;
              for (p<-pi){
                if(lds.contains(p)){
                    trigger(PL_Send(p,Decide(lc, nL)) -> plink);
                  //log.info(s"Process $self sends Decide $lc $nL to $p");
                }
            }
          }
      }
    }
  }

  sc uponEvent{
    case SC_Propose(c) => handle {
      if (state == (LEADER, PREPARE)) {
         propCmds=propCmds:+c;
      }
      else if (state == (LEADER, ACCEPT))
      {
         las(self)=las(self)+1;
         var op = c.operation.request
         //log.info(s"hasLease $hasLease");
         // Leader can only respond to fastread if Ti - Tl < 10*(1-p)
        if(LEASE_OPTION)
        {
          var canRespond = currentTimeMillis() - askPromise < (timeForLease*(1-clockDriftRate)).toLong
          var quickRead = hasLease && op =="GET" && canQuickRead(va,ld,c.operation.key) && canRespond;
          var markedOperation = RSM(c.src, c.operation, quickRead,c.id);
          if(quickRead)
          {
            //logger.info(s"Can perform fast read on $markedOperation")
            trigger(SC_Decide(markedOperation,leader.get) -> sc);
          } else {
            va=va:+markedOperation;
            for (p<-others)
            {
              if(lds.contains(p))
              {
                trigger(PL_Send(p,Accept(nL, markedOperation)) -> plink);
              }
            }
          }
        }
        else
        {
          va=va:+c;
          for (p<-others)
          {
            if(lds.contains(p))
            {
              trigger(PL_Send(p,Accept(nL, c)) -> plink);
            }
          }
        }

         // NOTE: We can improve performance here by only doing these checks if leader has lease

      }
    }
  }

// BLE
  ble uponEvent{
    case BLE_Leader(l, n) => handle {
        if(n>nL){
            leader=Some(l);
            nL=n;
            if(self==l && nL>nProm){
                state=(LEADER, PREPARE);

              //log.info(s"Process $self becomes leader");
                propCmds = List.empty[RSM];
                las = Map.empty[NetAddress, Int];
                lds = Map.empty[NetAddress, Int];
                acks = Map.empty[NetAddress, (Long, List[RSM])];
                lc=0;

                if(LEASE_OPTION)
                {
                  askForLease()
                }
                else
                {
                  for (p<-others)
                  {
                    trigger(PL_Send(p,Prepare(nL, ld, na)) -> plink);
                  }
                }
                logger.info(s"$self lease $LEASE_OPTION")

                acks(l)=(na,suffix(va,ld));
                lds(self)=ld;
                nProm=nL;
            }
            else{
                state=(FOLLOWER,state._2);
              //log.info(s"Process $self becomes follower");
            }
        }
    }
  }
}
