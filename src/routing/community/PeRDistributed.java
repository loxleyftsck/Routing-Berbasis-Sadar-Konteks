/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

import core.Buffer;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.Tuple;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Pauwerfull
 */
public class PeRDistributed implements RoutingDecisionEngine {

    public static final String DAMPING_FACTOR_SETTING = "dampingFactor";

    private final Map<DTNHost, List<Set<DTNHost>>> connHistory;
    private final Map<DTNHost, Tuple<Double, Integer>> per;

    private double dampingFactor = 0.75;

    public PeRDistributed(Settings s) {
        if (s.contains(DAMPING_FACTOR_SETTING)) {
            dampingFactor = s.getDouble(DAMPING_FACTOR_SETTING);
        }
        connHistory = new HashMap<>();
        per = new HashMap<>();
    }

    public PeRDistributed(PeRDistributed r) {
        this.dampingFactor = r.dampingFactor;
        this.connHistory = new HashMap<>(r.connHistory);
        this.per = new HashMap<>(r.per);
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        addConnectionToHistory(thisHost, peer);
        addConnectionToHistory(peer, thisHost);
        updatePeopleRank(thisHost);
        updatePeopleRank(peer);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        updatePeopleRank(thisHost);
        updatePeopleRank(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeRDistributed de = this.getOtherDecisionEngine(peer);

        if (connHistory.containsKey(myHost)) {
            List<Set<DTNHost>> connectionHistory = connHistory.get(myHost);

            for (Set<DTNHost> contactSet : connectionHistory) {
                if (contactSet.contains(peer)) {
                    updatePeopleRank(myHost);
                    updatePeopleRank(peer);
                    break;
                }
            }
        }
    }

    private void addConnectionToHistory(DTNHost host, DTNHost peer) {
        List<Set<DTNHost>> hostHistory = connHistory.getOrDefault(host, new LinkedList<>());
        Set<DTNHost> connectedSet = new HashSet<>();
        connectedSet.add(peer);
        hostHistory.add(connectedSet);
        connHistory.put(host, hostHistory);
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        double perThisHost = calculatePer(thisHost);
        double perOtherHost = calculatePer(otherHost);

        if (m.getTo() == otherHost) {
            return true;
        }

        if (connHistory.containsKey(otherHost)) {
            Iterator<Set<DTNHost>> iterator = connHistory.get(otherHost).iterator();
            while (iterator.hasNext()) {
                Set<DTNHost> contactSet = iterator.next();
                if (contactSet.contains(thisHost)) {
                    return perOtherHost >= perThisHost;
                }
            }
        }

        Buffer messageBuffer = new Buffer();
        int bufferSize = messageBuffer.getBufferSize(thisHost);
        Iterator<Message> bufferIterator = messageBuffer.Iterator();
        while (bufferSize > 0 && bufferIterator.hasNext()) {
            Message bufferedMessage = bufferIterator.next();
            if (perOtherHost >= perThisHost || otherHost.equals(bufferedMessage.getTo())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public void update(DTNHost thisHost) {
        // No specific action needed for routing update
    }

    /**
     * Get the PeopleRank value for a specific host.
     *
     * @param host The host for which to get the PeopleRank value
     * @return The calculated PeopleRank value
     */
    public double getPeopleRank(DTNHost host) {
        Tuple<Double, Integer> info = per.get(host);
        return (info != null) ? info.getKey() : 0.0;
    }

    private double calculatePer(DTNHost host) {
        double sum = 0.0;
        List<Set<DTNHost>> connectionHistory = connHistory.get(host);

        if (connectionHistory == null || connectionHistory.isEmpty()) {
            return 0.0;
        }

        int numNeighbors = connectionHistory.size(); // Menghitung jumlah set tetangga

        for (Set<DTNHost> neighborSet : connectionHistory) {
            for (DTNHost neighbor : neighborSet) {
                Tuple<Double, Integer> neighborInfo = per.get(neighbor);
                if (neighborInfo != null && neighborInfo.getValue() != 0) {
                    sum += neighborInfo.getKey(); // Menambahkan nilai PeopleRank tetangga
                }
            }
        }

        return (1 - dampingFactor) + dampingFactor * sum / numNeighbors;
    }

    private void updatePeopleRank(DTNHost host) {
        double sum = 0.0;
        List<Set<DTNHost>> connectionHistory = connHistory.get(host);

        if (connectionHistory == null || connectionHistory.isEmpty()) {
            return;
        }

        int numNeighbors = 0;
        for (Set<DTNHost> neighborSet : connectionHistory) {
            numNeighbors += neighborSet.size();
            for (DTNHost neighbor : neighborSet) {
                Tuple<Double, Integer> neighborInfo = per.get(neighbor);
                if (neighborInfo != null && neighborInfo.getValue() != 0) {
                    sum += neighborInfo.getKey();
                }
            }
        }

        double updatedPer = (1 - dampingFactor) + dampingFactor * sum / numNeighbors;
        Tuple<Double, Integer> existingInfo = per.get(host);
        if (existingInfo != null) {
            existingInfo.setKey(updatedPer);
        } else {
            per.put(host, new Tuple<>(updatedPer, 0));
        }
    }

    private PeRDistributed getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works with other routers of the same type";
        return (PeRDistributed) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeRDistributed(this);
    }
}
