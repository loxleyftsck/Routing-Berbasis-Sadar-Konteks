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
import core.SimClock;
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
public class PeopleRankDistributed implements RoutingDecisionEngine {

    // Konstanta untuk pengaturan faktor damping
    public static final String DAMPING_FACTOR_SETTING = "dampingFactor";
    // Konstanta untuk pengaturan threshold
    public static final String CONNECTION_DURATION_THRESHOLD_SETTING = "connectionDurationThreshold";

    // Struktur data untuk menyimpan riwayat koneksi dan nilai PeopleRank
    protected Map<DTNHost, List<Duration>> connHistory;
    public final Map<DTNHost, Tuple<Double, Integer>> per;

    // Faktor damping yang digunakan dalam algoritma PeopleRank
    private double dampingFactor = 0.75;
    // Threshold untuk menentukan apakah dua host dianggap sebagai teman berdasarkan durasi waktu koneksi
    private final double connectionDurationThreshold = 3600; // Misalnya, gunakan nilai threshold 3600 detik (1 jam)

    /**
     * Konstruktor untuk menginisialisasi objek PeopleRankDistributed.
     *
     * @param s Objek Settings yang berisi parameter simulasi
     */
    public PeopleRankDistributed(Settings s) {
        if (s.contains(DAMPING_FACTOR_SETTING)) {
            dampingFactor = s.getDouble(DAMPING_FACTOR_SETTING);
        } else {
            System.err.println("Connection duration threshold setting not found. Using default value.");
        }
        connHistory = new HashMap<>();
        per = new HashMap<>();
    }

    /**
     * Konstruktor salinan untuk mereplikasi objek PeopleRankDistributed.
     *
     * @param r Objek PeopleRankDistributed yang akan direplikasi
     */
    public PeopleRankDistributed(PeopleRankDistributed r) {
        // Salin faktor damping
        this.dampingFactor = r.dampingFactor;
        // Salin riwayat koneksi
        this.connHistory = new HashMap<>(r.connHistory);
        // Salin informasi PeopleRank
        this.per = new HashMap<>(r.per);
    }

    // Metode panggilan balik saat koneksi terhubung
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    // Metode panggilan balik saat koneksi terputus
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Menentukan durasi waktu koneksi antara thisHost dan peer
        double connectionDuration = calculateConnectionDuration(thisHost, peer);

        // Membuat objek Duration untuk menyimpan durasi waktu koneksi
        Duration duration = new Duration(connectionDuration); // Anggap start = 0 (mulai koneksi)

