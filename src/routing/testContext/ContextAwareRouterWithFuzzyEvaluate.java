package routing.testContext;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.EnergyAwareRouter;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.jFuzzyLogic.FIS;

public class ContextAwareRouterWithFuzzyEvaluate extends EnergyAwareRouter {
    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + ".copies";

    public static final String INIT_ENERGY_S = "intialEnergy";
    public static final String SCAN_ENERGY_S = "scanEnergy";
    public static final String TRANSMIT_ENERGY_S = "transmitEnergy";
    public static final String WARMUP_S = "energyWarmup";

    public static final String FCL_ContextNode = "fclcontextaware";

    private FIS fclcontextaware;

    protected int initialNrofCopies;
    protected boolean isBinary;
    protected double transmitEnergy;

    protected int intialEnergy;

    public ContextAwareRouterWithFuzzyEvaluate(Settings s) {
        super(s);
        intialEnergy = s.getInt(INIT_ENERGY_S);
        initialNrofCopies = s.getInt(NROF_COPIES);
        isBinary = s.getBoolean(BINARY_MODE);
        transmitEnergy = s.getDouble("transmitEnergy");

        String fclstring=s.getSetting(FCL_ContextNode);
        fclcontextaware = FIS.load(fclstring, true);

        if (fclcontextaware == null) {
            System.err.println("Error loading FCL file");
        } else {
            System.out.println("FCL loaded successfully.");
        }
    }

    protected ContextAwareRouterWithFuzzyEvaluate(ContextAwareRouterWithFuzzyEvaluate r) {
        super(r);
        this.intialEnergy = r.intialEnergy;
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
        this.transmitEnergy = r.transmitEnergy;

        this.fclcontextaware  = r.fclcontextaware;
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
    public ContextAwareRouterWithFuzzyEvaluate replicate() {
        return new ContextAwareRouterWithFuzzyEvaluate(this);
    }

}
