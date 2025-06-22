/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

import java.util.*;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 * Kelas ini mengimplementasikan algoritma PeopleRank terdistribusi untuk
 * jaringan toleransi keterlambatan (DTNs). Algoritma ini menggunakan sejarah
 * koneksi untuk menghitung peringkat node (host) dan membuat keputusan routing
 * berdasarkan peringkat tersebut.
 *
 * Algoritma ini menggunakan faktor redaman (damping factor) dan ambang batas
 * untuk mempertimbangkan koneksi.
 *
 * @author Pauwerfull
 */
public class PRDistributed implements RoutingDecisionEngine, NilaiRankNode {

    /**
     * Variabel inisialisasi untuk faktor redaman - pengaturan id
     */
    public static final String DUMPING_FACTOR_SETTING = "dumpingFactor";
    public static final String TRESHOLD_SETTING = "threshold";

    /**
     * Peta untuk menyimpan nilai PeopleRank untuk setiap host bersama dengan
     * jumlah total teman.
     */
    protected Map<DTNHost, TupleDe<Double, Integer>> per;
    protected Map<DTNHost, List<Duration>> connHistory; // Menyimpan sejarah koneksi untuk setiap host
    protected Map<DTNHost, Double> startTimestamps; // Menyimpan timestamp awal untuk setiap koneksi
    protected Set<DTNHost> thisHostSet; // Set untuk menyimpan teman dari host ini

    // Deteksi komunitas dan faktor redaman
    protected double dumpingFactor; // Faktor redaman yang digunakan dalam algoritma PeopleRank
    protected double treshold; // Ambang batas untuk mempertimbangkan koneksi

    /**
     * Konstruktor untuk PRDistributed. Menginisialisasi faktor redaman dan
     * ambang batas, serta mengatur struktur data.
     *
     * @param s Objek Settings untuk konfigurasi
     */
    public PRDistributed(Settings s) {
        if (s.contains(DUMPING_FACTOR_SETTING)) {
            dumpingFactor = s.getDouble(DUMPING_FACTOR_SETTING);
        } else {
            this.dumpingFactor = 0.85;
        }
        if (s.contains(TRESHOLD_SETTING)) {
            treshold = s.getDouble(TRESHOLD_SETTING);
        } else {
            this.treshold = 700;
        }
        connHistory = new HashMap<DTNHost, List<Duration>>();
        per = new HashMap<>();
        thisHostSet = new HashSet<DTNHost>();
    }

    /**
     * Copy constructor untuk PRDistributed. Menggandakan pengaturan dan
     * struktur data dari instance yang ada.
     *
     * @param r Instance yang akan digandakan
     */
    public PRDistributed(PRDistributed r) {
        // Menggandakan faktor redaman
        this.dumpingFactor = r.dumpingFactor;
        this.treshold = r.treshold;
        startTimestamps = new HashMap<DTNHost, Double>();
        // Menginisialisasi peta sejarah koneksi baru
        this.connHistory = new HashMap<DTNHost, List<Duration>>();
        this.thisHostSet = new HashSet<DTNHost>();
        this.per = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // Implementasi untuk ketika koneksi naik (tidak digunakan dalam kode ini)
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Mendapatkan waktu mulai koneksi sebelumnya dan waktu saat ini
        double time = getPreviousConnectionStartTime(thisHost, peer);
        double etime = SimClock.getTime();

        // Memeriksa Total ConnHistory untuk menemukan atau membuat daftar sejarah koneksi
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        // Memeriksa apakah durasi koneksi lebih besar atau sama dengan ambang batas
        if (etime - time >= treshold) {
            history.add(new Duration(time, etime));
            thisHostSet.add(peer); // Menambahkan peer ke daftar teman dari host ini
        }

        // Memperbarui connHistory, Total FriendRank, dan totalFriend serta menyimpannya di per setiap kali koneksi turun
        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost host = entry.getKey();
            double friendRank = calculatePer(host);
            Set<DTNHost> Fj = new HashSet<>(connHistory.keySet());
            Fj.add(peer); // Menambahkan peer ke set host
            int totalFriends = Fj.size();
            TupleDe<Double, Integer> tuple = new TupleDe<>(friendRank, totalFriends);
            per.put(host, tuple);
        }
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // Mendapatkan host lokal dari koneksi
        DTNHost myHost = con.getOtherNode(peer);
        // Mendapatkan mesin keputusan PeopleRank dari host remote (peer)
        PRDistributed de = this.getOtherDecisionEngine(peer);

