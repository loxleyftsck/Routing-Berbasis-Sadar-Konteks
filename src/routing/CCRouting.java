package routing;

import java.util.*;
import core.*;
import rl.*;

public class CCRouting extends ActiveRouter implements CongestionRate {
	private int msgReceived = 0;
	private int msgTransferred = 0;

	/** update interval diset dari settings */
	private final double updateInterval;

	// Variable untuk Congestion Ratio
	private int dataReceived = 0;
	private int dataTransferred = 0;
	private double lastUpdateTime = 0;
	private double totalContactTime = 0;
	private final Map<DTNHost, List<Double>> dataInContact;
	private final Map<DTNHost, List<Double>> congestionRatio;
	private final Map<DTNHost, List<Double>> ema;
	private static final double SMOOTHING_FACTOR = 0.5;

	// Variable untuk learning
	private QLearning ql;
	private IExplorationPolicy explorationPolicy;
	private final int totalState;
	private final int totalAction;
	/** 
	 * Integer sebagai address node, 
	 * jika status masih <CODE>pending</CODE> 
	 * atau <CODE>false</CODE> maka tidak dikirim
	 * */
	private Map<Integer, Boolean> waitForReward;

	/** namespace settings ({@value}) */
	private static final String CCRouting_NS = "CCRouting";
	/** nilai update interval atur di settings */
	private static final String UPDATE_INTERVAL = "updateInterval";
	/**
	 * Karena node dianggap sebagai state,
	 * maka state diinisalisasi sejumlah dengan total node
	 */
	private static final String TOTAL_STATE = "totalState";
	/** 
	 * Total action didapat dari 
	 * <CODE>(SimScenario.endTime / updateInterval)</CODE>
	 * */
	private static final String TOTAL_ACTION = "totalAction";

	// private boolean tesIsReceiving = false;
	/**
	 * Constructor
	 * 
	 * @param s
	 */
	public CCRouting(Settings s) {
		super(s);
		Settings ccSettings = new Settings(CCRouting_NS);
		updateInterval = ccSettings.getInt(UPDATE_INTERVAL);
		totalState = ccSettings.getInt(TOTAL_STATE);
		totalAction = ccSettings.getInt(TOTAL_ACTION);
		dataInContact = new HashMap<DTNHost, List<Double>>();
		congestionRatio = new HashMap<DTNHost, List<Double>>();
		ema = new HashMap<DTNHost, List<Double>>();
		initQL();
	}

	/**
	 * Copy constructor
	 * 
	 * @param r
	 */
	protected CCRouting(CCRouting r) {
		super(r);
		updateInterval = r.updateInterval;
		totalState = r.totalState;
		totalAction = r.totalAction;
		dataInContact = r.dataInContact;
		congestionRatio = r.congestionRatio;
		ema = r.ema;
		initQL();
	}

	protected void initQL() {
		this.explorationPolicy = new EpsilonGreedyExploration(0.989);

		this.ql = new QLearning(totalState, totalAction, this.explorationPolicy, false);
	}

	@Override
	public void changedConnection(Connection con) {
		// DTNHost peer = con.getOtherNode(getHost());
		if (con.isUp()) {
			getHost().setofHosts.add(con.getOtherNode(getHost()));
			// tesIsReceiving = false;
		} else {
			this.totalContactTime += SimClock.getTime();
		}
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		/**
		 * N.B. With application support the following if-block
		 * becomes obsolete, and the response size should be configured
		 * to zero.
		 */
		// check if msg was for this host and a response was requested
		if (m.getTo() == getHost() && m.getResponseSize() > 0) {
			// generate a response message
			Message res = new Message(this.getHost(), m.getFrom(),
					RESPONSE_PREFIX + m.getId(), m.getResponseSize());
			this.createNewMessage(res);
			this.getMessage(RESPONSE_PREFIX + m.getId()).setRequest(m);
		}

		getHost().msgReceived++; // increment jumlah message diterima
		this.msgReceived++;
		this.dataReceived += m.getSize();

		return m;
	}

	@Override
	protected void transferDone(Connection con) {
		getHost().msgTransferred++;
		this.msgTransferred++;
		this.dataTransferred += con.getMessage().getSize();
	}

	@Override
	public void update() {
		super.update();

		if ((SimClock.getTime() - lastUpdateTime) >= updateInterval) {
			countCongestionRatio(); // hitung CR

			// q learning
			//

			lastUpdateTime = SimClock.getTime();
		}

		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}

	@Override
	public CCRouting replicate() {
		return new CCRouting(this);
	}

	public int getTotalDataRcv() {
		return this.dataReceived;
	}

	public int getTotalDataTrf() {
		return this.dataTransferred;
	}

	public int getMsgReceived() {
		return this.msgReceived;
	}

	public int getMsgTransferred() {
		return this.msgTransferred;
	}

	public void countCongestionRatio() {
		double dataContact = 0;
		dataContact = (this.dataReceived + this.dataTransferred) / 
			(totalContactTime != 0 ? totalAction : 1 );

		List<Double> datas = this.dataInContact.get(getHost()) != null 
			? this.dataInContact.get(getHost())
			: new ArrayList<Double>();
		datas.add(dataContact);

		List<Double> dataForCr = this.congestionRatio.get(getHost()) != null 
			? this.congestionRatio.get(getHost())
			: new ArrayList<Double>();
		dataForCr.add(avgList(datas));
		

		this.dataInContact.put(getHost(), datas);
		this.congestionRatio.put(getHost(), dataForCr);

		int lastCr = this.congestionRatio.get(getHost()).size()-1;
		double oLast = this.congestionRatio.get(getHost()).get(lastCr);
		this.countEma(oLast);

		this.dataReceived = 0;
		this.dataTransferred = 0;

		// from DTNHost
		// testingCountEma();
	}

	public double avgList(List<Double> lists) {
		if (lists.size() == 0) {
			return 0;
		}

		double value = 0;

		for (double i : lists) {
			value += i;
		}

		return value / lists.size();
	}

	public void countEma(double oLast) {
		if(!this.ema.containsKey(getHost())) {
			this.ema.put(getHost(), new ArrayList<Double>());
		}
		int emaPrev2 = this.ema.get(getHost()).size() - 1; // last index dari ema

		double value2 = (this.ema.get(getHost()).size() > 0)
				? oLast * SMOOTHING_FACTOR + this.ema.get(getHost()).get(emaPrev2) * (1 - SMOOTHING_FACTOR)
				: oLast * SMOOTHING_FACTOR + 0 * (1 - SMOOTHING_FACTOR);

		this.ema.get(getHost()).add(value2);
	}

	public void testingDummyReward(double r) {
		getHost().dummyForReward.add(r);
	}

	public void testingCountEma() {
		double value = 0;
		for (double i : getHost().ema) {
			value += i;
		}

		testingDummyReward(1 / value);
	}

	@Override
	public List<Double> getCRNode(DTNHost host) {
		return this.congestionRatio.get(host);
	}

	@Override
	public List<Double> getDataInContactNode(DTNHost host) {
		return this.dataInContact.get(host);
	}

	@Override
	public List<Double> getEmaOfCR(DTNHost host) {
		return this.ema.get(host);
	}

	public QLearning getQl() {
		return this.ql;
	}
	
}
