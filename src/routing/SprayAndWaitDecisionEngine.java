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

/**
 *
 * @author Pauwerfull
 */
public class SprayAndWaitDecisionEngine implements RoutingDecisionEngine {

    /**
     * identifier for the initial number of copies setting ({@value})
     */
    public static final String NROF_COPIES = "NrofCopies";
    /**
     * identifier for the binary-mode setting ({@value})
     */
    public static final String BINARY_MODE = "binaryMode";
    /**
     * SprayAndWait router's settings name space ({@value})
     */
    public static final String SPRAYANDFOCUS_NS = "SprayAndFocusRouter";
    /**
     * Message property key
     */
    public static final String MSG_COUNT_PROPERTY = SPRAYANDFOCUS_NS + "."
            + "copies";
    protected int initialNrofCopies;
    protected boolean isBinary;

    protected final static String BETA_SETTING = "beta";
    protected final static String P_INIT_SETTING = "initial_p";
    protected final static String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

    protected static final double DEFAULT_P_INIT = 0.75;
    protected static final double GAMMA = 0.92;
    protected static final double DEFAULT_BETA = 0.45;
    protected static final int DEFAULT_UNIT = 30;

    protected double beta;
    protected double pinit;
    protected double lastAgeUpdate;
    protected int secondsInTimeUnit;
    private final Map<DTNHost, Double> preds;
    private Set<Message> msgStamp;
    private Map<DTNHost, Integer> relayed;
    private DTNHost meHost;

    public SprayAndWaitDecisionEngine(Settings s) {

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

    public SprayAndWaitDecisionEngine(SprayAndWaitDecisionEngine spraywait) {
        this.initialNrofCopies = spraywait.initialNrofCopies;
        beta = spraywait.beta;
        pinit = spraywait.pinit;
        secondsInTimeUnit = spraywait.secondsInTimeUnit;
        meHost = spraywait.meHost;
        msgStamp = new HashSet<>();
        relayed = new HashMap<>();
        preds = new HashMap<DTNHost, Double>();
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
        SprayAndWaitDecisionEngine de = getOtherDecisionEngine(peer);
        Set<DTNHost> hostSet = new HashSet<DTNHost>(this.preds.size()
                + de.preds.size());
        hostSet.addAll(this.preds.keySet());
        hostSet.addAll(de.preds.keySet());

        this.agePreds();
        de.agePreds();

        // Update preds for this connection
        double myOldValue = this.getPredFor(peer),
                peerOldValue = de.getPredFor(myHost),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;
        preds.put(peer, myPforHost);
        de.preds.put(myHost, peerPforMe);

        // Update transistivities
        for (DTNHost h : hostSet) {
            myOldValue = 0.0;
            peerOldValue = 0.0;

            if (preds.containsKey(h)) {
                myOldValue = preds.get(h);
            }
            if (de.preds.containsKey(h)) {
                peerOldValue = de.preds.get(h);
            }

            if (h != myHost) {
                preds.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
            }
            if (h != peer) {
                de.preds.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
            }
        }

    }

    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate)
                / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    private double getPredFor(DTNHost host) {
        agePreds(); // make sure preds are updated before getting
        if (preds.containsKey(host)) {
            return preds.get(host);
        } else {
            return 0;
        }
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, Integer.valueOf(initialNrofCopies));
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        int NrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (NrofCopies > 1) {
            NrofCopies = (int) Math.ceil(NrofCopies);
        }
        if (NrofCopies == 1) {
            return true;
        }

        return true;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        msgStamp.add(m);
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }
        int NrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        SprayAndWaitDecisionEngine de = getOtherDecisionEngine(otherHost);
        if (NrofCopies == 1) {
            if (msgStamp.contains(m)) {
                relayed.put(meHost, !relayed.containsKey(meHost) ? 1 : relayed.get(meHost) + 1);
            }
            return de.getPredFor(m.getTo()) > this.getPredFor(m.getTo());
        }
        return NrofCopies > 1;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        int NrofCopies;

        NrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        if (NrofCopies > 1) {
            NrofCopies /= 2;
        }
        if (NrofCopies == 1) {
            return true;
        }

        m.updateProperty(MSG_COUNT_PROPERTY, NrofCopies);

        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public void update(DTNHost thisHost) {}

    @Override
    public RoutingDecisionEngine replicate() {
        return new SprayAndWaitDecisionEngine(this);
    }

    private SprayAndWaitDecisionEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (SprayAndWaitDecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

}
