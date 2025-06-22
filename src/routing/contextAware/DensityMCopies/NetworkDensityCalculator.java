package routing.contextAware.DensityMCopies;

import routing.contextAware.ENS.EncounteredNodeSet;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * NetworkDensityCalculator
 * Menghitung contextual density dan menentukan jumlah salinan pesan
 * berdasarkan Encountered Node Set (ENS) dari node dan neighbor.
 */
public class NetworkDensityCalculator {

    private static final Random random = new Random();

    /**
     * Menghitung contextual node density dari ENS dua node.
     * Density = jumlah node unik yang pernah ditemui / total node.
     *
     * @param totalNodes  Jumlah total node di jaringan (misal 50).
     * @param hostENS     ENS dari node utama.
     * @param neighborENS ENS dari tetangga.
     * @param hostId      ID dari node utama (opsional).
     * @param neighborId  ID dari node tetangga (opsional).
     * @return Nilai densitas (0.0 – 1.0).
     */
    public static double calculateNodeDensity(int totalNodes,
                                              EncounteredNodeSet hostENS,
                                              EncounteredNodeSet neighborENS,
                                              String hostId,
                                              String neighborId) {

        Set<String> uniqueNodeIds = new HashSet<>();

        // Gabungkan ENS dari host dan neighbor
        if (hostENS != null) {
            hostENS.removeOldEncounters(); // buang yang kadaluarsa
            uniqueNodeIds.addAll(hostENS.getAllNodeIds());
        }

        if (neighborENS != null) {
            neighborENS.removeOldEncounters();
            uniqueNodeIds.addAll(neighborENS.getAllNodeIds());
        }

        // Tambahkan ID eksplisit bila ada
        if (hostId != null && !hostId.isEmpty()) {
            uniqueNodeIds.add(hostId);
        }
        if (neighborId != null && !neighborId.isEmpty()) {
            uniqueNodeIds.add(neighborId);
        }

        // Hindari pembagian nol
        if (totalNodes <= 0) return 0.0;

        return (double) uniqueNodeIds.size() / totalNodes;
    }

    /**
     * Menentukan jumlah salinan pesan berdasarkan kepadatan node.
     * Menentukan jumlah salinan pesan (L) berdasarkan node density.
     * Sesuai dengan paper, jumlah L dikontrol berdasarkan kepadatan area
     * untuk mengurangi overhead di area padat dan meningkatkan delivery di area jarang.
     * Rentang nilai random digunakan untuk memberikan fleksibilitas.
     * @param density Kepadatan jaringan
     * @return Jumlah salinan pesan
     */
    public static int calculateCopiesBasedOnDensity(double density) {
        if (density > 0.6) {

            return random.nextInt(25) + 5;
        } else if (density > 0.3) {

            return random.nextInt(80) + 80;
        } else {

            return random.nextInt(80) + 250;
        }
    }
}

//  if (density > 0.6) {
//        return random.nextInt(2) + 2;
//        } else if (density > 0.3) {
//        return random.nextInt(12) + 4;
//        } else {
//        return random.nextInt(12) + 6;
//        } fix 1
// if (density > 0.7) {
//        return random.nextInt(3) + 1;
//        } else if (density > 0.3) {
//        return random.nextInt(4) + 3;
//        } else {
//        return random.nextInt(5) + 8;
//        }