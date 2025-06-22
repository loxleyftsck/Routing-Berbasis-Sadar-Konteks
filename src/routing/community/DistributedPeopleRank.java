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
 * Implementation of Distributed PeopleRank algorithm for routing in community
 * networks. Algorithm 1 Distributed PeopleRank (i) Require: |F(i)| ≥ 0 PeR(i) ←
 * 0 while 1 do while i is in contact with j do if j ∈ F(i) then send(PeR(i),
 * |F(i)|) receive(PeR(j), |F(j)|) update(PeR(i)) (Eq. 2) end if while ∃ m ∈
 * buffer(i) do if PeR(j) ≥ PeR(i) OR j = destination(m) then Forward(m, j) end
 * if end while end while end while
 *
 * @author Pauwerfull
 */
public class DistributedPeopleRank implements RoutingDecisionEngine {

    // Konstanta untuk pengaturan faktor damping
    public static final String DUMPING_FACTOR_SETTING = "dumpingFactor";

    // Struktur data untuk menyimpan riwayat koneksi dan nilai ranking
    Map<DTNHost, List<Set<DTNHost>>> connHistory;
    Map<DTNHost, Tuple<Double, Integer>> per = new HashMap<>();

    // Faktor damping yang digunakan dalam algoritma PeopleRank
    protected double dampingFactor = 0.75;

    //Treeshold
    // Konstruktor untuk inisialisasi objek PeopleRankDistributed
    public DistributedPeopleRank(Settings s) {
        // Mengatur faktor damping dari pengaturan jika tersedia, jika tidak, 
        //menggunakan nilai default
        if (s.contains(DUMPING_FACTOR_SETTING)) {
            dampingFactor = s.getDouble(DUMPING_FACTOR_SETTING);
        }

        // Inisialisasi map untuk menyimpan riwayat koneksi
        connHistory = new HashMap<>();
    }

    // Metode untuk menyalin objek routing keputusan
    public DistributedPeopleRank(DistributedPeopleRank r) {
        this.dampingFactor = r.dampingFactor;
        this.connHistory = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Menambahkan informasi pemutusan koneksi ke riwayat koneksi
        addConnectionToHistory(thisHost, peer);
        addConnectionToHistory(peer, thisHost);
    }

    // Metode untuk pertukaran informasi saat koneksi baru terjadi
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // Mendapatkan host lain dalam koneksi
        DTNHost myHost = con.getOtherNode(peer);
        // Mendapatkan objek keputusan pengguna lain dari host yang baru terhubung
        DistributedPeopleRank de = this.getOtherDecisionEngine(peer);

        // Memeriksa riwayat koneksi untuk host saat ini
        if (connHistory.containsKey(myHost)) {
            List<Set<DTNHost>> connectionHistory = connHistory.get(myHost);

            // Memeriksa setiap riwayat koneksi
            for (Set<DTNHost> contactSet : connectionHistory) {
                // Jika host baru terhubung terdapat dalam riwayat koneksi
                if (contactSet.contains(peer)) {
                    // Memperbarui nilai PeopleRank host saat ini berdasarkan informasi dari host lain
                    updatePeopleRank(myHost, de, contactSet);
                    break;
                }
            }
        }
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    // Metode untuk menentukan apakah host saat ini adalah tujuan akhir pesan
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    // Metode untuk menentukan apakah pesan yang diterima harus disimpan oleh host saat ini
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    // Metode untuk menentukan apakah pesan harus dikirimkan ke host tertentu
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        double perThisHost = calculatePer(thisHost);
        double perOtherHost = calculatePer(otherHost);

        // Menentukan threshold untuk menentukan apakah host dianggap sebagai teman
        double threshold = 0.5; // Misalnya, gunakan nilai threshold 0.5

        // Memeriksa apakah tujuan pesan adalah host lain
        if (m.getTo() == otherHost) {
            return true; // Pesan harus dikirim langsung ke tujuan
        }

        // Memeriksa apakah host ini dalam kontak dengan host lainnya
        if (connHistory.containsKey(otherHost)) {
            Iterator<Set<DTNHost>> iterator = connHistory.get(otherHost).iterator();
            while (iterator.hasNext()) {
                Set<DTNHost> contactSet = iterator.next();
                if (contactSet.contains(thisHost)) {
                    // Memeriksa apakah nilai PeopleRank host lainnya lebih besar atau sama dengan host ini
                    // Jangan kirim pesan ke host lainnya
                    return perOtherHost >= perThisHost && perOtherHost >= threshold; // Kirim pesan ke host lainnya
                }
            }
        }

        // Jika tujuan host tidak dalam kontak dengan host saat ini, periksa Buffer
        Buffer messageBuffer = new Buffer();
        int bufferSize = messageBuffer.getBufferSize(thisHost);
        Iterator<Message> bufferIterator = messageBuffer.Iterator();
        while (bufferSize > 0 && bufferIterator.hasNext()) {
            Message bufferedMessage = bufferIterator.next();
            if ((perOtherHost >= perThisHost && perOtherHost >= threshold) || otherHost.equals(bufferedMessage.getTo())) {
                return true; // Kondisi terpenuhi, kirim pesan
            }
        }
        return false;
    }

    // Metode untuk menentukan apakah pesan yang dikirim harus dihapus
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    // Metode untuk menentukan apakah pesan lama harus dihapus
    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    // Metode untuk menangani pembaruan dalam jaringan
    @Override
    public void update(DTNHost thisHost) {
    }

    // Metode untuk menghitung nilai PeopleRank untuk host tertentu
    private double calculatePer(DTNHost host) {
        double sum = 0.0;

        List<Set<DTNHost>> connectionHistory = connHistory.get(host);

        if (connectionHistory == null || connectionHistory.isEmpty()) {
            return 0.0; // Kembalikan 0 jika tidak ada riwayat koneksi
        }

        for (Set<DTNHost> friendList : connectionHistory) {
            for (DTNHost friend : friendList) {
                Tuple<Double, Integer> friendInfo = per.get(friend);
                if (friendInfo != null && friendInfo.getValue() != 0) {
                    sum += friendInfo.getKey() / friendInfo.getValue();
                }
            }
        }

        return (1 - dampingFactor) + dampingFactor * sum;
    }

    // Metode untuk mendapatkan objek routing keputusan dari host lain
    private DistributedPeopleRank getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (DistributedPeopleRank) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    // Metode untuk menambahkan informasi pemutusan koneksi ke riwayat koneksi
    private void addConnectionToHistory(DTNHost host, DTNHost peer) {
        List<Set<DTNHost>> hostHistory = connHistory.getOrDefault(host, new LinkedList<>());
        Set<DTNHost> disconnectedSet = new HashSet<>();
        disconnectedSet.add(peer);
        hostHistory.add(disconnectedSet);
        connHistory.put(host, hostHistory);
    }

    // Metode untuk memperbarui nilai PeopleRank berdasarkan informasi yang diterima
    private void updatePeopleRank(DTNHost myHost, DistributedPeopleRank de, Set<DTNHost> contactSet) {
        for (DTNHost connectedHost : contactSet) {
            double updatedPer = de.calculatePer(connectedHost);
            Tuple<Double, Integer> existingInfo = de.per.get(connectedHost);
            if (existingInfo != null) {
                existingInfo.setKey(updatedPer);
            } else {
                de.per.put(connectedHost, new Tuple<>(updatedPer, 0));
            }
        }
    }

    // Metode untuk menyalin objek routing keputusan
    @Override
    public RoutingDecisionEngine replicate() {
        return new DistributedPeopleRank(this);
    }

}