        // Memperbarui timestamp awal untuk kedua host
        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
    }

    @Override
    public boolean newMessage(Message m) {
        // Selalu menerima pesan baru
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        // Memeriksa apakah pesan berada di tujuan akhirnya
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        // Menyimpan pesan jika host ini bukan tujuan akhirnya
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // Memeriksa apakah tujuan pesan adalah host lain
        if (m.getTo() == otherHost) {
            return true; // Pesan harus dikirim langsung ke tujuan
        }

        // Menghitung PeopleRank untuk host ini dan host lain
        double perThisHost = calculatePer(thisHost);
        double perOtherHost = calculatePer(otherHost);

        // Menginisialisasi F(i) sebagai set teman dari i
        Set<DTNHost> Fi = new HashSet<>(connHistory.keySet());
        Fi.add(thisHost);

        // Memeriksa apakah host ini sedang berhubungan dengan host lain atau sudah menjadi teman
        if (connHistory.containsKey(otherHost) || thisHostSet.contains(otherHost)) {
            // while 1 do
            while (true) {
                // while i is in contact with j do
                for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
                    if (entry.getKey().equals(otherHost)) {
                        Iterator<DTNHost> iterator = Fi.iterator();
                        while (iterator.hasNext()) {
                            DTNHost check = iterator.next();
                            if (otherHost.equals(check)) { // if j ∈ F(i) then
                                return true;
                            } else if (!check.equals(otherHost)) { // if j !∈ F(i) then
                                return false;
                            }
                        }
                    }
                    // while ∃ m ∈ buffer(i) do
                    Buffer messageBuffer = new Buffer(); // Menginstansiasi Buffer dengan pengaturan
                    int bufferSize = messageBuffer.getBufferSize(thisHost);
                    while (bufferSize > 0) {
                        if (perOtherHost >= perThisHost || otherHost.equals(m.getTo())) {
                            return true; // Kondisi terpenuhi, Teruskan
                        }
                    }
                }

                // Jika host tujuan tidak berhubungan dengan host saat ini, periksa akhir while
            }
        }
        return false; // Jika tidak, jangan kirim pesan ke host lain
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        // Jangan menghapus pesan yang telah dikirim
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        // Selalu hapus pesan lama
        return true;
    }

    @Override
    public void update(DTNHost thisHost) {
        // Implementasi untuk memperbarui status host ini (tidak digunakan dalam kode ini)
    }

    @Override
    public RoutingDecisionEngine replicate() {
        // Membuat dan mengembalikan instance baru dari mesin keputusan ini
        return new PRDistributed(this);
    }

    /**
     * Mendapatkan waktu mulai koneksi sebelumnya antara host ini dan peer.
     *
     * @param thisHost Host saat ini
     * @param peer Peer host
     * @return Waktu mulai koneksi sebelumnya, atau 0 jika tidak ada catatan
     */
    public double getPreviousConnectionStartTime(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(thisHost)) {
            return startTimestamps.get(peer);
        } else {
            return 0;
        }
    }

    /**
     * Menghitung PeopleRank untuk host tertentu.
     *
     * @param host Host yang akan dihitung PeopleRank-nya
     * @return Nilai PeopleRank
     */
    private double calculatePer(DTNHost host) {
        double dampingFactor = this.dumpingFactor;
        double sum = 0.0; // Inisialisasi jumlah

        // Menghitung total jumlah teman untuk node lain
        int totalFriends = 0;
        for (TupleDe<Double, Integer> tuple : per.values()) {
            totalFriends += tuple.getSecond();
        }

        // Iterasi melalui sejarah koneksi
        for (TupleDe<Double, Integer> tuple : per.values()) {
            if (!tuple.getFirst().equals(host)) { // Mengecualikan host itu sendiri
                double friendRanking = tuple.getFirst(); // Peringkat teman
                int friendsOfOtherHost = tuple.getSecond(); // Jumlah total teman dari teman
                if (friendsOfOtherHost > 0) {
                    sum += friendRanking / totalFriends; // Akumulasi jumlah
                }
            }
        }

        // Menghitung dan mengembalikan nilai PeopleRank sesuai formula
        return (1 - dampingFactor) + dampingFactor * sum;
    }

    /**
     * Mendapatkan mesin keputusan PeopleRank dari host tertentu.
     *
     * @param h Host yang akan diambil mesin keputusannya
     * @return Mesin keputusan PeopleRank dari host tersebut
     */
    private PRDistributed getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "Router ini hanya bekerja dengan router lain dari jenis yang sama";

        return (PRDistributed) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    /**
     * Mendapatkan nilai PeopleRank untuk semua host.
     *
     * @return Peta host ke nilai PeopleRank mereka
     */
    public Map<DTNHost, Double> getAllRankings() {
        Map<DTNHost, Double> rankings = new HashMap<>();

        // Iterasi melalui peta per untuk mengekstrak peringkat
        for (Map.Entry<DTNHost, TupleDe<Double, Integer>> entry : per.entrySet()) {
            DTNHost currentHost = entry.getKey();
            TupleDe<Double, Integer> tuple = entry.getValue();

            rankings.put(currentHost, tuple.getFirst());
        }

        return rankings;
    }

    /**
     * Mendapatkan jumlah total teman untuk host tertentu.
     *
     * @param host Host yang akan diambil jumlah total temannya
     * @return Jumlah total teman
     */
    @Override
    public int getTotalTeman(DTNHost host) {
        DecisionEngineRouter d = (DecisionEngineRouter) host.getRouter();
        PeopleRank othRouter = (PeopleRank) d.getDecisionEngine();
        return othRouter.per.size();
    }
}
