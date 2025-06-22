/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.testContext;

import core.DTNHost;
import routing.MessageRouter;
import core.SimClock;
import core.Connection;
import java.util.List;


/**
 *
 * @author LENOVO
 */
public class bufferCalculator {
    
    public static void neighborBuffers(List<DTNHost> hosts) {
        System.out.println("Time : " + SimClock.getTime());
        
        for (DTNHost host : hosts) {
            List<Connection> connections = host.getConnections();
            System.out.println("Node "+host.getAddress() +"has"+ connections.size() +"neighbor");
            
            for (Connection conn : connections) {
                DTNHost neighbor = conn.getOtherNode(host);
                MessageRouter router = neighbor.getRouter();
                double availableBuffer = router.getFreeBufferSize();
                
                System.out.println("Neighbor "+neighbor.getAddress()+ "Available Buffer "+availableBuffer);
                
            }
        }
    }
}
