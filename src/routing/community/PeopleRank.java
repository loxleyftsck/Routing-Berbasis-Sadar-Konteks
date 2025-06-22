/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

import core.Buffer;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.Tuple;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 * Implementasi algoritma PeopleRank untuk pengambilan keputusan routing. Kelas
 * ini mengimplementasikan antarmuka RoutingDecisionEngine untuk integrasi
 * dengan simulator DTN.
 *
 * Algoritma PeopleRank memberikan pentingnya pada node berdasarkan koneksi
 * sosial mereka, mirip dengan algoritma PageRank untuk halaman web.
 *
 * @author Pauwerfull
 */
public class PeopleRank implements RoutingDecisionEngine {

    // Konstanta untuk pengaturan faktor damping
    public static final String DUMPING_FACTOR_SETTING = "dumpingFactor";

    // Struktur data untuk menyimpan riwayat koneksi dan nilai PeopleRank
    Map<DTNHost, List<Set<DTNHost>>> connHistory;
    Map<DTNHost, Tuple<Double, Integer>> per = new HashMap<>();

    // Faktor damping yang digunakan dalam algoritma PeopleRank
    protected double dampingFactor = 0.75;

    /**
     * Konstruktor untuk menginisialisasi objek PeopleRank.
     *
     * @param s Objek Settings yang berisi parameter simulasi
     */
    public PeopleRank(Settings s) {
        // Set faktor damping dari pengaturan jika tersedia, jika tidak, gunakan nilai default
        if (s.contains(DUMPING_FACTOR_SETTING)) {
            dampingFactor = s.getDouble(DUMPING_FACTOR_SETTING);
        }
        // Inisialisasi map untuk menyimpan riwayat koneksi
        connHistory = new HashMap<>();
    }

    /**
     * Konstruktor salinan untuk mereplikasi objek PeopleRank.
     *
     * @param r Objek PeopleRank yang akan direplikasi
     */
    public PeopleRank(PeopleRank r) {
        // Salin faktor damping
        this.dampingFactor = r.dampingFactor;
        // Inisialisasi map baru untuk riwayat koneksi
        this.connHistory = new HashMap<>();
    }

    // Metode panggilan balik saat koneksi terhubung
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {}

