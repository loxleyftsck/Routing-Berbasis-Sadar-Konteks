package routing.testContext;

import java.util.ArrayList;
import java.util.List;

import routing.ActiveRouter;

import routing.util.EnergyModel; // Import EnergyModel

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;


public class SprayAndWaitEnergyAware extends ActiveRouter {

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." +
            "copies";
    public static final String INIT_ENERGY_S = "initialEnergy";

    protected int initialNrofCopies;
    protected boolean isBinary;
    protected int initialEnergy;

    public SprayAndWaitEnergyAware(Settings s) {
        super(s);
        Settings testSPWAWARE = new Settings(SPRAYANDWAIT_NS);
//        initialEnergy =testSPWAWARE.getInt(INIT_ENERGY_S);
        initialNrofCopies = testSPWAWARE.getInt(NROF_COPIES);
        isBinary = testSPWAWARE.getBoolean(BINARY_MODE);
//        initialEnergy = s.getInt(INIT_ENERGY_S);
//        initialNrofCopies = s.getInt(NROF_COPIES);
//        isBinary = s.getBoolean(BINARY_MODE);
    }

    public SprayAndWaitEnergyAware(SprayAndWaitEnergyAware r) {
        super(r);
        this.initialEnergy = r.initialEnergy;
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
    }


    public int receiveMessage(Message m, DTNHost from) {
        EnergyModel energy = getEnergyModel(); //Mengambil dari Active
        // Cek apakah node masih memiliki energi sebelum menerima pesan
        if (!hasEnergy()) {
            return DENIED_LOW_RESOURCES; // Tolak pesan jika tidak ada energi
        }

        // Kurangi energi karena ada proses discovery
        energy.reduceDiscoveryEnergy();

        // Lanjutkan proses menerima pesan
        return super.receiveMessage(m, from);
    }

    public Message messageTransferred(String id, DTNHost from) {
        Message msg = super.messageTransferred(id, from);
        Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);

        assert nrofCopies != null : "Not a SnW message: " + msg;

        if (isBinary) {
            nrofCopies = (int)Math.ceil(nrofCopies / 2.0);
        } else {
            nrofCopies = 1;
        }

        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        EnergyModel energy = getEnergyModel();
        if (!hasEnergy()) {
            return null; // Pastikan ada energi cukup untuk transfer
        }
        // Cek apakah node masih memiliki energi sebelum menerima pesan
        // Reduce energy on data transfer
        double energyCost = calculateEnergyCost(msg);
        energy.reduceEnergy(energyCost);// Reduce energy based on data transfer
        return msg;
    }

    /**
     * Menghitung biaya energi berdasarkan ukuran pesan yang dikirim
     * @param msg Pesan yang ditransfer
     * @return Biaya energi yang dikonsumsi
     */
    private double calculateEnergyCost(Message msg) {
        double dataSize = msg.getSize(); // Ukuran pesan dalam byte
        double costPerByte = 0.01; // Biaya energi per byte, sesuaikan dengan model
        return dataSize * costPerByte;
    }


    public boolean createNewMessage(Message msg) {
        // Pastikan ada energi yang cukup untuk membuat pesan baru
        if (!hasEnergy()) {
            return false; // Energi tidak cukup untuk membuat pesan baru
        }

        // Pastikan ada ruang yang cukup untuk pesan baru
        makeRoomForNewMessage(msg.getSize());

        // Mengurangi energi untuk pembuatan pesan baru
        EnergyModel energy = getEnergyModel(); // Mendapatkan model energi
        double energyCost = calculateEnergyCost(msg); // Menghitung biaya energi berdasarkan ukuran pesan
        energy.reduceEnergy(energyCost); // Mengurangi energi sesuai dengan biaya yang dihitung


        // Set TTL pesan
        msg.setTtl(this.msgTtl);

        // Tambahkan properti jumlah salinan sesuai dengan setting awal
        msg.addProperty(MSG_COUNT_PROPERTY, Integer.valueOf(initialNrofCopies));

        // Menambahkan pesan ke dalam koleksi pesan router
        addToMessages(msg, true);
        return true;
    }

    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring()) {
            return; // nothing to transfer or is currently transferring
        }

        // Coba kirim pesan yang dapat diterima oleh penerima akhir
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        // Buat daftar pesan yang masih memiliki salinan untuk didistribusikan
        List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());

        if (!copiesLeft.isEmpty()) {
            // Coba kirim pesan ke koneksi
            tryMessagesToConnections(copiesLeft, getConnections());
        }
    }

    protected List<Message> getMessagesWithCopiesLeft() {
        List<Message> list = new ArrayList<>();

        for (Message m : getMessageCollection()) {
            Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
            assert nrofCopies != null : "SnW message " + m + " didn't have nrof copies property!";
            if (nrofCopies > 1) {
                list.add(m);
            }
        }

        return list;
    }

    protected void transferDone(Connection con) {
        Integer nrofCopies;
        String msgId = con.getMessage().getId();
        /* get this router's copy of the message */
        Message msg = getMessage(msgId);

        if (msg == null) { // message has been dropped from the buffer after..
            return; // ..start of transfer -> no need to reduce amount of copies
        }

        /* reduce the amount of copies left */
        nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies /= 2;
        }
        else {
            nrofCopies--;
        }
        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
    }

    public SprayAndWaitEnergyAware replicate() {
        return new SprayAndWaitEnergyAware(this);
    }
}
