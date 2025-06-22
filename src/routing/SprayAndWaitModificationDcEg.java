/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static routing.ProphetDecisionEngine.BETA_SETTING;
import static routing.ProphetDecisionEngine.P_INIT_SETTING;
import static routing.ProphetRouter.SECONDS_IN_UNIT_S;

/**
 *
 * Implementasi algoritma Spray and Wait dengan modifikasi untuk menggunakan
 * forwarding berbasis utilitas (PROPHET)ketika jumlah salinan pesan (L) adalah
 * 1.
 *
 * @author Pauwerfull
 */

public class SprayAndWaitModificationDcEg implements RoutingDecisionEngine {

    // Identifikasi untuk jumlah salinan awal
    public static final String NROF_COPIES = "NrofCopies";
    // Identifikasi untuk mode biner (tidak digunakan dalam implementasi ini)
    public static final String BINARY_MODE = "binaryMode";
    // Namespace untuk pengaturan SprayAndFocusRouter
    public static final String SPRAYANDWAIT_NS = "SprayAndFocusRouter";
    // Properti pesan untuk menyimpan jumlah salinan
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + ".copies";

    // Pengaturan default untuk algoritma PROPHET
    protected static final double DEFAULT_P_INIT = 0.75;
    protected static final double GAMMA = 0.98;
    protected static final double DEFAULT_BETA = 0.25;
    protected static final int DEFAULT_UNIT = 30;

    // Variabel untuk menyimpan pengaturan
    protected int initialNrofCopies;
    protected boolean isBinary;

    protected double beta;
    protected double pinit;
    protected double lastAgeUpdate;
    protected int secondsInTimeUnit;

    // Struktur data untuk menyimpan prediksi pertemuan, cap pesan, dan jumlah relay
    private final Map<DTNHost, Double> preds;
    private Set<Message> msgStamp;
    private Map<DTNHost, Integer> relayed;
    private DTNHost meHost;

    /**
     * Konstruktor untuk menginisialisasi dengan pengaturan.
     *
     * @param s Pengaturan yang digunakan untuk mengkonfigurasi engine.
     */
    public SprayAndWaitModificationDcEg(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES);
        if (s.contains(BETA_SETTING)) {
            beta = s.getDouble(BETA_SETTING);
        } else {
            beta = DEFAULT_BETA;
        }

        if (s.contains(P_INIT_SETTING)) {
            pinit = s.getDouble(P_INIT_SETTING);
        } else {
            pinit = DEFAULT_P_INIT;
        }

        if (s.contains(SECONDS_IN_UNIT_S)) {
            secondsInTimeUnit = s.getInt(SECONDS_IN_UNIT_S);
        } else {
            secondsInTimeUnit = DEFAULT_UNIT;
        }
        preds = new HashMap<>();
        this.lastAgeUpdate = 0.0;
    }

    /**
     * Konstruktor untuk menyalin instance lain.
     *
     * @param spraywait Instance lain dari SprayAndWaitDecisionEngine.
     */
    public SprayAndWaitModificationDcEg(SprayAndWaitModificationDcEg spraywait) {
        this.initialNrofCopies = spraywait.initialNrofCopies;
        this.beta = spraywait.beta;
        this.pinit = spraywait.pinit;
        this.secondsInTimeUnit = spraywait.secondsInTimeUnit;
        this.meHost = spraywait.meHost;
        this.msgStamp = new HashSet<>();
        this.relayed = new HashMap<>();
        this.preds = new HashMap<>(spraywait.preds);
        this.lastAgeUpdate = spraywait.lastAgeUpdate;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        SprayAndWaitModificationDcEg de = getOtherDecisionEngine(peer);
        Set<DTNHost> hostSet = new HashSet<>(this.preds.size() + de.preds.size());
        hostSet.addAll(this.preds.keySet());
        hostSet.addAll(de.preds.keySet());

        // Memperbarui prediksi sebelum melakukan pertukaran
        this.agePreds();
        de.agePreds();

        // Menghitung nilai prediksi baru untuk node saat ini dan peer
        double myOldValue = this.getPredFor(peer);
        double peerOldValue = de.getPredFor(myHost);
        double myPforHost = myOldValue + (1 - myOldValue) * pinit;
        double peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;

        preds.put(peer, myPforHost);
        de.preds.put(myHost, peerPforMe);

        // Memperbarui nilai prediksi untuk semua host yang diketahui
        for (DTNHost h : hostSet) {
            myOldValue = preds.getOrDefault(h, 0.0);
            peerOldValue = de.preds.getOrDefault(h, 0.0);

            if (h != myHost) {
                preds.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
            }
            if (h != peer) {
                de.preds.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
            }
        }
    }

    /**
     * Metode untuk memperbarui nilai prediksi berdasarkan waktu.
     */
    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / secondsInTimeUnit;
        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        preds.replaceAll((host, value) -> value * mult);
        this.lastAgeUpdate = SimClock.getTime();
    }

    /**
     * Mendapatkan nilai prediksi untuk host tertentu.
     *
     * @param host Host yang ingin diketahui prediksinya.
     * @return Nilai prediksi untuk host tersebut.
     */
    private double getPredFor(DTNHost host) {
        agePreds();
        return preds.getOrDefault(host, 0.0);
    }

    @Override
    public boolean newMessage(Message m) {
        // Menambahkan properti jumlah salinan ke pesan baru
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        // Memeriksa apakah host ini adalah tujuan akhir pesan
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        // Menyimpan pesan yang diterima jika bukan tujuan akhirnya
        msgStamp.add(m);
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        int NrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (NrofCopies > 1) {
            // Binary Spray: kirim pesan jika jumlah salinan lebih dari 1
            return true;
        } else if (NrofCopies == 1) {
            // Utility-based Forwarding (PROPHET): gunakan prediksi untuk memutuskan
            SprayAndWaitModificationDcEg de = getOtherDecisionEngine(otherHost);
            return de.getPredFor(m.getTo()) > this.getPredFor(m.getTo());
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        int NrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (NrofCopies > 1) {
            // Kurangi jumlah salinan dengan membagi dua
            NrofCopies /= 2;
        } else if (NrofCopies == 1) {
            // Hapus pesan jika jumlah salinan adalah 1
            return true;
        }
        m.updateProperty(MSG_COUNT_PROPERTY, NrofCopies);
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        // Hapus pesan lama jika host ini adalah tujuan akhirnya
        return m.getTo() == hostReportingOld;
    }
    
//    private List<DTNHost> getNeighbors(DTNHost host) {
//    List<DTNHost> neighbors = new ArrayList<>();
//    for (Connection conn : host.getConnections()) {
//        neighbors.add(conn.getOtherNode(host));
//    }
//    return neighbors;
//}


    @Override
    public void update(DTNHost thisHost) {
        
//        List<DTNHost> neighbors = getNeighbors(thisHost);
//        bufferCalculator.neighborBuffers(neighbors);
    }

    @Override
    public RoutingDecisionEngine replicate() {
        // Mengembalikan instance baru dari objek ini
        return new SprayAndWaitModificationDcEg(this);
    }

    /**
     * Mendapatkan instance dari decision engine untuk host lain.
     *
     * @param h Host yang ingin diketahui decision enginenya.
     * @return Instance dari SprayAndWaitDecisionEngine untuk host tersebut.
     */
    private SprayAndWaitModificationDcEg getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "Router ini hanya bekerja dengan router tipe yang sama";
        return (SprayAndWaitModificationDcEg) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

}
