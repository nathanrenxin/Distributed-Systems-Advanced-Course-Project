package se.kth.id2203.simulation;

import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.network.identifier.Identifier;
import se.sics.kompics.simulator.network.identifier.IdentifierExtractor;

public class myIdentifierExtractor implements IdentifierExtractor {
    @Override
    public Identifier extract(Address adr){
        myIdentifier myId = new myIdentifier(adr);
        return myId;
    }
}
