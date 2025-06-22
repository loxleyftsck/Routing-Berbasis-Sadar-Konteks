/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import core.SimScenario;
import core.Tuple;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.community.PeopleRankDistributed;
import routing.RoutingDecisionEngine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/**
 *
 * @author Pauwerfull
 */
public class PeopleRankDistributedReport extends Report {

    private final Map<DTNHost, Double> nodeRanks;
    private final List<DTNHost> nodeListSelfish;

    public PeopleRankDistributedReport() {
        super();
        this.nodeRanks = new HashMap<>();
        this.nodeListSelfish = new LinkedList<>();
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
            if (!(decisionEngine instanceof PeopleRankDistributed peopleRankDistributed)) {
                continue;
            }

            double rank = getPeopleRank(host, peopleRankDistributed);
            nodeRanks.put(host, rank);

            // No need to access selfish nodes anymore
        }

        printReport();

        super.done();
    }

    private double getPeopleRank(DTNHost host, PeopleRankDistributed peopleRankDistributed) {
        // Accessing the PeopleRank directly from the per map
        return peopleRankDistributed.per.getOrDefault(host, new Tuple<>(0.0, 0)).getKey();
    }

    private void printReport() {
        // Printing the report
        write("PeopleRank Distribution Report");
        write("=============================");
        for (Map.Entry<DTNHost, Double> entry : nodeRanks.entrySet()) {
            DTNHost host = entry.getKey();
            double rank = entry.getValue();
            write("Host: " + host + ", PeopleRank: " + rank);
        }
        write("=============================");
        write("End of Report");
    }
}