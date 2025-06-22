package reinforcementLearning_ContextAware;

import routing.contextAware.ENS.EncounteredNodeSet;

import java.io.FileWriter;
import java.util.*;
import java.io.IOException;

/**
 * Kelas Qtable menyimpan dan mengelola nilai Q-learning untuk setiap kombinasi tujuan (destination) dan
 * tetangga (nextHop) dalam bentuk Map bersarang. Q-value menunjukkan nilai estimasi reward untuk memilih
 * nextHop tertentu guna mencapai destination.
 */
public class Qtable {

    // ID dari node pemilik Q-table ini
    private String ownerId;

    // Struktur Q-table: Map<destinationId, Map<nextHopId, Q-value>>
    private Map<String, Map<String, Double>> qtable;

    /**
     * Konstruktor Qtable.
     * @param ownerId ID dari node yang memiliki Q-table ini.
     */
    public Qtable(String ownerId) {
        this.ownerId = ownerId;
        this.qtable = new HashMap<>();
    }

    /**
     * Mengembalikan ID pemilik Q-table.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Inisialisasi semua kombinasi tujuan dan nextHop (selain owner) ke nilai Q = 0.0.
     * @param allNodeIds Set berisi semua ID node dalam jaringan.
     */
    public void initializeAllQvalues(Set<String> allNodeIds) {
        for (String destinationId : allNodeIds) {
            if (destinationId.equals(ownerId)) continue;
            for (String nextHop : allNodeIds) {
                if (nextHop.equals(ownerId)) continue;
                updateQvalue(destinationId, nextHop, 0.0);
            }
        }
    }

    /**
     * Mengambil nilai Q dari kombinasi (destinationId, nextHop).
     * @return Q-value atau 0.0 jika belum diinisialisasi.
     */
    public double getQvalue(String destinationId, String nextHop) {
        Map<String, Double> nextHopMap = qtable.get(destinationId);
        if (nextHopMap == null) return 0.0;
        return nextHopMap.getOrDefault(nextHop, 0.0);
    }

    /**
     * Memperbarui nilai Q untuk (destinationId, nextHop). Nilai dibatasi maksimal 1.0.
     */
    public void updateQvalue(String destinationId, String nextHop, double qvalue) {
        qvalue = Math.min(qvalue, 1.0);
        qtable.computeIfAbsent(destinationId, k -> new HashMap<>()).put(nextHop, qvalue);
    }

    /**
     * Mengecek apakah kombinasi (destinationId, nextHop) sudah ada dalam Q-table.
     */
    public boolean hasAction(String destinationId, String nextHop) {
        return qtable.containsKey(destinationId) && qtable.get(destinationId).containsKey(nextHop);
    }

    /**
     * Mengambil semua ID tujuan (state) yang telah disimpan dalam Q-table.
     */
    public Set<String> getAllDestinations() {
        return qtable.keySet();
    }

    /**
     * Mengembalikan seluruh struktur Q-table.
     */
    public Map<String, Map<String, Double>> getAllQvalues() {
        return this.qtable;
    }

    /**
     * Mengambil map aksi (nextHop) dan Q-value-nya untuk sebuah tujuan tertentu.
     */
    public synchronized Map<String, Double> getActionMap(String destination) {
        return qtable.get(destination);
    }

    /**
     * Menghitung Q-value maksimum ke tujuan tertentu dari semua node yang pernah ditemui (ENS).
     * @param destinationId ID tujuan.
     * @param ensNeighbor ENS node yang pernah ditemui.
     * @return Q-value maksimum atau 0.0 jika belum ada.
     */
    public double getMaxQvalue(String destinationId, EncounteredNodeSet ensNeighbor) {
        double maxQvalue = Double.NEGATIVE_INFINITY;
        for (String y : ensNeighbor.getAllNodeIds()) {
            double q = getQvalue(destinationId, y);
            maxQvalue = Math.max(maxQvalue, q);
        }
        return (maxQvalue == Double.NEGATIVE_INFINITY) ? 0.0 : maxQvalue;
    }

    /**
     * Representasi string dari Q-table.
     */
    public String toString() {
        return "Qtable milik" + ownerId + ":\n" + qtable.toString();
    }

    /**
     * Mengekspor isi Q-table ke file CSV (dalam format pivot).
     * @param filePath Path file tujuan.
     * @param append Jika true, data akan ditambahkan ke file.
     */
    public void exportToCSV(String filePath, boolean append) {
        System.out.println("Menulis ke file: " + filePath);
        int columnWidth = 12;
        try (FileWriter writer = new FileWriter(filePath, append)) {
            Set<String> allNextHops = new TreeSet<>();
            for (Map<String, Double> map : qtable.values()) {
                allNextHops.addAll(map.keySet());
            }

            writer.append(padRight("Qtab nd " + getOwnerId(), columnWidth));
            for (String nextHop : allNextHops) {
                writer.append(padRight("Action " + nextHop, columnWidth));
            }
            writer.append("\n");

            for (String state : new TreeSet<>(qtable.keySet())) {
                writer.append(padRight("State " + state, columnWidth));
                for (String nextHop : allNextHops) {
                    double qvalue = qtable.getOrDefault(state, new HashMap<>())
                            .getOrDefault(nextHop, 0.0);
                    writer.append(padRight(String.format("%.4f", qvalue), columnWidth));
                }
                writer.append("\n");
            }

            writer.append("\n");
        } catch (IOException e) {
            System.err.println("Error writing pivoted Q-table to CSV: " + e.getMessage());
        }
    }

    private String padRight(String str, int width) {
        return String.format("%-" + width + "s", str);
    }

    /**
     * Mencetak isi Q-table ke konsol dalam format tabel baris-kolom.
     * @param qtable Objek Qtable yang ingin dicetak.
     * @param ownerId ID pemilik Qtable.
     */
    public static void printQTable(Qtable qtable, String ownerId) {
        Map<String, Map<String, Double>> qMap = qtable.getAllQvalues();
        Set<String> allActions = new TreeSet<>();
        for (Map<String, Double> actions : qMap.values()) {
            allActions.addAll(actions.keySet());
        }
        List<String> actionList = new ArrayList<>(allActions);

        System.out.println();
        System.out.printf("%-10s", "Qtab Nd" + ownerId);
        for (String action : actionList) {
            System.out.printf("%10s", "Act " + action);
        }
        System.out.println();

        List<String> sortedStates = new ArrayList<>(qMap.keySet());
        Collections.sort(sortedStates);

        for (String state : sortedStates) {
            Map<String, Double> actionMap = qMap.get(state);
            System.out.printf("%-10s", "State" + state);
            for (String action : actionList) {
                double q = actionMap.getOrDefault(action, 0.0);
                System.out.printf("%10.4f", q);
            }
            System.out.println();
        }
    }
}
