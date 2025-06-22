package routing.testContext;

import java.util.ArrayList;
import java.util.List;

import core.*;
import routing.EnergyAwareRouter;

public class EnergyAwareSprayAndWaitRouter extends EnergyAwareRouter {
    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + ".copies";

    public static final String BUFFER_SIZE = "bufferSize";
    public static final String INIT_ENERGY_S = "intialEnergy";
    public static final String SCAN_ENERGY_S = "scanEnergy";
    public static final String TRANSMIT_ENERGY_S = "transmitEnergy";
    public static final String WARMUP_S = "energyWarmup";
    public static final  String ALPHA ="alpha";
    public static final  String GAMMA ="gamma";

    protected int initialNrofCopies;
    protected boolean isBinary;
    protected double transmitEnergy;

    protected int bufferSize;
    protected int intialEnergy;

    public EnergyAwareSprayAndWaitRouter(Settings s) {
        super(s);
        intialEnergy = s.getInt(INIT_ENERGY_S);
        initialNrofCopies = s.getInt(NROF_COPIES);
        isBinary = s.getBoolean(BINARY_MODE);
        transmitEnergy = s.getDouble("transmitEnergy");
    }

    protected EnergyAwareSprayAndWaitRouter(EnergyAwareSprayAndWaitRouter r) {
        super(r);
        this.intialEnergy = r.intialEnergy;
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
        this.transmitEnergy = r.transmitEnergy;
    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        if (getCurrentEnergy() <= 0) return DENIED_UNSPECIFIED;
        if (m.getProperty(MSG_COUNT_PROPERTY) == null) {
            m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        }
        return super.receiveMessage(m, from);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        if (getCurrentEnergy() <= transmitEnergy) return null;

        Message msg = super.messageTransferred(id, from);
        if (msg == null) return null;

        Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
        if (nrofCopies == null) {
            nrofCopies = initialNrofCopies;  // Setel nilai default jika null
            msg.addProperty(MSG_COUNT_PROPERTY, nrofCopies);  // Menambahkan properti jika belum ada
        }
        nrofCopies = isBinary ? (int) Math.ceil(nrofCopies / 2.0) : 1;
        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        reduceEnergy(transmitEnergy);
        return msg;
    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring() || getCurrentEnergy() <= transmitEnergy) return;

        if (exchangeDeliverableMessages() != null) return;

