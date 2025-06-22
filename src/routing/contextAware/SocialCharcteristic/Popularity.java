package routing.contextAware.SocialCharcteristic;

import core.DTNHost;
import core.SimClock;
import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Kelas Popularity
 * Mengukur tingkat popularitas (aktivitas sosial) suatu node berdasarkan
 * jumlah encounter dalam jendela waktu tertentu (sliding time window).
 *
 * Nilai berada dalam rentang [0, 1] dan dihitung menggunakan exponential smoothing.
 */
public class Popularity {

    private final Map<DTNHost, Double> popularityMap = new HashMap<>();
    private final static int NUMth = 12;            // Threshold encounter untuk normalisasi
    private final static double TIME_WINDOW = 240.0; // Jendela waktu dalam detik
    private final double alphaPopularity;           // Faktor smoothing (0–1)

    /**
     * Inisialisasi class Popularity dengan smoothing factor alpha.
     *
     * @param alphaPopularity Faktor smoothing (semakin tinggi, semakin responsif terhadap encounter baru).
     */
    public Popularity(double alphaPopularity) {
        this.alphaPopularity = alphaPopularity;
    }

    /**
     * Memperbarui nilai popularitas node berdasarkan encounter terbaru dari ENS.
     *
     * @param node Node yang dievaluasi.
     * @param ens  EncounteredNodeSet milik node tersebut.
     */
    public void updatePopularity(DTNHost node, EncounteredNodeSet ens) {
        double currentTime = SimClock.getTime();
        int recentEncounter = ens.countRecentEncounters(currentTime, TIME_WINDOW);

        // Normalisasi encounter terhadap threshold
        double normalized = Math.min((double) recentEncounter / NUMth, 1.0);
        double prevPopularity = popularityMap.getOrDefault(node, 0.0);

        // Exponential Smoothing Update
        double updated = (1 - alphaPopularity) * prevPopularity + alphaPopularity * normalized;

        popularityMap.put(node, updated);

//        System.out.printf("[POPULARITY] Node %s | Encounters: %d | Pop: %.3f -> %.3f\n",
//                node.getAddress(), recentEncounter, prevPopularity, updated);
    }

    /**
     * Mengambil nilai popularitas node saat ini.
     *
     * @param node Node yang ingin diperiksa.
     * @return Nilai popularitas (rentang [0.0 – 1.0]).
     */
    public double getPopularity(DTNHost node) {
        return popularityMap.getOrDefault(node, 0.0);
    }
}
