/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Settings;
import core.SimError;
import core.UpdateListener;

/**
 *
 * @author LENOVO
 */
public class NeighborEnergyReport extends Report implements UpdateListener{
    public static final String GRANULARITY = "granularity";
    protected final int granularity;
    protected double lastUpdate;

    public NeighborEnergyReport() {
        Settings settings = getSettings();
        this.lastUpdate = 0;
        this.granularity = settings.getInt(GRANULARITY);
        init();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        double simTime = getSimTime();
        if (isWarmup()) {
            return; 
        }
        if (simTime - lastUpdate >= granularity) {
            createSnapshot(hosts);
            this.lastUpdate = simTime - simTime % granularity;
        }
    }

    private void createSnapshot(List<DTNHost> hosts) {
        write("[" + (int) getSimTime() + "] Neighbor Energy Levels");
        
        for (DTNHost h : hosts) {
            write("Node: " + h.toString());
            for (Connection con : h.getConnections()) {
                DTNHost neighbor = con.getOtherNode(h);
                Double energy = (Double) neighbor.getComBus()
                        .getProperty(routing.EnergyAwareRouter.ENERGY_VALUE_ID);
                if (energy == null) {
                    throw new SimError("Host " + neighbor + " is not using an energy aware router");
                }
                write("  Neighbor: " + neighbor + " Energy: " + format(energy));
            }
        }
    }
}
