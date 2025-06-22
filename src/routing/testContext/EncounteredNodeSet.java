package routing.testContext;

import core.SimClock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Kelas EncounteredNodeSet digunakan untuk menyimpan informasi tentang node-node
 * yang telah bertemu dalam jaringan. Data encounter ini digunakan dalam routing
 * berbasis konteks.
 */
public class EncounteredNodeSet {

    private final Map<Integer, EncounterInfo> encounteredNodes = new HashMap<>();
    private static final double EXPIRATION_TIME = 1000.0; // 60 detik dalam unit waktu simulasi

    /**
     * Konstruktor untuk inisialisasi EncounteredNodeSet dengan data encounter yang diberikan.
     * @param encounteredNodes Map yang berisi data encounter awal.
     */
    public EncounteredNodeSet(Map<Integer, EncounterInfo> encounteredNodes) {
        if (encounteredNodes != null) {
            this.encounteredNodes.putAll(encounteredNodes);
        }    }

    public void merge(EncounteredNodeSet neighbor) {
        for (Map.Entry<Integer, EncounterInfo> entry : neighbor.getEncounters().entrySet()) {
            int nodeId = entry.getKey();
            EncounterInfo newInfo = entry.getValue();

            if (this.isNewerEncounter(nodeId, newInfo)) {
                this.encounteredNodes.put(nodeId, newInfo);
            }
        }
    }

    public EncounteredNodeSet clone() {
        Map<Integer, EncounterInfo> copiedEncounters = new HashMap<>(this.encounteredNodes);
        return new EncounteredNodeSet(copiedEncounters);
    }
    /**
     * Mengecek apakah encounter dengan node tertentu lebih baru dibandingkan data yang ada.
     * @param nodeId ID node yang ingin diperiksa.
     * @param encounterInfo Informasi encounter terbaru dari node tersebut.
     * @return True jika encounter baru lebih baru dari data yang ada, false jika tidak.
     */
    private boolean isNewerEncounter(int nodeId, EncounterInfo encounterInfo) {
        EncounterInfo existingInfo = encounteredNodes.get(nodeId);
        if (existingInfo == null) {
            return true; // Encounter baru otomatis lebih baru jika belum ada data
        }
        return (encounterInfo.MeetingTime > existingInfo.MeetingTime) ||
                (encounterInfo.MeetingTime == existingInfo.MeetingTime && encounterInfo.RemainingBuffer < existingInfo.RemainingBuffer) ||
                (encounterInfo.MeetingTime == existingInfo.MeetingTime && encounterInfo.RemainingEnergy < existingInfo.RemainingEnergy);
     }
    /**
     * Menambahkan encounter baru ke dalam ENS.
     * @param nodeId ID node yang ditemui.
     * @param encounterInfo Informasi encounter yang mencakup waktu pertemuan, buffer, energi, dan durasi koneksi.
     */
    public void addEncounter(int nodeId, EncounterInfo encounterInfo ) {
        if (isNewerEncounter(nodeId, encounterInfo)) {
            encounteredNodes.put(nodeId, encounterInfo);
        }
//        encounteredNodes .put(nodeId, encounterInfo);
    }

    /**
     * Memperbarui durasi koneksi untuk node yang sudah ada dalam ENS.
     * @param nodeId ID node yang ingin diperbarui.
     * @param newDuration Durasi koneksi baru dalam satuan waktu simulasi.
     */
    public void updateEncounterDuration(int nodeId, double newDuration) {
        Optional.ofNullable(encounteredNodes.get(nodeId))
                .ifPresent(encounter -> {
                    encounter.connDuration = newDuration;
                    encounter.lastSeenTime = SimClock.getTime();
                });
    }

    /**
     * Menghapus encounter berdasarkan nodeId dari ENS.
     * @param nodeId ID node yang ingin dihapus dari ENS.
     */
    public void removeEncounter(int nodeId) {
//        System.out.println("ENS Saat Ini: " + encounteredNodes .keySet());
        if (encounteredNodes .containsKey(nodeId)) {
            encounteredNodes .remove(nodeId);  // Menghapus encounter untuk node yang terputus
        }
    }

    /**
     * Membersihkan encounter yang sudah lebih dari 60 detik dalam waktu simulasi.
     * Encounter yang kadaluwarsa akan dihapus dari ENS.
     */
    public void cleanOldEncounters() {
        double currentTime = SimClock.getTime(); // Ambil waktu simulasi saat ini

        Iterator<Map.Entry<Integer, EncounterInfo>> iterator = encounteredNodes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, EncounterInfo> entry = iterator.next();

            double age = currentTime - entry.getValue().lastSeenTime;

            if (age >= EXPIRATION_TIME) {
                iterator.remove();
            }
        }
    }

    /**
     * Mengembalikan jumlah encounter yang masih dalam periode waktu tertentu.
     * @param timeWindow Waktu dalam detik untuk mengambil encounter (misalnya 200 detik)
     * @return Jumlah node yang masih dalam timeWindow terakhir.
     */
    public int getEncountersWithinTimeWindow(double timeWindow) {
        double currentTime = SimClock.getTime();
        int count = 0;

        for (EncounterInfo info : encounteredNodes.values()) {
            if ((currentTime - info.lastSeenTime) <= timeWindow) {
                count++;
            }
        }

        return count;
    }

    /**
     * Menghitung jumlah encounter untuk node tertentu dalam 200 detik terakhir.
     * @param nodeId ID node yang dicek
     * @return Jumlah encounter dari nodeId yang diberikan dalam 200 detik terakhir.
     */
    public int getRecentEncounterCount(int nodeId) {
        double currentTime = SimClock.getTime(); // Mendapatkan waktu simulasi saat ini

        if (encounteredNodes.containsKey(nodeId)) {  // Jika node ada di ENS
            EncounterInfo info = encounteredNodes.get(nodeId);  // Ambil informasi encounter node

            if (info != null) {
                double timeSinceLastSeen = currentTime - info.lastSeenTime;  // Hitung waktu sejak terakhir node terlihat

                // Periksa apakah encounter terjadi dalam 200 detik terakhir
                if (timeSinceLastSeen <= 200.0) {
                    return 1;  // Node ditemui dalam 200 detik terakhir
                }
            }
        }

        return 0;  // Node tidak ditemui dalam 200 detik terakhir
    }
     /**
      * Mengembalikan daftar encounter yang tersimpan dalam ENS.
      * @return Map yang berisi informasi encounter dengan key sebagai nodeId.
     */
    public Map<Integer, EncounterInfo> getEncounters() {
        return encounteredNodes ;
    }

    /**
     * Mengembalikan jumlah node yang terdapat dalam ENS.
     * @return Jumlah encounter yang tersimpan dalam ENS.
     */
    public int getEncounterCount(){
        return encounteredNodes .size();
    }


    /**
     * Mencetak isi ENS untuk debugging
     */
    public void printENS() {
        if (encounteredNodes.isEmpty()) {
            System.out.println("⚠️ ENS KOSONG!");
        } else {
            for (Map.Entry<Integer, EncounterInfo> entry : encounteredNodes.entrySet()) {
                EncounterInfo info = entry.getValue();
                System.out.printf("🔹 Node %d | Last Seen: %.2f | Durasi: %.2f | Buffer: %.2f | Energi: %.2f%n",
                        entry.getKey(), info.lastSeenTime, info.connDuration, info.RemainingBuffer, info.RemainingEnergy);
            }
        }
    }
}
