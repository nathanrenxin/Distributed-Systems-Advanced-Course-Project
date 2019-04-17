package se.kth.id2203.simulation;

import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.network.identifier.Identifier;
import se.sics.kompics.simulator.network.identifier.IdentifierExtractor;

public class myIdentifier implements Identifier {
    final Address addr;
    public myIdentifier(Address addr) {
        this.addr = addr;
    }
    @Override
    public int partition(int nrPartitions){
        return 0;
    }
}
