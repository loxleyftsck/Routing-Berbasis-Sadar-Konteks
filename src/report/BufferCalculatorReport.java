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
import core.Settings;
import routing.testContext.neighborInfo;

/**
 *
 * @author Pauwerfull
 */
public class BufferCalculatorReport extends Report implements UpdateListener {

    private double lastRecord = Double.MIN_VALUE;
    private final int interval;
    private final Map<DTNHost, Double> bufferUsage;
    
    public static final String BUFFER_REPORT_INTERVAL = "bufferReportInterval";
    public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 3600;
    
    public BufferCalculatorReport() {
        super();
        bufferUsage = new HashMap<>();
        
        Settings settings = getSettings();
        if (settings.contains(BUFFER_REPORT_INTERVAL)) {
            interval = settings.getInt(BUFFER_REPORT_INTERVAL);
        } else {
            interval = DEFAULT_BUFFER_REPORT_INTERVAL;
        }
    }
    
    @Override
    public void updated(List<DTNHost> hosts) {
        if (isWarmup()) {
            return;
        }
        
        if (SimClock.getTime() - lastRecord >= interval) {
            lastRecord = SimClock.getTime();
            logBufferUsage(hosts);
        }
    }
    
    private void logBufferUsage(List<DTNHost> hosts) {
        write("Time: " + SimClock.getTime());
        
        for (DTNHost host : hosts) {
            Map<Integer, Double> neighborBuffers = neighborInfo.getNeighborBuffers(host);
            
            write("Node " + host.getAddress() + " has " + neighborBuffers.size() + " neighbors");
            
            for (Map.Entry<Integer, Double> entry : neighborBuffers.entrySet()) {
                write("Neighbor " + entry.getKey() + " Available Buffer: " + entry.getValue());
                bufferUsage.put(host, entry.getValue());
            }
        }
    }
    
    @Override
    public void done() {
        write("Final Buffer Usage Report:");
        for (Map.Entry<DTNHost, Double> entry : bufferUsage.entrySet()) {
            write("Node " + entry.getKey().getAddress() + " Average Free Buffer: " + entry.getValue());
        }
        super.done();
    }
}

//     private double lastRecord = Double.MIN_VALUE;
//    private int interval;
//    private Map<DTNHost, Double> bufferUsage;
//
//    public static final String BUFFER_REPORT_INTERVAL = "bufferReportInterval";
//    public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 3600;
//
//    public BufferCalculatorReport() {
//        super();
//        bufferUsage = new HashMap<>();
//
//        Settings settings = getSettings();
//        if (settings.contains(BUFFER_REPORT_INTERVAL)) {
//            interval = settings.getInt(BUFFER_REPORT_INTERVAL);
//        } else {
//            interval = DEFAULT_BUFFER_REPORT_INTERVAL;
//        }
//    }
//
//     @Override
//    public void updated(List<DTNHost> hosts) {
//        if (isWarmup()) {
//            return;
//        }
//
//        if (SimClock.getTime() - lastRecord >= interval) {
//            lastRecord = SimClock.getTime();
//            logBufferUsage(hosts);
//        }
//    }
//
//    private void logBufferUsage(List<DTNHost> hosts) {
//        write("Time: " + SimClock.getTime());
//
//        for (DTNHost host : hosts) {
//            List<Connection> connections = host.getConnections();
//            write("Node " + host.getAddress() + " has " + connections.size() + " neighbors");
//
//            for (Connection conn : connections) {
//                DTNHost neighbor = conn.getOtherNode(host);
//                MessageRouter router = neighbor.getRouter();
//                double availableBuffer = router.getFreeBufferSize();
//
//                write("Neighbor " + neighbor.getAddress() + " Available Buffer: " + availableBuffer);
//                bufferUsage.put(neighbor, availableBuffer);
//            }
//        }
//    }
//
//    @Override
//    public void done() {
//        write("Final Buffer Usage Report:");
//        for (Map.Entry<DTNHost, Double> entry : bufferUsage.entrySet()) {
//            write("Node " + entry.getKey().getAddress() + " Average Free Buffer: " + entry.getValue());
//        }
//        super.done();
//    }
