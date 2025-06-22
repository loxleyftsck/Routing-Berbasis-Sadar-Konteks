/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;


import java.util.*;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import routing.testContext.neighborInfo;


/**
 *
 * @author Pauwerfull
 */
public class BufferNeighborInfoReport extends Report implements UpdateListener {
    @Override
    public void updated(List<DTNHost> hosts) {
        if (isWarmup()) {
            return;
        }

        write("Time: " + SimClock.getTime());

        for (DTNHost host : hosts) {
            Map<Integer, Double> neighborBuffers = neighborInfo.getNeighborBuffers(host);
            
            write("Node " + host.getAddress() + " has " + neighborBuffers.size() + " neighbors");
            for (Map.Entry<Integer, Double> entry : neighborBuffers.entrySet()) {
                write("Neighbor " + entry.getKey() + " Available Buffer: " + entry.getValue());
            }
        }
    }

    @Override
    public void done() {
        super.done();
    }

}

