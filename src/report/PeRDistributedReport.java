/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.PeRDistributed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author LENOVO
 */
public class PeRDistributedReport extends Report {

    private final Map<DTNHost, Double> nodeRanks;

    public PeRDistributedReport() {
        super();
        this.nodeRanks = new HashMap<>();
    }

    @Override
    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();
        for (DTNHost host : nodes) {
            MessageRouter router = host.getRouter();
            if (!(router instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine decisionEngine = ((DecisionEngineRouter) router).getDecisionEngine();
            if (!(decisionEngine instanceof PeRDistributed peRDistributed)) {
                continue;
            }

            double rank = peRDistributed.getPeopleRank(host);
            nodeRanks.put(host, rank);
        }

        printReport();

        super.done();
    }

    private void printReport() {
        write("PeRDistributed Report");
        write("======================");
        for (Map.Entry<DTNHost, Double> entry : nodeRanks.entrySet()) {
            DTNHost host = entry.getKey();
            double rank = entry.getValue();
            write("Host: " + host + ", PeRDistributed Rank: " + rank);
        }
        write("======================");
        write("End of Report");
    }
}
