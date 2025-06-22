/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementasi dari router Spray and Wait yang dimodifikasi. Router ini akan
 * beralih ke metode PRoPHET untuk penerusan pesan ketika jumlah salinan pesan
 * adalah 1. Pauwerfull
 * 
 * 
 * Teknik forwarding masih kurang tepat
 */
public class ModifiedSprayAndWaitRouter extends ActiveRouter {

    /**
     * Identifier untuk pengaturan jumlah salinan awal ({@value})
     */
    public static final String NROF_COPIES = "nrofCopies";
    /**
     * Identifier untuk pengaturan mode biner ({@value})
     */
    public static final String BINARY_MODE = "binaryMode";
    /**
     * Namespace pengaturan untuk router Modified Spray and Wait ({@value})
     */
    public static final String MODIFIED_SPRAYANDWAIT_NS = "SprayAndWaitRouter";
    /**
     * Kunci properti pesan
     */
    public static final String MSG_COUNT_PROPERTY = MODIFIED_SPRAYANDWAIT_NS + "." + "copies";

    // Pengaturan spesifik PRoPHET
    public static final double P_INIT = 0.75;
    public static final double DEFAULT_BETA = 0.25;
    public static final double GAMMA = 0.98;
    public static final String PROPHET_NS = "ProphetRouter";
    public static final String SECONDS_IN_UNIT_S = "secondsInTimeUnit";
    public static final String BETA_S = "beta";

    protected int initialNrofCopies;
    protected boolean isBinary;
    private final int secondsInTimeUnit;
    private final double beta;
    private Map<DTNHost, Double> preds;
    private double lastAgeUpdate;

    /**
     * Konstruktor untuk ModifiedSprayAndWaitRouter
     *
     * @param s pengaturan yang digunakan
     */
    public ModifiedSprayAndWaitRouter(Settings s) {
        super(s);
        Settings msnwSettings = new Settings(MODIFIED_SPRAYANDWAIT_NS);
        initialNrofCopies = msnwSettings.getInt(NROF_COPIES);
        isBinary = msnwSettings.getBoolean(BINARY_MODE);

        // Inisialisasi pengaturan PRoPHET
        Settings prophetSettings = new Settings(PROPHET_NS);
        secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
        if (prophetSettings.contains(BETA_S)) {
            beta = prophetSettings.getDouble(BETA_S);
        } else {
            beta = DEFAULT_BETA;
        }
        initPreds();
    }

    /**
     * Konstruktor untuk menyalin router yang sudah ada
     *
     * @param r router yang akan disalin
     */
    protected ModifiedSprayAndWaitRouter(ModifiedSprayAndWaitRouter r) {
        super(r);
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
        this.secondsInTimeUnit = r.secondsInTimeUnit;
        this.beta = r.beta;
        initPreds();
    }

    /**
     * Inisialisasi peta prediksi
     */
    private void initPreds() {
        this.preds = new HashMap<>();
        this.lastAgeUpdate = 0;
    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        return super.receiveMessage(m, from);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message msg = super.messageTransferred(id, from);
        Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);

        assert nrofCopies != null : "Bukan pesan SnW: " + msg;

        if (isBinary) {
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        } else {
            nrofCopies = 1;
        }

        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return msg;
    }

    @Override
    public boolean createNewMessage(Message msg) {
        makeRoomForNewMessage(msg.getSize());

        msg.setTtl(this.msgTtl);
        msg.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        addToMessages(msg, true);
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring()) {
            return; // Tidak ada yang dapat ditransfer atau sedang mentransfer
        }

        // Coba kirim pesan yang bisa diantarkan ke penerima akhir
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        // Buat daftar pesan yang memiliki salinan tersisa untuk dibagikan
        List<Message> copiesLeft = getMessagesWithCopiesLeft();

        if (copiesLeft.size() > 0) {
            // Coba kirim pesan-pesan tersebut
            this.tryMessagesToConnections(copiesLeft, getConnections());
        }
    }

    /**
     * Mendapatkan daftar pesan yang masih memiliki salinan tersisa
     *
     * @return daftar pesan
     */
    protected List<Message> getMessagesWithCopiesLeft() {
        List<Message> list = new ArrayList<>();

        for (Message m : getMessageCollection()) {
            Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
            assert nrofCopies != null : "Pesan SnW " + m + " tidak memiliki properti jumlah salinan!";
            if (nrofCopies > 1) {
                list.add(m);
            } else {
                // Jika L = 1, beralih ke penerusan PRoPHET
                if (utilityBasedForwarding(m)) {
                    list.add(m);
                }
            }
        }

        return list;
    }

    /**
     * Implementasi penerusan berbasis utilitas menggunakan metode PRoPHET
     *
     * @param m pesan yang akan diteruskan
     * @return true jika ditemukan node yang lebih baik untuk meneruskan pesan
     */
    private boolean utilityBasedForwarding(Message m) {
        // Implementasi logika penerusan PRoPHET di sini
        updateDeliveryPreds();

        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            if (getPredFor(other) > getPredFor(m.getTo())) {
                return true; // Ditemukan node yang lebih baik untuk meneruskan pesan
            }
        }
        return false; // Tidak ditemukan node yang lebih baik
    }

    /**
     * Memperbarui prediksi pengantaran
     */
    private void updateDeliveryPreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    /**
     * Mendapatkan prediksi untuk host tertentu
     *
     * @param host host yang akan dicari prediksinya
     * @return nilai prediksi
     */
    private double getPredFor(DTNHost host) {
        updateDeliveryPreds();
        return preds.getOrDefault(host, 0.0);
    }

    @Override
    protected void transferDone(Connection con) {
        Integer nrofCopies;
        String msgId = con.getMessage().getId();
        Message msg = getMessage(msgId);

        if (msg == null) {
            return; // pesan telah dihapus dari buffer setelah transfer dimulai
        }

        nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies /= 2;
        } else {
            nrofCopies--;
        }
        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
    }

    @Override
    public ModifiedSprayAndWaitRouter replicate() {
        return new ModifiedSprayAndWaitRouter(this);
    }
}
