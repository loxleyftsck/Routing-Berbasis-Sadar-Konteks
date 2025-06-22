package reinforcementLearning_ContextAware;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ContextAwareRLRouter;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.*;

import static reinforcementLearning_ContextAware.Qtable.printQTable;

/**
 * QTableUpdateStrategy adalah kelas yang mengatur pembaruan Q-table
 * berdasarkan tiga strategi dalam algoritma Q-learning:
 * 1. Pembaruan saat koneksi aktif (Q-learning standar)
 * 2. Aging (peluruhan nilai Q setelah koneksi putus)
 * 3. Sinkronisasi dua arah antar Q-table
 */
public class QTableUpdateStrategy {

    private Qtable qtable; // Q-table milik node saat ini

    // Parameter Q-learning
    private static final double GAMMA = 0.4; // diskonto
    private static final double ALPHA = 0.6; // learning rate
    private static final double AGING_CONSTANT = 0.998; // peluruhan eksponensial
    public static final double MIN_ELAPSED_FOR_AGING = 240.0; // minimal durasi untuk aging
    private static final double MIN_Q = 0.05; // batas bawah Q-value

    public QTableUpdateStrategy(Qtable qtable) {
        this.qtable = qtable;
    }

    /**
     * Menghitung reward dari perspektif neighbor berdasarkan
     * apakah destinationId ada dalam ENS miliknya.
     */
    public double calculateReward(EncounteredNodeSet ensNeighbor, String destinationId) {
        return ensNeighbor.getAllNodeIds().contains(destinationId) ? 1.0 : 0.0;
    }

    /**
     * Strategi 1 - Pembaruan Q-value berdasarkan koneksi aktif.
     * Menggunakan reward, maxQ, dan faktor fuzzy opportunity.
     */
    public void updateFirstStrategy(DTNHost host, DTNHost neighbor, String destinationId, String nextHop, double fuzzOpp) {
        String hostId = String.valueOf(host.getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());
        double qCurrent = qtable.getQvalue(destinationId, nextHop);
        EncounteredNodeSet ensNeighbor = ((ContextAwareRLRouter) neighbor.getRouter()).getEncounteredNodeSet();
        double reward = calculateReward(ensNeighbor, destinationId);
        double maxQ = qtable.getMaxQvalue(destinationId, ensNeighbor);
        double newQ = ALPHA * (reward + GAMMA * fuzzOpp * maxQ) + (1 - ALPHA) * qCurrent;
        qtable.updateQvalue(destinationId, nextHop, newQ);
    }

    /**
     * Menjalankan decay nilai Q untuk tetangga yang koneksinya telah lama putus.
     */
    public void processDelayedAging(DTNHost host, Map<String, Double> pendingAging) {
        double now = SimClock.getTime();
        List<String> expired = new ArrayList<>();
        Set<String> keysSnapshot = new HashSet<>(pendingAging.keySet());

        for (String neighborId : keysSnapshot) {
            double endTime = pendingAging.get(neighborId);
            double elapsed = now - endTime;
            if (elapsed >= MIN_ELAPSED_FOR_AGING) {
                List<ConnectionDuration> connections = ConnectionDuration.getConnectionsFromHost(host);
                for (ConnectionDuration cd : connections) {
                    if (String.valueOf(cd.getToNode().getAddress()).equals(neighborId)) {
                        if (updateSecondStrategy(host, cd.getToNode())) {
                            expired.add(neighborId);
                        }
                        break;
                    }
                }
            }
        }
        for (String id : expired) {
            pendingAging.remove(id);
        }
    }

    /**
     * Strategi 2 - Penurunan Q-value dengan peluruhan eksponensial
     * untuk koneksi yang tidak aktif dalam waktu tertentu.
     */
    public boolean updateSecondStrategy(DTNHost host, DTNHost neighbor) {
        String nexthop = String.valueOf(neighbor.getAddress());
        ConnectionDuration connection = ConnectionDuration.getConnection(host, neighbor);
        if (connection == null || connection.getEndTime() == -1) return false;

        double elapsedTime = SimClock.getTime() - connection.getEndTime();
        if (elapsedTime < MIN_ELAPSED_FOR_AGING) return false;

        Map<String, Map<String, Double>> allQ = qtable.getAllQvalues();
        boolean updated = false;

        for (Map.Entry<String, Map<String, Double>> entry : allQ.entrySet()) {
            String destinationId = entry.getKey();
            Map<String, Double> actionMap = entry.getValue();
            if (!actionMap.containsKey(nexthop)) continue;
            double currentQ = actionMap.get(nexthop);
            double decayFactor = Math.pow(AGING_CONSTANT, elapsedTime);
            double agedQ = Math.max(currentQ * decayFactor, MIN_Q);
            qtable.updateQvalue(destinationId, nexthop, agedQ);
            updated = true;
        }
        return updated;
    }

    /**
     * Strategi 3 - Sinkronisasi dua arah antar Q-table antara dua node.
     */
    public static void updateThirdStrategy(Qtable senderQtable, Qtable receiverQtable, String sender, String receiver) {
        syncQEntries(senderQtable, receiverQtable);
        syncQEntries(receiverQtable, senderQtable);
    }

    /**
     * Sinkronisasi satu arah dari source ke target Q-table.
     */
    private static void syncQEntries(Qtable target, Qtable source) {
        for (String state : source.getAllDestinations()) {
            if (state.equals(target.getOwnerId())) continue;
            Map<String, Double> sourceActions = source.getActionMap(state);
            if (sourceActions == null) continue;

            for (Map.Entry<String, Double> entry : sourceActions.entrySet()) {
                String nextHop = entry.getKey();
                double sourceQ = entry.getValue();

                if (nextHop.equals(target.getOwnerId())) continue;

                if (target.hasAction(state, nextHop)) {
                    double targetQ = target.getQvalue(state, nextHop);
                    if (targetQ != 0.0 && sourceQ != 0.0) {
                        if (targetQ < sourceQ) {
                            target.updateQvalue(state, nextHop, sourceQ);
                        } else if (targetQ > sourceQ) {
                            source.updateQvalue(state, nextHop, targetQ);
                        }
                    }
                }
            }
        }
    }
}
