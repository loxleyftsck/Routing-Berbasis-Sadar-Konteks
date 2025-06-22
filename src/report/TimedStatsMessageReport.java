/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class TimedStatsMessageReport extends Report implements MessageListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times

	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;

	private final List<Double> milestonesTimes = List.of(5000.200, 6000.200, 8000.200, 10000.200, 12000.200, 16000.200,
			18000.200, 20000.200, 22000.200, 24000.200, 26000.200, 28000.200, 30000.200,
			32000.200, 34000.200, 36000.200, 38000.200, 40000.200, 43000.200,45000.200);
	//private final List<Double> milestonesTimes = List.of(5000.200, 10000.200, 30000.200, 40000.200, 43000.200, 45000.200, 50000.200, 55000.200,60000.200);
	//private final List<Double> milestonesTimes = List.of(5000.200, 10000.200, 30000.200, 43000.0, 45000.0);
//	private final List<Double> milestonesTimes = List.of(1000.0, 2000.0, 3000.0, 4000.0, 5000.0);
	private int nextMilestoneIndex=0;

	/**
	 * Constructor.
	 */
	public TimedStatsMessageReport() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.msgBufferTime = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.rtt = new ArrayList<Double>();
		
		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
	}

	private void checkMilestones(){
		double now = getSimTime();
		while (nextMilestoneIndex < milestonesTimes.size() && now >= milestonesTimes.get(nextMilestoneIndex)) {
			double t = milestonesTimes.get(nextMilestoneIndex);
			write("\n=== Milestone at " +(int) t+" ===\n");
			writeMilestoneStats();
			nextMilestoneIndex++;
		}
	}

	private void writeMilestoneStats(){
		double deliveryProb = 0; // delivery probability
		double responseProb = 0; // request-response success probability
		double overHead = Double.NaN;	// overhead ratio

		if (this.nrofCreated > 0) {
			deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
		}
		if (this.nrofDelivered > 0) {
			overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /
					this.nrofDelivered;
		}
		if (this.nrofResponseReqCreated > 0) {
			responseProb = (1.0* this.nrofResponseDelivered) /
					this.nrofResponseReqCreated;
		}

		write("created: " + nrofCreated);
		write("started: " + nrofStarted);
		write("relayed: " + nrofRelayed);
		write("aborted: " + nrofAborted);
		write("dropped: " + nrofDropped);
		write("removed: " + nrofRemoved);
		write("delivered: " + nrofDelivered);
		write("delivery_prob: " + format(deliveryProb));
		write("response_prob: " + format(responseProb));
		write("overhead_ratio: " + format(overHead));
		write("latency_avg: " + getAverage(latencies));
		write("latency_med: " + getMedian(latencies));
		write("hopcount_avg: " + getIntAverage(hopCounts));
		write("hopcount_med: " + getIntMedian(hopCounts));
		write("buffertime_avg: " + getAverage(msgBufferTime));
		write("buffertime_med: " + getMedian(msgBufferTime));
		write("rtt_avg: " + getAverage(rtt));
		write("rtt_med: " + getMedian(rtt));
	}
	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofDropped++;
		}
		else {
			this.nrofRemoved++;
		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
		checkMilestones();
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			checkMilestones();
			return;
		}
		
		this.nrofAborted++;
		checkMilestones();
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
		          boolean finalTarget) {
        if (isWarmupID(m.getId())) {
			checkMilestones();
            return;
        }

        this.nrofRelayed++;
        if (finalTarget) {
            this.latencies.add(getSimTime() - this.creationTimes.get(m.getId()));
            this.nrofDelivered++;
            this.hopCounts.add(m.getHops().size() - 1);

            if (m.isResponse()) {
                this.rtt.add(getSimTime() - m.getRequest().getCreationTime());
                this.nrofResponseDelivered++;
            }
        }
		checkMilestones();
	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
		checkMilestones();
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			checkMilestones();
			return;
		}

		this.nrofStarted++;
		checkMilestones();
	}
	

	@Override
	public void done() {
		write("\n=== Final Statistics at " + format(getSimTime()) + "s ===");
		writeMilestoneStats();
		super.done();
	}
	
}
