/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.testContext;

import core.DTNHost;
import core.Connection;
import routing.MessageRouter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import routing.EnergyAwareRouter;

/**
 *
 * @author Pauwerfull
 */
public class neighborInfo {

    /**
     * Mendapatkan daftar neighbor dan buffer yang tersedia
     *
     * @param host Node yang sedang diperiksa
     * @return Map berisi pasangan (neighborID -> bufferAvailable)
     */
    public static Map<Integer, Double> getNeighborBuffers(DTNHost host) {
        Map<Integer, Double> neighborBuffer = new HashMap<>();
        List<Connection> connections = host.getConnections();

        for (Connection conn : connections) {
            DTNHost neighbor = conn.getOtherNode(host);
            MessageRouter router = neighbor.getRouter();
            double availableBuffer = router.getFreeBufferSize();

            neighborBuffer.put(neighbor.getAddress(), availableBuffer);
        }

        return neighborBuffer;
    }

    /**
     * Mendapatkan daftar neighbor dan energi yang tersisa.
     *
     * @param host Node yang sedang diperiksa
     * @return Map berisi pasangan (neighborID -> energyRemaining)
     */
    public static Map<Integer, Double> getNeighborEnergy(DTNHost host) {
        Map<Integer, Double> neighborEnergy = new HashMap<>();
        List<Connection> connections = host.getConnections();

        for (Connection conn : connections) {
            DTNHost neighbor = conn.getOtherNode(host);
            MessageRouter router = neighbor.getRouter();

            double energyRemaining = -1.0; // Default jika router bukan EnergyAwareRouter

            if (router instanceof EnergyAwareRouter) {
                energyRemaining = EnergyManager.getRemainingEnergy((EnergyAwareRouter) router);
            }

            neighborEnergy.put(neighbor.getAddress(), energyRemaining);
        }

        return neighborEnergy;
    }

}
