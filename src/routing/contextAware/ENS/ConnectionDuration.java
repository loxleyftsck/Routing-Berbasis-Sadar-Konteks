package routing.contextAware.ENS;

import core.DTNHost;
import core.SimClock;

import java.util.*;

/**
 * ConnectionDuration
 * Merekam dan mengelola durasi koneksi antar node dalam DTN.
 * Digunakan untuk menghitung tie-strength dan mengupdate ENS.
 */
public class ConnectionDuration {
    private DTNHost fromNode;
    private DTNHost toNode;
    private double startTime;
    private double endTime;
    private double totalDuration;

    private static Map<DTNHost, Map<DTNHost, ConnectionDuration>> connectionHistory = new HashMap<>();

    // ===== Constructor =====
    public ConnectionDuration(DTNHost fromNode, DTNHost toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.startTime = SimClock.getTime();
        this.endTime = -1; // -1 menandakan koneksi masih aktif
        this.totalDuration = 0;
    }

    // ===== Start Connection =====
    public static ConnectionDuration startConnection(DTNHost fromNode, DTNHost toNode) {
        connectionHistory.putIfAbsent(fromNode, new HashMap<>());

        // Jika koneksi sudah pernah dibuat, lanjutkan
        if (connectionHistory.get(fromNode).containsKey(toNode)) {
            ConnectionDuration existing = connectionHistory.get(fromNode).get(toNode);
            existing.startTime = SimClock.getTime();
            existing.endTime = -1;
            return existing;
        }

        // Jika belum, buat koneksi baru
        ConnectionDuration newConn = new ConnectionDuration(fromNode, toNode);
        connectionHistory.get(fromNode).put(toNode, newConn);
        return newConn;
    }

    // ===== End Connection =====
    public void endConnection(DTNHost fromNode, DTNHost toNode, EncounteredNodeSet encounteredNodeSet) {
        this.endTime = SimClock.getTime();
        double sessionDuration = endTime - startTime;
        this.totalDuration += sessionDuration;

        // Update EncounteredNodeSet (ENS) untuk kedua node
        String fromId = String.valueOf(fromNode.getAddress());
        String toId = String.valueOf(toNode.getAddress());
        long sessionDurationLong = (long) sessionDuration;

        encounteredNodeSet.updateConnectionDuration(fromId, sessionDurationLong);
        encounteredNodeSet.updateConnectionDuration(toId, sessionDurationLong);

        // Simpan kembali total durasi ke connectionHistory
        connectionHistory.get(fromNode).put(toNode, this);
    }

    // ===== Get Durasi Saat Ini =====
    public double getDuration() {
        if (endTime == -1) {
            return totalDuration + (SimClock.getTime() - startTime);
        }
        return totalDuration;
    }

    // ===== Static Accessors =====
    public static ConnectionDuration getConnection(DTNHost from, DTNHost to) {
        return connectionHistory.getOrDefault(from, new HashMap<>()).get(to);
    }

    public static List<ConnectionDuration> getConnectionsFromHost(DTNHost host) {
        if (!connectionHistory.containsKey(host)) return new ArrayList<>();
        return new ArrayList<>(connectionHistory.get(host).values());
    }

    public static double getTotalConnectionDuration(DTNHost nodeA, DTNHost nodeB) {
        if (connectionHistory.containsKey(nodeA)) {
            Map<DTNHost, ConnectionDuration> map = connectionHistory.get(nodeA);
            if (map.containsKey(nodeB)) {
                return map.get(nodeB).getDuration();
            }
        }
        return 0.0;
    }

    public static void removeConnection(DTNHost fromNode, DTNHost toNode, double startTime) {
        if (connectionHistory.containsKey(fromNode)) {
            connectionHistory.get(fromNode).remove(toNode);
            if (connectionHistory.get(fromNode).isEmpty()) {
                connectionHistory.remove(fromNode);
            }
        }
    }

    // ===== Debug Tools =====
    public static void printConnectionHistory(DTNHost node) {
        if (connectionHistory.containsKey(node)) {
            for (Map.Entry<DTNHost, ConnectionDuration> entry : connectionHistory.get(node).entrySet()) {
                System.out.println("[DEBUG] Koneksi dari " + node.getAddress() +
                        " ke " + entry.getKey().getAddress() +
                        " durasi: " + entry.getValue().getDuration() + " detik.");
            }
        } else {
            System.out.println("[DEBUG] Tidak ada koneksi untuk node " + node.getAddress());
        }
    }

    public void printConnectionInfo(DTNHost host, DTNHost neighbor) {
        System.out.println("================================");
        System.out.println("Connection Info:");
        System.out.println("From Node: " + host.getAddress());
        System.out.println("To Node: " + neighbor.getAddress());
        System.out.println("Start Time: " + startTime);
        if (endTime == -1) {
            System.out.println("End Time: Still Active");
            System.out.println("Current Duration: " + (SimClock.getTime() - startTime) + " detik");
        } else {
            System.out.println("End Time: " + endTime);
            System.out.println("Final Duration: " + getDuration() + " detik");
        }
        System.out.println("================================");
    }

    // ===== Getter & Utility =====
    public boolean isActive() {
        return endTime == -1;
    }

    public double getEndTime() {
        return this.endTime;
    }

    public DTNHost getFromNode() {
        return fromNode;
    }

    public DTNHost getToNode() {
        return toNode;
    }
}
