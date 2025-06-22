package routing.contextAware.ENS;

import core.DTNHost;
import core.SimClock;

import java.util.*;

/**
 * EncounteredNodeSet
 * Menyimpan dan mengelola informasi node-node yang pernah ditemui (ENS).
 * Digunakan untuk mendukung evaluasi konteks sosial seperti tie-strength, popularitas, dan encounter frequency.
 */
public class EncounteredNodeSet {

    private final Map<String, EncounteredNode> ensTable = new HashMap<>();
    private final Map<String, Map<String, List<Double>>> pairWiseEncounter = new HashMap<>();

    // ===================== 1. UPDATE / INSERT ===================== //

    public void updateENS(DTNHost host, DTNHost neighbor, String nodeId,
                          long encounterTime, double remainingEnergy, int bufferSize, long connectionDuration, double popularity) {
        String myId = String.valueOf(host.getAddress());

        if (!nodeId.equals(myId)) {
            EncounteredNode newNode = new EncounteredNode(nodeId, encounterTime, remainingEnergy, bufferSize, connectionDuration);
            newNode.setPopularity(popularity);
            updateOrInsert(nodeId, newNode, myId);
        }
    }

    private void updateOrInsert(String nodeId, EncounteredNode newNode, String myId) {
        EncounteredNode existingNode = ensTable.get(nodeId);

        if (existingNode == null) {
            ensTable.put(nodeId, newNode);
            newNode.incrementEncounterCount();
        } else {
            existingNode.incrementEncounterCount();
            existingNode.setEncounterTime(newNode.getEncounterTime());
            existingNode.setPopularity(newNode.getPopularity());

            if (newNode.isMoreRelevantThan(existingNode)) {
                existingNode.setRemainingEnergy(newNode.getRemainingEnergy());
                existingNode.setBufferSize(newNode.getBufferSize());
                existingNode.setConnectionDuration(newNode.getConnectionDuration());
            }
        }
    }

    public void updateConnectionDuration(String nodeId, long duration) {
        EncounteredNode node = ensTable.get(nodeId);
        if (node != null) node.updateConnectionDuration(duration);
    }

    // ===================== 2. MERGE & EXCHANGE ===================== //

    public void mergeENS(DTNHost host, EncounteredNodeSet otherENS, long currentTime, DTNHost neighbor) {
        if (otherENS == null || otherENS.ensTable.isEmpty()) return;
        String myId = String.valueOf(host.getAddress());

        for (Map.Entry<String, EncounteredNode> entry : otherENS.ensTable.entrySet()) {
            String nodeId = entry.getKey();
            if (!nodeId.equals(myId)) {
                ensTable.merge(nodeId, entry.getValue().clone(), (oldNode, newNode) -> {
                    return newNode.isMoreRelevantThan(oldNode) ? newNode.clone() : oldNode;
                });
            }
        }
    }

    public void exchangeWith(EncounteredNodeSet otherENS, DTNHost self, DTNHost peer, long currentTime) {
        EncounteredNodeSet otherClone = otherENS.clone();
        otherClone.removeEncounter(String.valueOf(peer.getAddress()));
        this.mergeENS(self, otherClone, currentTime, peer);
    }

    // ===================== 3. FILTER & REMOVAL ===================== //

    public void removeEncounter(String nodeId) {
        ensTable.remove(nodeId);
    }

    public void removeOldEncounters() {
        ensTable.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public boolean isEmpty() {
        return ensTable.isEmpty();
    }

    // ===================== 4. ANALISIS SOSIAL ===================== //

    public int countRecentEncounters(double currentTime, double timeWindow) {
        int count = 0;
        for (EncounteredNode node : ensTable.values()) {
            if ((currentTime - node.getEncounterTime()) <= timeWindow) count++;
        }
        return count;
    }

    public Set<String> getAllNodeIds() {
        return new HashSet<>(ensTable.keySet());
    }

    public EncounteredNodeSet clone() {
        EncounteredNodeSet cloned = new EncounteredNodeSet();
        for (Map.Entry<String, EncounteredNode> entry : ensTable.entrySet()) {
            cloned.ensTable.put(entry.getKey(), entry.getValue().clone());
        }
        return cloned;
    }

    // ===================== 5. ENCOUNTER FREQUENCY ===================== //

    public void recordEncounterBetween(DTNHost nodeA, DTNHost nodeB) {
        String aId = String.valueOf(nodeA.getAddress());
        String bId = String.valueOf(nodeB.getAddress());
        if (aId.compareTo(bId) > 0) { String temp = aId; aId = bId; bId = temp; }

        double time = SimClock.getTime();
        pairWiseEncounter.putIfAbsent(aId, new HashMap<>());
        pairWiseEncounter.get(aId).putIfAbsent(bId, new ArrayList<>());
        pairWiseEncounter.get(aId).get(bId).add(time);
    }

    public int getFrequencyBetween(DTNHost nodeA, DTNHost nodeB, double currentTime, double timeWindow) {
        String aId = String.valueOf(nodeA.getAddress());
        String bId = String.valueOf(nodeB.getAddress());
        if (aId.compareTo(bId) > 0) { String temp = aId; aId = bId; bId = temp; }

        List<Double> times = pairWiseEncounter.getOrDefault(aId, new HashMap<>()).getOrDefault(bId, new ArrayList<>());
        int count = 0;
        for (double t : times) {
            if ((currentTime - t) <= timeWindow) count++;
        }
        return count;
    }

    // ===================== 6. DEBUG & PRINT ===================== //

    public void printENS(String hostId) {
        if (ensTable.isEmpty()) {
            System.out.println("  (ENS KOSONG)");
        } else {
            for (EncounteredNode node : ensTable.values()) {
                System.out.printf("  NodeID: %-5s | Encounter: %-5d | Energy: %-5.1f | Buffer: %-5d | Duration: %-5ds\n",
                        node.getNodeId(),
                        node.getEncounterTime(),
                        node.getRemainingEnergy(),
                        node.getBufferSize(),
                        node.getConnectionDuration());
            }
        }
        System.out.println("============================================");
    }

    public void printEncounterLog(DTNHost host, String neighborId, EncounteredNodeSet neighborENS) {
        System.out.println("============================================");
        System.out.printf("Node %s bertemu dengan Node %s\n", host.getAddress(), neighborId);
        System.out.println("\n[ENS: " + host.getAddress() + "]");
        this.printENS(String.valueOf(host.getAddress()));
        System.out.println("\n[ENS: " + neighborId + "]");
        neighborENS.printENS(neighborId);
        System.out.println("============================================\n");
    }

    public void debugENS(DTNHost host) {
        System.out.println("[DEBUG] ENS Node: " + host.getAddress());
        if (ensTable.isEmpty()) {
            System.out.println("  (ENS kosong)");
        } else {
            for (Map.Entry<String, EncounteredNode> entry : ensTable.entrySet()) {
                System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
            }
        }
    }
}
