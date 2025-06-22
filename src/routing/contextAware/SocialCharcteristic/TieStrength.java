package routing.contextAware.SocialCharcteristic;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ENS.ConnectionDuration;
import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.HashMap;
import java.util.Map;

/**
 * TieStrength
 * Menghitung kekuatan hubungan sosial antara dua node berdasarkan:
 * 1. Frekuensi encounter (jumlah pertemuan)
 * 2. Closeness (total durasi koneksi)
 * 3. Recency (berapa lama sejak encounter terakhir)
 *
 * Semua faktor dinormalisasi ke [0–1] dan diberi bobot, menghasilkan skor akhir ∈ [0–1].
 */
public class TieStrength {

    private static final Map<DTNHost, Map<DTNHost, Double>> tieStrengthMap = new HashMap<>();

    // Bobot komponen
    private static final double FREQUENCY_WEIGHT = 0.5;
    private static final double CLOSENESS_WEIGHT = 0.2;
    private static final double RECENCY_FACTOR = 0.3;

    // Normalisasi
    private static final double MAX_FREQUENCY = 15.0;
    private static final double MAX_CLOSENESS = 900.0; // 15 menit
    private static final double RECENCY_DECAY_SCALE = 1000.0; // semakin besar = decay lebih lambat

    /**
     * Hitung frekuensi encounter dalam time window tertentu.
     */
    public static int calculateFrequency(DTNHost host, DTNHost neighbor, EncounteredNodeSet ens) {
        double now = SimClock.getTime();
        double timeWindow = 600.0;
        return ens.getFrequencyBetween(host, neighbor, now, timeWindow);
    }

    /**
     * Ambil total durasi koneksi (closeness) antar dua node.
     */
    public static double calculateCloseness(DTNHost host, DTNHost neighbor) {
        return ConnectionDuration.getTotalConnectionDuration(host, neighbor);
    }

    /**
     * Hitung waktu yang berlalu sejak encounter terakhir.
     */
    public static double calculateRecency(DTNHost host, DTNHost neighbor) {
        ConnectionDuration conn = ConnectionDuration.getConnection(host, neighbor);
        if (conn != null) {
            return SimClock.getTime() - conn.getEndTime();
        }
        return Double.MAX_VALUE; // belum pernah bertemu
    }

    /**
     * Hitung TieStrength antar dua node, dan simpan ke map.
     */
    public void calculateTieStrength(DTNHost host, DTNHost neighbor, EncounteredNodeSet ens) {
        // 1. Frequency
        int freq = calculateFrequency(host, neighbor, ens);
        double normFreq = normalize(freq, MAX_FREQUENCY);

        // 2. Closeness
        double duration = calculateCloseness(host, neighbor);
        double normCloseness = normalize(duration, MAX_CLOSENESS);

        // 3. Recency decay (semakin baru, semakin kuat)
        double recency = calculateRecency(host, neighbor);
        double decay = Math.exp(-recency / RECENCY_DECAY_SCALE);

        // 4. Aggregate Score
        double baseScore = (FREQUENCY_WEIGHT * normFreq) + (CLOSENESS_WEIGHT * normCloseness);
        double finalScore = baseScore * (1 + RECENCY_FACTOR * decay);

        // Clamp to [0,1]
        finalScore = Math.max(0.0, Math.min(finalScore, 1.0));

        // Simpan
        tieStrengthMap.computeIfAbsent(host, k -> new HashMap<>()).put(neighbor, finalScore);

//        System.out.printf("[TieStrength] %s -> %s | Freq: %d, Closeness: %.1f, Recency: %.1f, Final: %.3f\n",
//                host.getAddress(), neighbor.getAddress(), freq, duration, recency, finalScore);
    }

    /**
     * Ambil nilai tieStrength jika sudah dihitung.
     */
    public double getTieStrength(DTNHost host, DTNHost neighbor) {
        return tieStrengthMap.getOrDefault(host, new HashMap<>()).getOrDefault(neighbor, 0.0);
    }

    /**
     * Fungsi normalisasi ke skala 0–1.
     */
    private static double normalize(double value, double max) {
        return Math.min(value / max, 1.0);
    }
}
