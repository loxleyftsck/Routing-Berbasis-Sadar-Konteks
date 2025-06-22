/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.testContext;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.ActiveRouter;
import routing.util.EnergyModel;

import net.sourceforge.jFuzzyLogic.FIS;


import java.util.*;

/**
 * Implementation of Spray and wait router as depicted in 
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class ContextSprayAndWaitRouter extends ActiveRouter {
	/** identifier for the initial number of copies setting ({@value})*/
	public static final String NROF_COPIES = "nrofCopies";
	/** identifier for the binary-mode setting ({@value})*/
	public static final String BINARY_MODE = "binaryMode";
	/** SprayAndWait router's settings name space ({@value})*/
	public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." +
		"copies";

	public static final String BUFFER_SIZE = "bufferSize";
	public static final String INIT_ENERGY_S = "initialEnergy";

	public static final String FCL_Context = "fcl";
	public static final String ALPHA_POPULARITY = "alphaPopularity";


	protected int bufferSize;
	protected int initialNrofCopies;
	protected boolean isBinary;
	protected int initialEnergy;
	protected double alphaPopularity;

	protected FIS fclcontextaware;
//	private Popularity popularity;
//	private TieStrength tieStrength;

	private DTNHost selectedRelayNode;

	public ContextSprayAndWaitRouter(Settings s) {
		super(s);
//		Settings testSPWAWARE = new Settings(SPRAYANDWAIT_NS);
		//Popularity
		alphaPopularity = s.getDouble(ALPHA_POPULARITY);
//		this.popularity = new Popularity(alphaPopularity);
		//TieStregth
//		this.tieStrength = new TieStrength();

		initialNrofCopies = s.getInt(NROF_COPIES);
		isBinary = s.getBoolean(BINARY_MODE);
		initialEnergy = s.getInt(INIT_ENERGY_S);
		bufferSize = s.getInt(BUFFER_SIZE);
		String fclfromstring =s.getSetting(FCL_Context);
		fclcontextaware = FIS.load(fclfromstring);

//		if (fclcontextaware == null) {
//			System.out.println("Error: Gagal memuat file FCL. Pastikan formatnya benar dan file tersedia!");
//		} else {
//			System.out.println("FCL berhasil dimuat!");
////			System.out.println(fclcontextaware);
//		}

//		System.out.println("BufferSize: " + bufferSize);

//		// Jika initialEnergy ada dalam settings, ambil nilainya
//		if (testSPWAWARE.contains("initialEnergy")) {
//			initialEnergy = testSPWAWARE.getInt("initialEnergy");
//		} else {
//			initialEnergy = 5000; // Default jika tidak ditemukan di settings
//		}

//		System.out.println("Router initialized: initialNrofCopies = " + initialNrofCopies);
//		// Print nilai energi saat inisialisasi
////		initialEnergy =testSPWAWARE.getInt(INIT_ENERGY_S);
//		System.out.println("Router initialized: Energy = " + initialEnergy);
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ContextSprayAndWaitRouter(ContextSprayAndWaitRouter r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
		this.isBinary = r.isBinary;
		//Buffer
		this.bufferSize = r.bufferSize;
		//Energi
		this.initialEnergy = r.initialEnergy;

		//FCL
		this.fclcontextaware = r.fclcontextaware;

		//Popularity
//		this.popularity=r.popularity;
		this.alphaPopularity = r.alphaPopularity;
		//TieStrength
//		this.tieStrength = r.tieStrength;

	}

	//Getter FCL
	protected FIS getFclcontextaware(){
		return fclcontextaware;
	}

	// Method untuk mengatur node relay yang dipilih
	public void setRelayNode(DTNHost relayNode) {
		this.selectedRelayNode = relayNode;
	}

	// Method untuk mendapatkan relay node yang dipilih
	public DTNHost getRelayNode() {
		return selectedRelayNode;
	}
	
	@Override
	public int receiveMessage(Message m, DTNHost from) {
		EnergyModel energy = getEnergyModel(); //Mengambil dari Active
		// Cek apakah node masih memiliki energi sebelum menerima pesan
		if (!hasEnergy()) {
			return DENIED_LOW_RESOURCES; // Tolak pesan jika tidak ada energi
		}
		// Kurangi energi karena ada proses discovery
		energy.reduceDiscoveryEnergy();

		//Popularity
//		popularity.recordEncounter(this.getHost(), from);
//		popularity.updatePopularity();

		//TieStrength
//		tieStrength.recordEncounter(this.getHost(), from);
//		tieStrength.recordEncounter(from, this.getHost());


//		System.out.println("Energy after receiving message: " + energy.getEnergy());
		return super.receiveMessage(m, from);
	}


	/// Daftra Tetangga
//	public static List<DTNHost> getNeighbors(DTNHost host, Popularity popularity, TieStrength tieStrength) {
//		List<DTNHost> neighbors = new ArrayList<>();
//		List<Connection> connections = host.getConnections();
//
//
//		// Debugging: Cek jumlah koneksi dan status masing-masing koneksi
////		System.out.println("Total connections: " + connections.size());
//		if (popularity == null) {
//			System.err.println("Error: Popularity object is NULL!");
//			return neighbors; // Mengembalikan daftar kosong agar tidak crash
//		}
//
//		if (tieStrength == null){
//			System.out.println("Error TieStrength Null");
//			return neighbors;
//		}
//
//		// Dapatkan popularitas node saat ini
////		double currentNodePopularity = popularity.getPopularity(host);
////		System.out.println("Popularitas Node " + host.getAddress() + ": " + currentNodePopularity);
//
//		for (Connection conn : connections) {
//			// Periksa apakah koneksi aktif
//			if (conn.isUp()) {
//				DTNHost neighbor = conn.getOtherNode(host);
//				neighbors.add(neighbor);
////				System.out.println("Connection to active neighbor: " + neighbor.getAddress());
//
//				//Popularity
//				// Dapatkan dan cetak popularitas tetangga
////				double neighborPopularity = popularity.getPopularity(neighbor);
////				System.out.println("Tetangga: " + neighbor.getAddress() + ", Popularitas: " + neighborPopularity);
//
//				//tieStrength
////				double neighborTieStrength =tieStrength.getTieStrength(host, neighbor);
////				System.out.println("Tetangga: " + neighbor.getAddress() + ", tiestrength: " + neighborTieStrength);
//			}
//		}
////		// Cetak hanya jika ada lebih dari 2 tetangga aktif
////		if (neighbors.size() >= 2) {
////			System.out.println("Node " + host.getAddress() + " memiliki " + neighbors.size() + " tetangga dengan koneksi aktif.");
////
////			// Menampilkan alamat tetangga dengan koneksi aktif
////			for (DTNHost neighbor : neighbors.values()) {
////				System.out.println("- " + neighbor.getAddress());
////			}
////		}
//
//		return neighbors;
//	}
	
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		
		assert nrofCopies != null : "Not a SnW message: " + msg;
		
		if (isBinary) {
			/* in binary S'n'W the receiving node gets ceil(n/2) copies */
			nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		}
		else {
			/* in standard S'n'W the receiving node gets only single copy */
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
//		System.out.println("Energy after messageTransferred: " + energy.getEnergy());
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
	
	@Override 
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
//		System.out.println("Energi After Create Massage"+energy.getEnergy());

		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, Integer.valueOf(initialNrofCopies));
		addToMessages(msg, true);
		return true;
	}



	@Override
	public void update() {
		super.update();

		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}