        List<Message> copiesLeft = getMessagesWithCopiesLeft();
        if (!copiesLeft.isEmpty()) {
            this.tryMessagesToConnections(copiesLeft, getConnections());
        }
    }

    protected List<Message> getMessagesWithCopiesLeft() {
        List<Message> list = new ArrayList<>();
        for (Message m : getMessageCollection()) {
            Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY); // Mengambil properti MSG_COUNT_PROPERTY
            if (nrofCopies == null) {
                nrofCopies = initialNrofCopies;  // Menggunakan nilai default jika properti tidak ada
            }

            if (nrofCopies > 1) {
                list.add(m); // Jika jumlah salinan lebih dari 1, masukkan pesan ke dalam daftar
            }
        }
        return list;
    }

    @Override
    protected void transferDone(Connection con) {
        if (getCurrentEnergy() <= 0) return;

        Message msg = con.getMessage();
        Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);

        if (nrofCopies == null) {
            nrofCopies = initialNrofCopies;  // Menggunakan nilai default jika properti tidak ada
        }


        nrofCopies = Math.max(isBinary ? nrofCopies / 2 : nrofCopies - 1, 0);
        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        reduceEnergy(transmitEnergy);
    }

    @Override
    public EnergyAwareSprayAndWaitRouter replicate() {
        return new EnergyAwareSprayAndWaitRouter(this);
    }

}
//
//    public static final String NROF_COPIES = "nrofCopies";
//    public static final String BINARY_MODE = "binaryMode";
//    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
//    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + ".copies";
//    public static final String INIT_ENERGY_S = "initialEnergy";
//    public static final String TRANSMIT_ENERGY_S = "transmitEnergy";
//
//    protected int initialNrofCopies;
//    protected boolean isBinary;
//    private double currentEnergy;
//    private double transmitEnergy;
//
//    public EnergyAwareSprayAndWaitRouter(Settings s) {
//        super(s);
//        Settings snwSettings = new Settings(SPRAYANDWAIT_NS);
//
//        initialNrofCopies = s.getInt(NROF_COPIES);
//        isBinary = s.getBoolean(BINARY_MODE);
//        currentEnergy = s.getDouble(INIT_ENERGY_S);
//        transmitEnergy = s.getDouble(TRANSMIT_ENERGY_S);
//    }
//
//    protected EnergyAwareSprayAndWaitRouter(EnergyAwareSprayAndWaitRouter r) {
//        super(r);
//        this.initialNrofCopies = r.initialNrofCopies;
//        this.isBinary = r.isBinary;
//        this.currentEnergy = r.currentEnergy;
//        this.transmitEnergy = r.transmitEnergy;
//    }
//
//    @Override
//    public int receiveMessage(Message m, DTNHost from) {
//        if (currentEnergy <= 0) return DENIED_UNSPECIFIED;
//
////        // Tambahkan properti jika belum ada
////        if (!m.hasProperty(MSG_COUNT_PROPERTY)) {
////            m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
////        }
//
//        return super.receiveMessage(m, from);
//    }
//
//    @Override
//    public Message messageTransferred(String id, DTNHost from) {
//        if (currentEnergy <= 0) return null;
//
//        Message msg = super.messageTransferred(id, from);
//        Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
//        if (nrofCopies == null) {
//            nrofCopies = initialNrofCopies; // Set default jika null
//            msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
//        }
//        if (isBinary) {
//            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
//        } else {
//            nrofCopies = 1;
//        }
//
//        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
//        reduceEnergy(transmitEnergy);
//        return msg;
//    }
//
//    @Override
//    public void update() {
//        super.update();
//        if (!canStartTransfer() || isTransferring() || currentEnergy <= 0) {
//            return;
//        }
//
//        if (exchangeDeliverableMessages() != null) {
//            return;
//        }
//
//        // Membuat objek NeighborInformation untuk mengakses data tentang tetangga
//        NeighborInformation neighborInfo = new NeighborInformation(getHost());
//
//        // Mendapatkan energi tetangga
//        Map<Integer, Double> neighborEnergy = neighborInfo.getNeighborEnergy();
//
//
//        // Periksa energi tetangga dan buat keputusan berdasarkan itu
//        for (Map.Entry<Integer, Double> entry : neighborEnergy.entrySet()) {
//            Integer neighborId = entry.getKey();
//            Double energyLeft = entry.getValue();
//
//            // Misalnya, hanya kirim ke tetangga yang memiliki energi lebih dari 0
//            if (energyLeft > 0) {
//                // Lakukan pengiriman pesan ke tetangga yang sesuai
//                List<Message> copiesLeft = getMessagesWithCopiesLeft();
//                if (!copiesLeft.isEmpty()) {
//                    this.tryMessagesToConnections(copiesLeft, getConnections());
//                }
//            }
//        }
//    }
//
//    protected List<Message> getMessagesWithCopiesLeft() {
//        List<Message> list = new ArrayList<>();
//
//        for (Message m : getMessageCollection()) {
//            Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
//            if (nrofCopies != null && nrofCopies > 1) {
//                list.add(m);
//            }
//        }
//        return list;
//    }
//
//    @Override
//    protected void transferDone(Connection con) {
//        if (currentEnergy <= 0) return;
//
//        String msgId = con.getMessage().getId();
//        Message msg = getMessage(msgId);
//        if (msg == null) return;
//
//        Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
//
//        /// /
//        if (nrofCopies == null) {
//            nrofCopies = initialNrofCopies; // Set default jika null
//            msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
//        }
//
//        if (isBinary) {
//            nrofCopies /= 2;
//        } else {
//            nrofCopies--;
//        }
//        nrofCopies = Math.max(nrofCopies, 0);
//        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
//        reduceEnergy(transmitEnergy);
//    }
//
//    private void reduceEnergy(double amount) {
//        currentEnergy -= amount;
//        if (currentEnergy < 0) currentEnergy = 0;
//    }
//
//    @Override
//    public EnergyAwareSprayAndWaitRouter replicate() {
//        return new EnergyAwareSprayAndWaitRouter(this);
//    }
//}
