package routing.testContext;

import core.DTNHost;
import core.SimClock;
//import routing.contextAware.ContextAwareRLRouter;
//import routing.contextAware.ENS.ConnectionDuration;

import java.util.Collections;
import java.util.Map;
//
///**
// * Kelas EncounterManager mengelola EncounteredNodeSet (ENS) untuk menyimpan informasi tentang node-node yang telah bertemu.
// * Kelas ini menangani penambahan, pembaruan, penghapusan, pertukaran, dan pembersihan ENS dalam jaringan Delay Tolerant Network (DTN).
// */
//public class EncounterManager {
//
//    private final EncounteredNodeSet encounteredNodeSet;
//    private double lastCleanupTime = 0; // Waktu terakhir pembersihan
//
//    /**
//     * Konstruktor untuk EncounterManager yang menginisialisasi EncounteredNodeSet baru.
//     */
//    public EncounterManager() {
//        encounteredNodeSet = new EncounteredNodeSet(Collections.emptyMap());
//    }
//
//    /**
//     * Mengembalikan objek EncounteredNodeSet yang dikelola oleh EncounterManager.
//     * @return EncounteredNodeSet yang berisi informasi encounter node.
//     */
//    public EncounteredNodeSet getEncounteredNodeSet() {
//        return encounteredNodeSet;
//    }
//
//
//    /**
//     * Menambahkan encounter baru ke dalam ENS.
//     * @param nodeId ID node yang ditemui.
//     * @param meetingTime Waktu pertemuan dalam simulasi.
//     * @param remainingBuffer Kapasitas buffer yang tersisa.
//     * @param remainingEnergy Energi yang tersisa.
//     * @param connectionDuration Durasi koneksi dengan node yang ditemui.
//     */
//    public void addEncounterToENS(int nodeId, double meetingTime, int remainingBuffer, double remainingEnergy, double connectionDuration) {
//        encounteredNodeSet.addEncounter(nodeId, new EncounterInfo(meetingTime, remainingBuffer, remainingEnergy, connectionDuration));
//    }
//
//    /**
//     * Memperbarui durasi koneksi dalam ENS berdasarkan informasi dari koneksi antara dua node.
//     * @param fromNode Node sumber.
//     * @param toNode Node tujuan.
//     */
//    public void updateConnectionDurationInENS(DTNHost fromNode, DTNHost toNode) {
////        ConnectionDuration conn = ConnectionDuration.getConnection(fromNode, toNode);
////        if (conn != null) {
////            int nodeId = toNode.getAddress();
////            encounteredNodeSet.updateEncounterDuration(nodeId, conn.getDuration());
//        }
//    }
//
//    /**
//     * Mengecek apakah encounter dengan node tertentu lebih baru dibandingkan data yang ada.
//     * @param nodeId ID node yang ingin diperiksa.
//     * @param encounterInfo Informasi encounter terbaru dari node tersebut.
//     * @return True jika encounter baru lebih baru dari data yang ada, false jika tidak.
//     */
//    private boolean isNewerEncounter(int nodeId, EncounterInfo encounterInfo) {
//        EncounterInfo existingInfo = encounteredNodeSet.getEncounters().get(nodeId);
//        return existingInfo == null || encounterInfo.MeetingTime > existingInfo.MeetingTime
//                || encounterInfo.RemainingBuffer < existingInfo.RemainingBuffer
//                || encounterInfo.RemainingEnergy < existingInfo.RemainingEnergy;
//    }
//
//
//    /**
//     * Menukar ENS antara dua node untuk berbagi informasi tentang encounter yang mereka miliki.
//     * @param neighborManager EncounterManager milik node tetangga.
//     */
//    public void exchangeENS(EncounterManager neighborManager) {
//        if (this.encounteredNodeSet != null && neighborManager.getEncounteredNodeSet() != null) {
//            // Ambil ENS Map
//            Map<Integer, EncounterInfo> myENSMap = this.encounteredNodeSet.getEncounters();
//            Map<Integer, EncounterInfo> neighborENSMap = neighborManager.getEncounteredNodeSet().getEncounters();
//
//            // Lakukan merge ENS dari node tetangga
//            for (Map.Entry<Integer, EncounterInfo> entry : neighborENSMap.entrySet()) {
//                if (!myENSMap.containsKey(entry.getKey())) {
//                    myENSMap.put(entry.getKey(), entry.getValue());  // Menambah ENS dari tetangga
//                } else {
//                    //Cek apakah encounter dari tetangga lebih baru
//                    EncounterInfo myInfo = myENSMap.get(entry.getKey());
//                    EncounterInfo neighborInfo = entry.getValue();
//
//                    if (neighborInfo.MeetingTime > myInfo.MeetingTime) {
//                        myENSMap.put(entry.getKey(), neighborInfo);  // Update ENS jika encounter lebih baru
//                    }
//                }
//            }
//
//        }
//    }
//
//    /**
//     * Memperbarui ENS saat koneksi ke suatu node terputus dengan menghapus encounter terkait.
//     * @param nodeId ID node yang koneksinya terputus.
//     */
//    public void updateENSOnConnectionDown(int nodeId) {
//        // Menghapus encounter yang terputus (misalnya Node C)
////        removeEncounterFromENS(nodeId);
//
//        if (encounteredNodeSet.getEncounters().containsKey(nodeId)) {
////            System.out.println("updateENSOnConnectionDown: Menghapus node " + nodeId);
//            removeEncounterFromENS(nodeId);
//        } else {
////            System.out.println("updateENSOnConnectionDown: Node " + nodeId + " sudah tidak ada di ENS.");
//        }
//    }
//
//    /**
//     * Menghapus encounter dari ENS berdasarkan ID node.
//     * @param nodeId ID node yang akan dihapus dari ENS.
//     */
//    public synchronized void removeEncounterFromENS(int nodeId) {
//        if (encounteredNodeSet.getEncounters().containsKey(nodeId)) {
////            System.out.println("Menghapus node " + nodeId + " dari ENS...");
//            encounteredNodeSet.removeEncounter(nodeId);  // Menghapus encounter berdasarkan nodeId
//
//            // Pastikan node benar-benar terhapus
//            if (!encounteredNodeSet.getEncounters().containsKey(nodeId)) {
////                System.out.println("Node " + nodeId + " berhasil dihapus dari ENS.");
//            }
//        }
//    }
//
//    /**
//     * Memeriksa apakah perlu melakukan pembersihan ENS berdasarkan waktu simulasi.
//     * Pembersihan dilakukan setiap 60 detik dalam waktu simulasi.
//     */
//    public void checkAndCleanENS() {
//        double currentTime = SimClock.getTime(); // Ambil waktu simulasi
//        if (currentTime - lastCleanupTime >= 60.0) {
//            encounteredNodeSet.cleanOldEncounters();
//            lastCleanupTime = currentTime; // Perbarui waktu terakhir pembersihan
//        }
//    }
//
//    /**
//     * Mengembalikan jumlah encounter yang tersimpan dalam ENS.
//     * @return Jumlah encounter yang ada di ENS.
//     */
//    public int getEncounterCount() {
//        return encounteredNodeSet.getEncounterCount();
//    }
//
//    /**
//     * Mencetak informasi ENS untuk node tertentu.
//     * @param host Node yang ENS-nya akan dicetak.
//     */
//    public void printENS(DTNHost host) {
//        // Mendapatkan router untuk host
//        ContextAwareRLRouter router = (ContextAwareRLRouter) host.getRouter();
//
//        // Mendapatkan EncounterManager dari router
////        EncounterManager encounterManager = router.getEncounterManage();
//
//        // Mendapatkan EncounteredNodeSet
////        EncounteredNodeSet encounteredNodeSet = encounterManager.getEncounteredNodeSet();
//
//
//        // Mencetak jumlah encounter yang ada
////        System.out.println("Node " + host.getAddress() + " memiliki " + encounteredNodeSet.getEncounterCount() + " encounter(s):");
//
//        // Mencetak setiap encounter
//        for (Map.Entry<Integer, EncounterInfo> entry : encounteredNodeSet.getEncounters().entrySet()) {
//            int nodeId = entry.getKey();
//            EncounterInfo encounterInfo = entry.getValue();
//
//            // Mencetak informasi untuk setiap encounter
//            System.out.println("Node ID: " + nodeId + ", Meeting Time: " + encounterInfo.MeetingTime +
//                    ", Remaining Buffer: " + encounterInfo.RemainingBuffer +
//                    ", Remaining Energy: " + encounterInfo.RemainingEnergy +
//                    ", Connection Duration: " + encounterInfo.connDuration);
//        }
//        System.out.println("====================");
//    }
//}