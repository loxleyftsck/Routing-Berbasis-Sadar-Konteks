package routing.contextAware.Crips;

import core.DTNHost;

/**
 * CripsContextMsg
 * Kelas ini mengevaluasi prioritas pesan dalam jaringan DTN berdasarkan
 * Time-To-Live (TTL) dan Hop Count secara diskrit (crisp rulebase).
 */
public class CripsContextMsg {

    // Enumerasi level diskrit
    private enum Level { SMALL, MEDIUM, LARGE }
    public enum MsgPriority { LOW, NORMAL, HIGH, URGENT }

    /**
     * Mengklasifikasikan TTL yang telah dinormalisasi ke level diskrit.
     * TTL kecil = prioritas tinggi.
     */
    private static Level getTtlLevel(double ttl) {
        if (ttl <= 0.4) return Level.SMALL;
        else if (ttl <= 0.7) return Level.MEDIUM;
        else return Level.LARGE;
    }

    /**
     * Mengklasifikasikan Hop Count yang telah dinormalisasi ke level diskrit.
     * Hop besar = sudah banyak berpindah = potensi nyebar luas.
     */
    private static Level getHopLevel(double hop) {
        if (hop <= 0.4) return Level.SMALL;
        else if (hop <= 0.7) return Level.MEDIUM;
        else return Level.LARGE;
    }

    /**
     * Menentukan prioritas pesan dari kombinasi level TTL dan Hop Count.
     * Rule ini diturunkan dari fuzzy logic manual.
     */
    public static MsgPriority getPriority(Level ttl, Level hop) {
        if (ttl == Level.SMALL && hop == Level.LARGE) return MsgPriority.URGENT;
        if (ttl == Level.SMALL && hop == Level.MEDIUM) return MsgPriority.HIGH;
        if (ttl == Level.SMALL && hop == Level.SMALL) return MsgPriority.NORMAL;

        if (ttl == Level.MEDIUM && hop == Level.LARGE) return MsgPriority.HIGH;
        if (ttl == Level.MEDIUM && hop == Level.MEDIUM) return MsgPriority.NORMAL;
        if (ttl == Level.MEDIUM && hop == Level.SMALL) return MsgPriority.LOW;

        if (ttl == Level.LARGE && hop == Level.LARGE) return MsgPriority.HIGH;
        if (ttl == Level.LARGE && hop == Level.MEDIUM) return MsgPriority.NORMAL;
        if (ttl == Level.LARGE && hop == Level.SMALL) return MsgPriority.NORMAL;

        return MsgPriority.LOW; // fallback default
    }

    /**
     * Konversi prioritas pesan ke nilai biner.
     * HIGH/URGENT = 1.0 (prioritas forwarding), lainnya = 0.0
     */
    public static double priorityBinary(MsgPriority prio) {
        switch (prio) {
            case URGENT:
            case HIGH:
                return 1.0;
            case NORMAL:
            case LOW:
            default:
                return 0.0;
        }
    }

    /**
     * Fungsi utama untuk mengevaluasi prioritas pesan berdasarkan TTL dan HopCount.
     * Nilai dinormalisasi dan diklasifikasikan ke level, lalu dinilai melalui rule.
     *
     * @param host DTNHost pengirim (tidak digunakan di sini, disiapkan untuk konteks lanjutan)
     * @param msgTTL Time-to-live pesan (dalam detik)
     * @param msgHopCount Jumlah hop pesan saat ini
     * @return Nilai prioritas: 1.0 jika prioritas tinggi (HIGH/URGENT), 0.0 jika tidak
     */
    public double evaluateMsg(DTNHost host, int msgTTL, int msgHopCount) {
        double maxTtl = 240.0;     // Sesuaikan dengan TTL maksimum di simulasi
        double maxHop = 10.0;      // Sesuaikan dengan batas hop maksimum

        // Normalisasi nilai input
        double normalizedTtl = 1.0 - Math.min(msgTTL / maxTtl, 1.0);  // Semakin kecil TTL, semakin tinggi prioritas
        double normalizedHop = Math.min(msgHopCount / maxHop, 1.0);  // Semakin besar hop, berarti sudah menyebar

        // Klasifikasi ke dalam level
        Level ttlLevel = getTtlLevel(normalizedTtl);
        Level hopLevel = getHopLevel(normalizedHop);

        // Evaluasi prioritas pesan
        MsgPriority priority = getPriority(ttlLevel, hopLevel);
        return priorityBinary(priority);
    }
}