        // Tambahkan informasi pemutusan koneksi ke riwayat koneksi
        addConnectionToHistory(thisHost, peer, duration);
        addConnectionToHistory(peer, thisHost, duration);
    }

    // Metode panggilan balik untuk pertukaran informasi saat koneksi baru terjadi
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankDistributed de = getOtherDecisionEngine(peer);

        // Memeriksa apakah myHost pernah bertemu dengan peer sebelumnya
        if (connHistory.containsKey(myHost)) {
            List<Duration> connectionHistory = connHistory.get(myHost);
            for (Duration duration : connectionHistory) {
                // Jika myHost bertemu dengan peer di masa lalu
                if (duration.start <= SimClock.getTime() && duration.end >= SimClock.getTime()) {
                    // Memperbarui nilai PeopleRank
                    updatePeopleRank(myHost, de, duration);
                    break;
                }
            }
        }
    }

    /**
     *
     * @param host
     * @param peer
     * @param duration
     */
    public void addConnectionToHistory(DTNHost host, DTNHost peer, Duration duration) {
        List<Duration> hostHistory = connHistory.getOrDefault(host, new LinkedList<>());
        hostHistory.add((Duration) duration);
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

    // Metode untuk menentukan apakah pesan harus dikirimkan ke host tertentu
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        double perThisHost = calculatePer(thisHost);
        double perOtherHost = calculatePer(otherHost);

        // Jika tujuan pesan adalah host lain, kirim langsung ke tujuan
        if (m.getTo() == otherHost) {
            return true;
        }

        // Memeriksa apakah host ini pernah terhubung dengan host lain
        if (connHistory.containsKey(otherHost)) {
            List<Duration> connectionHistory = connHistory.get(otherHost);
            for (Duration duration : connectionHistory) {
                // Mengonversi Duration menjadi Set<DTNHost>
                Set<DTNHost> hosts = duration.getHosts();
                // Memeriksa apakah thisHost ada dalam setiap koneksi pada durasi waktu
                if (hosts.contains(thisHost)) {
                    // Jika durasi waktu koneksi masih berlangsung
                    if (duration.start <= SimClock.getTime() && duration.end >= SimClock.getTime()) {
                        // Jika nilai PeopleRank host lain lebih besar atau sama dengan host ini, kirim pesan
                        return perOtherHost >= perThisHost;
                    }
                }
            }
        }

        // Jika tujuan host tidak dalam kontak dengan host saat ini, periksa Buffer
        Buffer messageBuffer = new Buffer();
        int bufferSize = messageBuffer.getBufferSize(thisHost);
        Iterator<Message> bufferIterator = messageBuffer.Iterator();
        while (bufferSize > 0 && bufferIterator.hasNext()) {
            Message bufferedMessage = bufferIterator.next();
            if (otherHost.equals(bufferedMessage.getTo())) {
                return true; // Kondisi terpenuhi, kirim pesan
            }
        }
        return false;
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
        List<Duration> connectionHistory = connHistory.get(host);
        if (connectionHistory == null || connectionHistory.isEmpty()) {
            return 0.0;
        }

        int numNeighbors = 0;
        for (Duration duration : connectionHistory) {
            if (duration.end >= connectionDurationThreshold) {
                numNeighbors++;
                Tuple<Double, Integer> neighborInfo = per.get(duration.getHosts());
                if (neighborInfo != null && neighborInfo.getValue() != 0) {
                    sum += neighborInfo.getKey() / neighborInfo.getValue();
                }
            }
        }

        return (1 - dampingFactor) + dampingFactor * sum / numNeighbors;
    }

    /**
     * Metode untuk memperbarui nilai PeopleRank untuk host terhubung.
     *
     * @param myHost Host lokal di mana koneksi dilakukan
     * @param de Mesin keputusan PeopleRank dari host jarak jauh
     * @param contactSet Set host dengan mana host lokal melakukan koneksi
     */
    private void updatePeopleRank(DTNHost myHost, PeopleRankDistributed de, Duration duration) {
        Set<DTNHost> connectedHosts = duration.getHosts();
        for (DTNHost connectedHost : connectedHosts) {
            double updatedPer = de.calculatePer(connectedHost);
            Tuple<Double, Integer> existingInfo = de.per.get(connectedHost);
            if (existingInfo != null) {
                existingInfo.setKey(updatedPer);
            } else {
                de.per.put(connectedHost, new Tuple<>(updatedPer, 0));
            }
        }
    }

    private double calculateConnectionDuration(DTNHost host1, DTNHost host2) {
        // Implementasi untuk menghitung durasi waktu koneksi antara host1 dan host2
        // Anda dapat menggunakan logika seperti menghitung selisih waktu saat koneksi terhubung dan terputus
        // dan mengembalikan hasil dalam satuan waktu yang diinginkan (misalnya, detik).
        // Contoh:
        // return durationInSeconds;
        return 0.0; // Pengembalian sementara untuk menghindari kesalahan kompilasi
    }

    /**
     * Metode untuk mendapatkan mesin keputusan PeopleRank dari host lain.
     *
     * @param h Host untuk mendapatkan mesin keputusan PeopleRank
     * @return Mesin keputusan PeopleRank dari host yang ditentukan
     */
    private PeopleRankDistributed getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works with other routers of the same type";
        return (PeopleRankDistributed) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    // Metode untuk menyalin mesin keputusan routing
    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankDistributed(this); // Mengembalikan salinan objek PeopleRankDistributed
    }
}
