package se.kth.id2203.simulation;

import se.sics.kompics.simulator.network.PartitionMapper;

public class myPartitionMapper implements PartitionMapper<myIdentifier>{
    @Override
    public int getPartition(myIdentifier nodeId){
        //System.out.printf("the ip is: "+nodeId.addr.getIp().toString());
        if(nodeId.addr.getIp().toString()== "/192.193.0.6"){
            return 1;
         }
         else{return 2;}
    }
}
