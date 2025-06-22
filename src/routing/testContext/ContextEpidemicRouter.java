/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.testContext;

import core.Connection;
import core.DTNHost;
import core.Settings;
import routing.ActiveRouter;

import java.util.HashSet;
import java.util.Set;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class ContextEpidemicRouter extends ActiveRouter {

	public static final String BUFFER_SIZE = "bufferSize";

	// Menyimpan daftar tetangga
	private Set<String> neighbors;

	private final double bufferSize;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ContextEpidemicRouter(Settings s) {
		super(s);
		neighbors = new HashSet<>();
		//TODO: read&use epidemic router specific settings (if any)
		bufferSize = s.getDouble(BUFFER_SIZE);
		System.out.println("bufferSize: " + bufferSize);
    }

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ContextEpidemicRouter(ContextEpidemicRouter r) {
		super(r);
		this.neighbors = new HashSet<>(r.neighbors);
		//TODO: copy epidemic settings here (if any)
        this.bufferSize = r.bufferSize;
    }

	/**
	 * Memperbarui daftar tetangga berdasarkan koneksi aktif
	 */
	private void updateNeighbors() {
		Set<String> newNeighbors = new HashSet<>();

		// Ambil semua koneksi aktif dari node saat ini
		for (Connection conn : getHost().getConnections()) {
			if (conn.isUp()) {  // Pastikan koneksi aktif
				DTNHost otherHost = conn.getOtherNode(getHost());
				newNeighbors.add(String.valueOf(otherHost.getAddress()));
			}
		}

		// Cek apakah ada perubahan sebelum memperbarui daftar tetangga
		if (!newNeighbors.equals(neighbors)) {
//			System.out.println("Node ID: " + this.getHost().getAddress() + " updated neighbors.");
			neighbors = newNeighbors;
//			printNeighbors();
		}
	}

	/**
	 * Mencetak informasi tentang node dan daftar tetangganya
	 */
	private void printNeighbors() {
		System.out.println("Node ID: " + this.getHost().getAddress() + " has the following neighbors:");
		for (String neighbor : neighbors) {
			System.out.println("  Neighbor: " + neighbor);
		}
	}

	@Override
	public void update() {
		super.update();
		updateNeighbors();
		printNeighbors();
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
	public ContextEpidemicRouter replicate() {
		return new ContextEpidemicRouter(this);
	}

}