//		//Penghapusan Pesan
//		List<Message> toRemove = new ArrayList<>();
//		for (Message m : getMessageCollection()) {
//			if (m.getTtl() <= 0) {
//				toRemove.add(m);
//			}
//		}
//
//		for (Message m : toRemove) {
//			deleteMessage(m.getId(), false);
//		}

		// Mengambil daftar tetangga aktif
//		List<DTNHost> neighbors = getNeighbors(this.getHost(), this.popularity, this.tieStrength); // Menampilkan tetangga aktif

		/// EvaluateNeighbor class
//		routing.testContext.NeighborEvaluator.evaluateNeighbors(this.getHost(), this.popularity, this.tieStrength);


		DTNHost relayNode =getRelayNode();
		if (relayNode != null) {

			/* create a list of SAWMessages that have copies left to distribute */
			@SuppressWarnings(value = "unchecked")
			List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());

			if(!copiesLeft.isEmpty()) {
				tryMessagesToConnections(copiesLeft, getConnections());
			}
		}
		


//		// Lakukan pengiriman pesan ke tetangga aktif
//		if (!neighbors.isEmpty()) {
//			// Lakukan pengiriman pesan ke tetangga aktif (misalnya dengan mencoba mengirim pesan)
//			tryMessagesToConnections(copiesLeft, getConnections());
//		}
	}
	
	/**
	 * Creates and returns a list of messages this router is currently
	 * carrying and still has copies left to distribute (nrof copies > 1).
	 * @return A list of messages that have copies left
	 */
	protected List<Message> getMessagesWithCopiesLeft() {
		List<Message> list = new ArrayList<Message>();

		for (Message m : getMessageCollection()) {
			Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
			assert nrofCopies != null : "SnW message " + m + " didn't have " + 
				"nrof copies property!";
			if (nrofCopies > 1) {
				list.add(m);
			}
		}
		
		return list;
	}
	
	/**
	 * Called just before a transfer is finalized (by 
	 * {@link ActiveRouter#update()}).
	 * Reduces the number of copies we have left for a message. 
	 * In binary Spray and Wait, sending host is left with floor(n/2) copies,
	 * but in standard mode, nrof copies left is reduced by one. 
	 */
	@Override
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

//		if (nrofCopies <= 0) {
//			System.out.println("Menghapus pesan: " + msgId);
//			deleteMessage(msgId, false);
//			System.out.println("Buffer setelah penghapusan: " + getBufferSize());
////			deleteMessage(msgId, false); // Hapus pesan dari buffer
//		} else {
//			msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
//		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
	}


	@Override
	public ContextSprayAndWaitRouter replicate() {
		return new ContextSprayAndWaitRouter(this);
	}
}
