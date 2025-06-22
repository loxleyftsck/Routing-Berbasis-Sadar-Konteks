/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.testContext.EnergyManager;

/**
 * Implementation of Spray and wait router as depicted in 
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class SprayAndWaitRouterBufferEnergy extends ActiveRouter {
	public static final String NROF_COPIES = "nrofCopies";
	public static final String BINARY_MODE = "binaryMode";
	public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
	public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + ".copies";

	protected int initialNrofCopies;
	protected boolean isBinary;

	public SprayAndWaitRouterBufferEnergy(Settings s) {
		super(s);
		Settings snwSettings = new Settings(SPRAYANDWAIT_NS);
		initialNrofCopies = snwSettings.getInt(NROF_COPIES);
		isBinary = snwSettings.getBoolean(BINARY_MODE);
	}

	protected SprayAndWaitRouterBufferEnergy(SprayAndWaitRouterBufferEnergy r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
		this.isBinary = r.isBinary;
	}

	@Override
	public int receiveMessage(Message m, DTNHost from) {
		// Cek apakah cukup energi untuk menerima pesan
		DTNHost receiver = getHost();
		if (receiver.getRouter() instanceof EnergyAwareRouter) {
			double remainingEnergy = EnergyManager.getRemainingEnergy((EnergyAwareRouter) receiver.getRouter());
			if (remainingEnergy < 2.0) { // Jika energi kurang dari threshold
				System.out.println("Node " + receiver.getAddress() + " tidak memiliki cukup energi untuk menerima pesan!");
				return DENIED_UNSPECIFIED; // Tolak pesan
			}

			// Konsumsi energi saat menerima pesan
			EnergyManager.consumeEnergy((EnergyAwareRouter) receiver.getRouter(), 1.0);
		}

		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
		assert nrofCopies != null : "Not a SnW message: " + msg;

		if (isBinary) {
			nrofCopies = Math.max(1, (int) Math.ceil(nrofCopies / 2.0)); // Pastikan minimal 1
		} else {
			nrofCopies = Math.max(1, nrofCopies - 1);
		}

		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		return msg;
	}

	@Override
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());
		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, Integer.valueOf(initialNrofCopies));
		addToMessages(msg, true);
		return true;
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return;
		}

		if (exchangeDeliverableMessages() != null) {
			return;
		}

		List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());

		if (copiesLeft.size() > 0) {
			this.tryMessagesToConnections(copiesLeft, getConnections());
		}
	}

	protected List<Message> getMessagesWithCopiesLeft() {
		List<Message> list = new ArrayList<>();

		for (Message m : getMessageCollection()) {
			Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
			assert nrofCopies != null : "SnW message " + m + " didn't have copies property!";
			if (nrofCopies > 1) {
				list.add(m);
			}
		}
		return list;
	}

	@Override
	protected void transferDone(Connection con) {
		String msgId = con.getMessage().getId();
		Message msg = getMessage(msgId);

		if (msg == null) {
			return;
		}

		Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
		if (isBinary) {
			nrofCopies = Math.max(1, nrofCopies / 2);
		} else {
			nrofCopies = Math.max(1, nrofCopies - 1);
		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

		// Reduce energy upon transfer
		DTNHost sender = getHost();
		if (sender.getRouter() instanceof EnergyAwareRouter) {
			double remainingEnergy = EnergyManager.getRemainingEnergy((EnergyAwareRouter) sender.getRouter());
			if (remainingEnergy < 2.0) {
				System.out.println("Node " + sender.getAddress() + " kehabisan energi! Transfer gagal.");
				return; // Transfer dibatalkan
			}
			EnergyManager.consumeEnergy((EnergyAwareRouter) sender.getRouter(), 2.0);
		}
	}

	@Override
	public SprayAndWaitRouterBufferEnergy replicate() {
		return new SprayAndWaitRouterBufferEnergy(this);
	}
}
