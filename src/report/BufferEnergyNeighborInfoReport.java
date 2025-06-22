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
import routing.MessageRouter;
import routing.testContext.neighborInfo;
import routing.testContext.EnergyManager;
import routing.EnergyAwareRouter;

/**
 *
 * @author Pauwerfull
 */
public class BufferEnergyNeighborInfoReport extends Report implements UpdateListener {

    private double lastRecord = Double.MIN_VALUE;
    private final int interval;
    private final Map<DTNHost, Double> energyUsage;

    public static final String ENERGY_REPORT_INTERVAL = "energyReportInterval";
    public static final int DEFAULT_ENERGY_REPORT_INTERVAL = 3600;

    public BufferEnergyNeighborInfoReport() {
        super();
        energyUsage = new HashMap<>();

        Settings settings = getSettings();
        if (settings.contains(ENERGY_REPORT_INTERVAL)) {
            interval = settings.getInt(ENERGY_REPORT_INTERVAL);
        } else {
            interval = DEFAULT_ENERGY_REPORT_INTERVAL;
        }
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (isWarmup()) {
            return;
        }

        if (SimClock.getTime() - lastRecord >= interval) {
            lastRecord = SimClock.getTime();
            logEnergyUsage(hosts);
        }
    }

   private void logEnergyUsage(List<DTNHost> hosts) {
        write("Time: " + SimClock.getTime());

        for (DTNHost host : hosts) {
            MessageRouter router = host.getRouter();
            double energyRemaining = -1.0;

            if (router instanceof EnergyAwareRouter) {
                energyRemaining = EnergyManager.getRemainingEnergy((EnergyAwareRouter) router);
            }

            write("Node " + host.getAddress() + " Energy Remaining: " + energyRemaining);
            energyUsage.put(host, energyRemaining);
            
               // **Tambahkan informasi neighbor**
            Map<Integer, Double> neighborEnergies = neighborInfo.getNeighborEnergy(host);
            write("Node " + host.getAddress() + " has " + neighborEnergies.size() + " neighbors");

            for (Map.Entry<Integer, Double> entry : neighborEnergies.entrySet()) {
                write("Neighbor " + entry.getKey() + " Energy Remaining: " + entry.getValue());
            }
        }
    }

    @Override
    public void done() {
        write("Final Energy Report:");
        for (Map.Entry<DTNHost, Double> entry : energyUsage.entrySet()) {
            write("Node " + entry.getKey().getAddress() + " Final Energy: " + entry.getValue());
        }
        super.done();
    }

}