    // Metode panggilan balik saat koneksi terputus
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Tambahkan informasi pemutusan koneksi ke riwayat koneksi
        addConnectionToHistory(thisHost, peer);
        addConnectionToHistory(peer, thisHost);
    }

    // Metode panggilan balik untuk pertukaran informasi saat koneksi baru terjadi
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // Dapatkan host lokal dan mesin keputusan PeopleRank dari host jarak jauh
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRank de = this.getOtherDecisionEngine(peer);

        // Periksa apakah myHost ada dalam riwayat koneksi
        if (connHistory.containsKey(myHost)) {
            // Dapatkan riwayat koneksi untuk myHost
            List<Set<DTNHost>> connectionHistory = connHistory.get(myHost);

            // Iterasi melalui riwayat koneksi
            for (Set<DTNHost> contactSet : connectionHistory) {
                // Jika myHost bertemu dengan peer
                if (contactSet.contains(peer)) {
                    // Perbarui PeopleRank untuk host terhubung
                    updatePeopleRank(myHost, de, contactSet);
                    break;
                }
            }
        }
    }

    /**
     * Metode untuk menambahkan informasi pemutusan koneksi ke riwayat koneksi.
     *
     * @param host Host lokal tempat koneksi terputus
     * @param peer Host rekan dengan mana koneksi terputus
     */
    private void addConnectionToHistory(DTNHost host, DTNHost peer) {
        // Dapatkan atau buat daftar riwayat koneksi untuk host
        List<Set<DTNHost>> hostHistory = connHistory.getOrDefault(host, new LinkedList<>());
        // Buat set yang berisi peer yang terhubung
        Set<DTNHost> connectedSet = new HashSet<>();
        connectedSet.add(peer);
        // Tambahkan set ke daftar riwayat koneksi
        hostHistory.add(connectedSet);
        // Perbarui map riwayat koneksi
        connHistory.put(host, hostHistory);
    }

    // Metode panggilan balik untuk pesan baru
    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    // Metode panggilan balik untuk menentukan apakah host saat ini adalah tujuan akhir pesan
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    // Metode panggilan balik untuk menentukan apakah pesan yang diterima harus disimpan oleh host saat ini
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    // Metode panggilan balik untuk menentukan apakah pesan harus dikirimkan ke host tertentu
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        double perThisHost = calculatePer(thisHost);
        double perOtherHost = calculatePer(otherHost);

        // Periksa apakah pesan ditujukan ke otherHost
        if (m.getTo() == otherHost) {
            return true;
        }

        // Periksa apakah otherHost ada dalam riwayat koneksi
        if (connHistory.containsKey(otherHost)) {
            // Iterasi melalui riwayat koneksi otherHost
            Iterator<Set<DTNHost>> iterator = connHistory.get(otherHost).iterator();
            while (iterator.hasNext()) {
                Set<DTNHost> contactSet = iterator.next();
                // Jika otherHost berhubungan dengan thisHost
                if (contactSet.contains(thisHost)) {
                    // Periksa apakah nilai PeopleRank otherHost lebih besar atau sama dengan nilai PeopleRank thisHost
                    // Jangan kirim pesan ke otherHost
                    return perOtherHost >= perThisHost; // Kirim pesan ke otherHost
                }
            }
        }

        // Jika host tujuan tidak ada dalam kontak dengan host saat ini, cek buffer pesan
        Buffer messageBuffer = new Buffer();
        int bufferSize = messageBuffer.getBufferSize(thisHost);
        Iterator<Message> bufferIterator = messageBuffer.Iterator();
        while (bufferSize > 0 && bufferIterator.hasNext()) {
            Message bufferedMessage = bufferIterator.next();
            // Jika nilai PeopleRank otherHost lebih besar atau sama dengan nilai PeopleRank thisHost atau otherHost adalah tujuan pesan, kirim pesan
            if (perOtherHost >= perThisHost || otherHost.equals(bufferedMessage.getTo())) {
                return true;
            }
        }
        return false; // Jika tidak, jangan kirim pesan ke otherHost
    }

    // Metode panggilan balik untuk menentukan apakah pesan yang dikirim harus dihapus
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        // Tidak perlu melakukan tindakan khusus ketika pesan dikirim
        return false;
    }

    // Metode panggilan balik untuk menentukan apakah pesan lama harus dihapus
    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld; // Hapus pesan jika ditujukan untuk host ini
    }

    // Metode panggilan balik untuk memperbarui keputusan routing
    @Override
    public void update(DTNHost thisHost) {
        // Tidak perlu melakukan tindakan khusus saat pembaruan routing
    }

    /**
     * Metode untuk menghitung nilai PeopleRank untuk host tertentu.
     *
     * @param host Host untuk dihitung nilai PeopleRank-nya
     * @return Nilai PeopleRank yang dihitung
     */
    private double calculatePer(DTNHost host) {
        double sum = 0.0;
        List<Set<DTNHost>> connectionHistory = connHistory.get(host);

        // Periksa jika tidak ada riwayat koneksi atau kosong
        if (connectionHistory == null || connectionHistory.isEmpty()) {
            return 0.0; // Kembalikan 0 jika tidak ada riwayat koneksi
        }

        int numNeighbors = 0;
        // Iterasi melalui riwayat koneksi untuk menghitung jumlah tetangga dan nilai PeopleRank tetangga
        for (Set<DTNHost> neighborSet : connectionHistory) {
            numNeighbors += neighborSet.size();
            for (DTNHost neighbor : neighborSet) {
                Tuple<Double, Integer> neighborInfo = per.get(neighbor);
                if (neighborInfo != null && neighborInfo.getValue() != 0) {
                    sum += neighborInfo.getKey() / neighborInfo.getValue(); // Perbarui jumlah dengan PeopleRank tetangga
                }
            }
        }

        // Hitung nilai PeopleRank untuk host berdasarkan rumus
        return (1 - dampingFactor) + dampingFactor * sum / numNeighbors;
    }

    /**
     * Metode untuk memperbarui nilai PeopleRank untuk host terhubung.
     *
     * @param myHost Host lokal di mana koneksi dilakukan
     * @param de Mesin keputusan PeopleRank dari host jarak jauh
     * @param contactSet Set host dengan mana host lokal melakukan koneksi
     */
    private void updatePeopleRank(DTNHost myHost, PeopleRank de, Set<DTNHost> contactSet) {
        for (DTNHost connectedHost : contactSet) {
            double updatedPer = de.calculatePer(connectedHost); // Hitung nilai PeopleRank yang diperbarui
            Tuple<Double, Integer> existingInfo = de.per.get(connectedHost);
            if (existingInfo != null) {
                existingInfo.setKey(updatedPer); // Perbarui nilai PeopleRank yang ada
            } else {
                de.per.put(connectedHost, new Tuple<>(updatedPer, 0)); // Tambahkan nilai PeopleRank baru jika tidak ada
            }
        }
    }

    /**
     * Metode untuk mendapatkan mesin keputusan PeopleRank dari host lain.
     *
     * @param h Host untuk mendapatkan mesin keputusan PeopleRank
     * @return Mesin keputusan PeopleRank dari host yang ditentukan
     */
    private PeopleRank getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works with other routers of the same type";
        return (PeopleRank) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    // Metode untuk menyalin mesin keputusan routing
    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRank(this); // Mengembalikan salinan objek PeopleRank
    }
}
