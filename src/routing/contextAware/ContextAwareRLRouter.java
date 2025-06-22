package routing.contextAware;

import core.Settings;
import net.sourceforge.jFuzzyLogic.FIS;
import reinforcementLearning_ContextAware.QTableUpdateStrategy;
import reinforcementLearning_ContextAware.Qtable;
import routing.ActiveRouter;
import core.*;
import routing.MessageRouter;
import routing.contextAware.ContextMessage.MessageListTable;
import routing.contextAware.DensityMCopies.NetworkDensityCalculator;
import routing.contextAware.ENS.*;
//import routing.contextAware.ENS.*;
import routing.contextAware.Crips.CripsContextAware;
import routing.contextAware.Crips.CripsContextMsg;
import routing.contextAware.SocialCharcteristic.*;


import java.util.*;

public class ContextAwareRLRouter extends ActiveRouter {

//    public static final String NROF_HOSTS = "nrofHosts";
    public static final String BUFFER_SIZE = "bufferSize";
    public static final String MSG_TTL = "msgTtl";
    public static final String INIT_ENERGY_S = "initialEnergy"; //Inisialisasi Energi
    public static final String ALPHA_POPULARITY = "alphaPopularity"; //Inisialisasi Alpha Popularity

//    public static final String FCL_Context = "fcl"; //FuzzyLogicController
//    public static final String FCL_MSG = "fclmsg"; //FuzzyLogicController

//    protected int nrofHosts;
    protected int bufferSize;
    protected int msgTtl;
    protected int initialEnergy;
    protected double alphaPopularity;
    protected FIS fclcontextaware; //FLC
    protected FIS fclcontextmsg;
    private double latestDensity;
    private Map<String, Double> pendingAging;

    private Popularity popularity; //Instance class popularity
    private TieStrength tieStrength; //Instance class TieStrength
    private EncounteredNodeSet encounteredNodeSet;
    private CripsContextAware cripsContextAware;
    private CripsContextMsg cripsContextMsg;
    private Qtable qtable;
    private MessageListTable messageListTable;

    public ContextAwareRLRouter(Settings s) {
        super(s);

        this.encounteredNodeSet = new EncounteredNodeSet();
        this.alphaPopularity = s.getDouble(ALPHA_POPULARITY);
        this.popularity = new Popularity(alphaPopularity);
        this.tieStrength = new TieStrength(); //Object TieStrength
        //Read FCL file
//        String FLCfromString = s.getSetting(FCL_Context);
//        fclcontextaware = FIS.load(FLCfromString);
//        String FCLmsgfromString = s.getSetting(FCL_MSG);
//        fclcontextmsg = FIS.load(FCLmsgfromString);

        this.cripsContextAware = new CripsContextAware();
        this.cripsContextMsg = new CripsContextMsg();

        bufferSize = s.getInt(BUFFER_SIZE);
        msgTtl = s.getInt(MSG_TTL);
        initialEnergy = s.getInt(INIT_ENERGY_S); //Energi
        latestDensity = -1;
        pendingAging = new HashMap<>();

//        this.deleteDelivered = true; //ACK-based deletion otomatis
    }

    protected ContextAwareRLRouter(ContextAwareRLRouter r) {
        super(r);

        this.encounteredNodeSet = r.encounteredNodeSet.clone();
        this.alphaPopularity = r.alphaPopularity;
        this.popularity = r.popularity;
        this.tieStrength = r.tieStrength;
//        this.fclcontextaware = r.fclcontextaware;
//        this.fclcontextmsg = r.fclcontextmsg;
        this.cripsContextAware = r.cripsContextAware;
        this.cripsContextMsg = r.cripsContextMsg;
        // biarkan init() yang akan mengisi ulang qtable
        this.qtable = null;
        this.messageListTable = null;

        this.bufferSize = r.bufferSize;
        this.msgTtl = r.msgTtl;
        this.initialEnergy = r.initialEnergy;
        this.latestDensity = r.latestDensity;
        this.pendingAging = r.pendingAging;

//        this.deleteDelivered = r.deleteDelivered;
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
//        System.out.println("Inisialisasi router...");
        super.init(host, mListeners);
        this.qtable = new Qtable(String.valueOf(this.getHost().getAddress()));
        this.messageListTable = new MessageListTable(this.getHost());


    }

    public void postInitQtable(Set<String> allHostIds) {
//        System.out.println("Memanggil postInitQtable...");
        if (this.qtable != null) {
            this.qtable.initializeAllQvalues(allHostIds);
//            String filePath = "C:/Users/LENOVO/Documents/the-one-ql-main/the-one-ql-main/src/reinforcementLearning_ContextAware/qtable.csv";
//            boolean append = (this.getHost().getAddress() != 0);
//            this.qtable.exportToCSV(filePath, append);
        } else {
            System.err.println("QTable belum diinisialisasi pada host: " + this.getHost());
        }
    }

    //Getter FLC
    public FIS getFIS() {
        return fclcontextaware;
    }

    public FIS getFISMsg(){
        return fclcontextmsg;
    }

    //Getter Q-Table
    public Qtable getQtable() {
        return this.qtable;
    }

    // Getter ENS untuk bisa diakses oleh node lain
    public EncounteredNodeSet getEncounteredNodeSet() {
        return this.encounteredNodeSet;
    }

    public Popularity getPopularity() {
        return this.popularity;
    }

    public TieStrength getTieStrength(){
        return this.tieStrength;
    }


    /**
     * Metode ini dipanggil ketika ada perubahan status koneksi antara dua node dalam jaringan.
     * Jika koneksi baru terbentuk, informasi pertemuan akan ditambahkan ke Encountered Node Set (ENS),
     * dan pertukaran ENS dilakukan antara kedua node. Jika koneksi terputus, durasi koneksi dicatat
     * dan informasi encounter dari ENS dihapus untuk node yang terputus.
     *
     * @param con Objek Connection yang merepresentasikan koneksi antara dua node.
     */
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        DTNHost host = getHost();
        DTNHost neighbor = con.getOtherNode(getHost());
        ContextAwareRLRouter neighborRouter = (ContextAwareRLRouter) neighbor.getRouter();
        EncounteredNodeSet neighborENS = neighborRouter.getEncounteredNodeSet();

        if (con.isUp()) {
            evaluateMessagePriorities();
            handleConnectionUp(host, neighbor, neighborRouter, neighborENS);
        } else {
            handleConnectionDown(neighbor, neighborENS);
        }
    }

    private void handleConnectionUp(DTNHost host, DTNHost neighbor, ContextAwareRLRouter neighborRouter, EncounteredNodeSet neighborENS) {
        long currentTime = (long) SimClock.getTime();
        String myId = String.valueOf(this.getHost().getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());

        double neighborEnergy = neighborRouter.getEnergyModel().getEnergy();
        // Casting langsung ke tipe int
        int integerEnergy = (int) neighborEnergy;
        int neighborBuffer = neighborRouter.getFreeBufferSize();

        // Mulai pencatatan durasi koneksi
        ConnectionDuration connectionDuration = ConnectionDuration.startConnection(this.getHost(), neighbor);
        long duration = (long) connectionDuration.getDuration();

        // Debug log sebelum update ENS
//        System.out.println("[INFO] Sebelum update ENS:");
        // Bersihkan ENS yang sudah kadaluarsa

        // !!!!
        this.encounteredNodeSet.removeOldEncounters();
        neighborENS.removeOldEncounters();

//        System.out.println("[DEBUG] ENS Sebelum tukar" + getHost().getAddress() + ":");
//        this.encounteredNodeSet.printEncounterLog(getHost(), String.valueOf(neighbor.getAddress()), neighborENS);

        this.popularity.updatePopularity(host, this.encounteredNodeSet);
        neighborRouter.getPopularity().updatePopularity(neighbor, neighborENS);
        // Ambil nilai popularitas terbaru setelah update
        double neighborPop = popularity.getPopularity(neighbor);
        double myPop = popularity.getPopularity(host);
//        System.out.println("[DEBUG] Popularitas host (" + host.getAddress() + "): " + myPop);
//        System.out.println("[DEBUG] Popularitas neighbor (" + neighbor.getAddress() + "): " + neighborPop);


        // Update ENS kedua node
        // Tambahkan ke ENS jika bukan diri sendiri
        if (!myId.equals(neighborId)) {
            this.encounteredNodeSet.updateENS(host, neighbor, neighborId, currentTime, integerEnergy, neighborBuffer, duration, neighborPop);
            neighborENS.updateENS(neighbor, host, myId, currentTime, integerEnergy, neighborBuffer, duration, myPop);

            this.encounteredNodeSet.recordEncounterBetween(host, neighbor);
        }
        // Perhitungan density (contextual)
        this.latestDensity = NetworkDensityCalculator.calculateNodeDensity(
                SimScenario.getInstance().getHosts().size(),
                this.encounteredNodeSet,
                neighborENS,
                myId,
                neighborId
        );

        // Pastikan ENS keduanya tidak kosong
        if (!this.encounteredNodeSet.isEmpty() && !neighborENS.isEmpty()) {
            // Saling tukar ENS dengan filtering otomatis
            this.encounteredNodeSet.exchangeWith(neighborENS, this.getHost(), neighbor, currentTime);
            neighborENS.exchangeWith(this.encounteredNodeSet, neighbor, this.getHost(), currentTime);

//            System.out.println("[DEBUG] ENS setelah saling tukar di " + getHost().getAddress() + ":");
//            this.encounteredNodeSet.printEncounterLog(getHost(), String.valueOf(neighbor.getAddress()), neighborENS);
        }

        this.tieStrength.calculateTieStrength(host, neighbor, this.encounteredNodeSet);
        double TieStrength = tieStrength.getTieStrength(host, neighbor);

        double transferOpportunity = cripsContextAware.evaluateNeighbor(this.getHost(),neighbor, neighborBuffer, integerEnergy, neighborPop, TieStrength);

        System.out.println("[DEBUG] energy: " + integerEnergy);
        updateQValueOnConUp(host,neighbor,transferOpportunity);
//        qtable.printQtableByHost(myId);
    }

    /**
     * Memperbarui Q-Value saat terjadi koneksi UP antara host dan neighbor.
     * Fungsi ini akan memeriksa semua pesan yang dimiliki oleh host.
     * Q-value hanya akan diperbarui jika:
     * Neighbor bukan pengirim asli pesan
     * Jika neighbor bukan tujuan akhir, maka Transfer Opportunity (TOpp) minimal 0.3
     * Q-value diperbarui menggunakan strategi pembaruan pertama.
     *
     * @param host Node yang memegang pesan dan ingin memperbarui Q-table-nya
     * @param neighbor Node yang baru terkoneksi (akan menjadi kandidat relay / action)
     * @param tfOpportunity Nilai Transfer Opportunity hasil evaluasi fuzzy logic (FLC4)
     */
    private void updateQValueOnConUp(DTNHost host, DTNHost neighbor, double tfOpportunity) {
        MessageRouter router = host.getRouter();
        Collection<Message> messages = router.getMessageCollection();
        String hostId = String.valueOf(host.getAddress());
        String nextHopId = String.valueOf(neighbor.getAddress());
        QTableUpdateStrategy qtableUpdate = new QTableUpdateStrategy(this.qtable);

        for (Message msg : messages){
            String destinationId = String.valueOf(msg.getTo().getAddress());
            String sourceId = String.valueOf(msg.getFrom().getAddress());
            // Jangan update Q jika neighbor adalah pengirim asli (menghindari loop tidak perlu
            if (nextHopId.equals(sourceId)) {
                System.out.println("[SKIP] Neighbor adalah pengirim asli pesan → " + msg.getId());
                continue;
            }
            // Ambil nilai Q sebelum update
            double oldQ = qtable.getQvalue(destinationId, nextHopId);
            qtableUpdate.updateFirstStrategy(host, neighbor, destinationId, nextHopId, tfOpportunity);
            // Ambil nilai Q setelah update
            double newQ = qtable.getQvalue(destinationId, nextHopId);

//            System.out.printf("[Q-CHANGE] dst = %s → via %s | Old Q = %.4f | New Q = %.4f%n",
//                    destinationId, nextHopId, oldQ, newQ);
        }
    }

    private void handleConnectionDown(DTNHost neighbor, EncounteredNodeSet neighborENS) {
        String myId = String.valueOf(this.getHost().getAddress());
        String neighborId = String.valueOf(neighbor.getAddress());
        try {
            // Akhiri pencatatan durasi koneksi
            ConnectionDuration connDuration = ConnectionDuration.getConnection(this.getHost(), neighbor);
            if (connDuration != null) {
                connDuration.endConnection(this.getHost(), neighbor, neighborENS);
//                System.out.printf("[DEBUG] Koneksi %s - %s diakhiri pada %.2f detik%n",
//                        this.getHost().getAddress(), neighbor.getAddress(), SimClock.getTime());
            }
            // Catat waktu putus di pendingAging
            pendingAging.put(neighborId, SimClock.getTime());
//            System.out.println("[DEBUG] Sebelum Dihapus karena terputus ");
//            this.encounteredNodeSet.printEncounterLog(this.getHost(), neighborId, neighborENS);
//            connDuration.printConnectionInfo(this.getHost(), neighbor);
        }
        finally {
            this.encounteredNodeSet.removeEncounter(neighborId);
            neighborENS.removeEncounter(myId);
        }

    }


    /**
     * Membuat dan menambahkan pesan baru ke buffer node.
     * Jika node ini adalah pengirim utama, akan dibuat salinan tambahan
     * berdasarkan kepadatan jaringan saat ini.
     *
     * @param m Pesan yang akan dibuat dan disimpan.
     * @return true jika pesan berhasil ditambahkan ke buffer.
     */
    @Override
    public boolean createNewMessage(Message m) {
        // Pastikan ada ruang di buffer untuk pesan baru
        makeRoomForMessage(m.getSize());
        // Hitung jumlah copies berdasarkan density node saat ini
        int copies = NetworkDensityCalculator.calculateCopiesBasedOnDensity(this.latestDensity);

        System.out.println("copies: " + copies);
        // Set TTL dan properti copies untuk pesan utama
        m.setTtl(msgTtl);
        m.addProperty("copies", copies);
        this.addToMessages(m, true);
//        System.out.println("Mssg" +m.getId()+"copies"+m.getProperty("copies"));

//        // Jika node ini adalah pengirim awal, buat salinan tambahan dan pesan belum punya properti "copies"
//        if (m.getFrom().equals(this.getHost()) && copies > 1) {
//            // Buat copies-1 salinan tambahan
//            for (int i = 1; i < copies; i++) {
//                Message copy = m.replicate(); // Salin pesan
//                copy.setTtl(msgTtl);
//                copy.updateProperty("copies", copies);
//                this.addToMessages(copy, true);
//            }
//        }
        return true;
    }

    /**
     * Mencoba mengosongkan buffer agar cukup untuk menyimpan pesan baru.
     * Pesan dengan prioritas paling rendah akan dihapus terlebih dahulu,
     * kecuali yang sedang dikirim (in transfer).
     *
     * @param size Ukuran pesan baru yang ingin dimasukkan ke buffer.
     * @return true jika cukup ruang berhasil disediakan, false jika tidak memungkinkan.
     */
    @Override
    protected boolean makeRoomForMessage(int size) {
        // Jika ukuran pesan lebih besar dari ukuran buffer total, langsung tolak
        if (size > this.getBufferSize()) {
            return false;
        }
        int freeBuffer = this.getFreeBufferSize();
        while (freeBuffer < size){
            // Ambil pesan dengan prioritas terendah (selain yang sedang dikirim)
//            Message Highest = getHighestPriorityMessage(true);
            Message toRemove= null;
//            if(Highest == null){
//                // Tidak ada lagi pesan yang bisa dihapus, gagal membuat ruang
//                return false;
//            }
            // 1. Coba cari pesan yang sudah delivered
            for (Message m : this.getMessageCollection()) {
                if (isDeliveredMessage(m)) {
                    toRemove = m;
                    break;
                }
            }
            // 2. Jika tidak ada pesan delivered, cari berdasarkan prioritas
            if (toRemove == null) {
                toRemove = getHighestPriorityMessage(true);
            }
            // 3. Kalau tidak ada yang bisa dihapus, return false
            if (toRemove == null) {
                return false;
            }
            Double priority = messageListTable.containsMessage(toRemove) ?
                    messageListTable.getPriority(toRemove) : 0.0;

            // Hapus pesan dari buffer (mode drop = true)
            deleteMessage(toRemove.getId(),true);
            System.out.printf("[BUFFER-DROP] Dropping message %s | priority = %.4f\n",
                    toRemove.getId(), priority);
            messageListTable.removeMessage(toRemove);
            // Tambah ruang kosong berdasarkan ukuran pesan yang dihapus
            freeBuffer += toRemove.getSize();
        }
        // Berhasil menyediakan ruang yang cukup
        return true;
    }

    /**
     * Mengembalikan pesan dengan prioritas fuzzy paling rendah di buffer,
     * dengan opsi mengecualikan pesan yang sedang dikirim.
     *
     * @param excludeMsgBeingSent Jika true, abaikan pesan yang sedang dikirim
     * @return Pesan dengan prioritas terendah, atau null jika tidak ada kandidat yang valid
     */
//    private Message getLowestPriorityMessage(boolean excludeMsgBeingSent){
//        Collection<Message> messages =this.getMessageCollection();
//        Message lowest=null;
//        for (Message m : messages){
//            // Lewati pesan yang sedang dikirim (jika diminta)
//            if(excludeMsgBeingSent && isSending(m.getId())) continue;
//            // Update jika belum ada kandidat, atau jika pesan ini lebih rendah prioritasnya
//            if(lowest==null || messageListTable.getPriority(m) < messageListTable.getPriority(lowest)){
//                lowest = m;
//            }
//        }
//        return lowest;
//    }
    private Message getHighestPriorityMessage(boolean excludeMsgBeingSent){
        Collection<Message> messages = this.getMessageCollection();
        Message highest = null;
        for (Message m : messages){
            if (excludeMsgBeingSent && isSending(m.getId())) continue;
            if (highest == null || messageListTable.getPriority(m) > messageListTable.getPriority(highest)) {
                highest = m;
            }
        }
        return highest;
    }

    /**
     * Mengurutkan daftar pesan berdasarkan nilai prioritas fuzzy (dari tinggi ke rendah).
     * Prioritas masing-masing pesan diambil dari {@code messageListTable} yang telah diisi sebelumnya.
     * Digunakan untuk menentukan urutan pengiriman pesan yang paling penting lebih dulu.
     *
     * @param messages Daftar pesan (List<Message>) yang akan diurutkan.
     */
    protected void sortByFuzzyPriority(List<Message> messages) {
        // Urutkan pesan berdasarkan nilai prioritas fuzzy secara menurun (descending)
        messages.sort(( m1, m2) -> {
            // Ambil nilai prioritas dari masing-masing pesan
            double p1 = messageListTable.getPriority(m1);
            double p2 = messageListTable.getPriority(m2);
            // Urutkan dari prioritas tertinggi ke terendah
            return Double.compare(p2, p1); //Desending (prioritas tinggi di awal)
        });
//        System.out.println("[SORT] Urutan pesan berdasarkan prioritas:");
//        for (Message m : messages) {
//            double pr = messageListTable.getPriority(m);
//            System.out.printf("  → %s (Priority: %.4f)\n", m.getId(), pr);
//        }
    }

    private void evaluateMessagePriorities(){
        Collection<Message> messages = this.getMessageCollection();
        for(Message m : messages){
            double priority = cripsContextMsg.evaluateMsg(this.getHost(), m.getTtl(), m.getHopCount());
            messageListTable.updateMessagePriority(m, priority);
        }
    }

    @Override
    protected Connection tryAllMessagesToAllConnections() {
        // Ambil semua koneksi node saat ini
        List<Connection> connections = getConnections();
        // Jika tidak ada koneksi atau tidak ada pesan, langsung return null
        if (connections.isEmpty() || this.getNrofMessages() == 0) {
            return null;
        }
        // Ambil semua pesan yang ada di buffer node ini
        List<Message> messages = new ArrayList<>(this.getMessageCollection());
        // Urutkan menggunakan sortByQueueMode()
        sortByFuzzyPriority(messages);
        for (Message msg : messages){
            for (Connection con : connections){
                copiesControlMechanism(msg, con);
            }
        }
        return null;
    }



    private void copiesControlMechanism(Message msg, Connection c){
        DTNHost host = this.getHost();
        DTNHost destination = msg.getTo();
        String hostId = String.valueOf(host.getAddress());
        String destinationId = String.valueOf(destination.getAddress());

        DTNHost neighbor = c.getOtherNode(host);
        String neighborId = String.valueOf(neighbor.getAddress());

        // 1. Cek apakah neighbor adalah pengirim asli pesan
        String sourceId = String.valueOf(msg.getFrom().getAddress());
        if (neighborId.equals(sourceId)) {
//            System.out.printf("[SKIP] Neighbor %s adalah pengirim asli pesan %s%n", neighborId, msg.getId());
            return;
        }

        boolean isDestination = neighborId.equals(destinationId);
        double messagePriority = messageListTable.getPriority(msg);
//        System.out.println("Message Priority :" + messagePriority);
        int copies = (int) msg.getProperty("copies");
//        System.out.println("Priority :" + messagePriority);
        // Forward langsung ke tujuan jika sedang terkoneksi ---
        if (isDestination && c.isReadyForTransfer()) {
            int result = startTransfer(msg, c);
            if (result == RCV_OK) {
                if (copies > 1) {
                    msg.updateProperty("copies", copies - 1);
                } else {
                    deleteMessage(msg.getId(), true);
                }
            }
            return;
        }

        // --- Forward ke neighbor berdasarkan FLC dan Q-Value ---
        if(c.isReadyForTransfer()){
            double myPop = popularity.getPopularity(host);
            double tie  = tieStrength.getTieStrength(host, neighbor);
            double mySocial = cripsContextAware.evaluateSelf(host, myPop, tie);

            double neighborPop  = popularity.getPopularity(neighbor);
            double neighborSocial = cripsContextAware.evaluateSelf(neighbor, neighborPop, tie);

            double myQ = qtable.getQvalue(destinationId, neighborId);
            double neighborQ = qtable.getQvalue(destinationId, hostId);

            boolean socialBetter = neighborSocial > mySocial;
            boolean qValueBetter = neighborQ > myQ;

            // --- Jika punya lebih dari 1 salinan ---
            // Jika memiliki lebih dari 1 salinan
            if (copies > 1) {
                if (isDestination) {
                    // Jika neighbor adalah tujuan langsung
                    int result = startTransfer(msg, c);
                    if (result == RCV_OK && this.hasMessage(msg.getId())) {
                        deleteMessage(msg.getId(), true);
                    }
                } else if (messagePriority <= 1.0) {
                    System.out.println("Flod" + messagePriority);
                    // Jika prioritas cukup dan minimal salah satu (OR) lebih baik
                    if (socialBetter || qValueBetter) {
                        MessageRouter targetRouter = neighbor.getRouter();
                        boolean alreadyHasMessage = targetRouter.getMessageCollection().stream()
                                .anyMatch(m -> m.getId().equals(msg.getId()));

                        int sendCopies = copies / 2;
                        int remainingCopies = copies - sendCopies;

                        System.out.printf("[SPRAY] %s has %d copies → Sending %d copies to %s, Remaining: %d%n",
                                msg.getId(), copies, sendCopies, neighborId, remainingCopies);
                        Message copy = msg.replicate();
                        copy.updateProperty("copies", sendCopies);


//                        System.out.printf("[CHECK] Neighbor %s already has message %s? %b%n",
//                                neighborId, msg.getId(), alreadyHasMessage);
                        if (!alreadyHasMessage) {
                            int result = startTransfer(copy, c);
                            if (result == RCV_OK) {
                                msg.updateProperty("copies", remainingCopies);
                                System.out.printf("[UPDATE] Message %s now has %d remaining copies%n",
                                        msg.getId(), remainingCopies);
                            } else {
                                System.out.printf("[TRANSFER FAIL] %s → %s | BUFFER %.2f | Result: %d%n",
                                        hostId, neighborId, (double) targetRouter.getFreeBufferSize(), result);
                            }
                        }
                    }
                }
            }
// Jika hanya memiliki 1 salinan
            else if (copies == 1) {
                if (isDestination) {
                    int result = startTransfer(msg, c);
                    if (result == RCV_OK && this.hasMessage(msg.getId())) {
                        deleteMessage(msg.getId(), true);
                    }
                } else if (messagePriority >= 1.0) {
                    // Harus memenuhi dua kondisi (AND) jika prioritas tinggi
                    if (socialBetter || qValueBetter) {
                        int result = startTransfer(msg, c);
                        if (result == RCV_OK && this.hasMessage(msg.getId())) {
                            deleteMessage(msg.getId(), true);
                        }
                    }
                }
            }
//            if (copies > 1 && messagePriority >= 0.4 && (socialBetter || qValueBetter)) {
//                int sendCopies = Math.max(1, copies / 2);
//                int remainingCopies = copies - sendCopies;
//
//                Message copy = msg.replicate();
////                copy.setTtl(msg.getTtl());
//                copy.updateProperty("copies", sendCopies);
//
//                MessageRouter targetRouter = neighbor.getRouter();
//                boolean alReadyHasMessage = targetRouter.getMessageCollection().stream()
//                        .anyMatch(m -> m.getId().equals(msg.getId()));
//
//                if (!alReadyHasMessage) {
//                    int result = startTransfer(copy, c);
//                    if (result == RCV_OK) {
//                        msg.updateProperty("copies", remainingCopies);
//                    } else {
//                        System.out.printf("[TRANSFER FAIL] %s → %s | BUFFER %.2f | Result: %d%n",
//                                hostId, neighborId, (double) targetRouter.getFreeBufferSize(), result);
//                    }
//                }
//            }
//            // --- Jika hanya tersisa 1 salinan (final hop) ---
//            else if (copies == 1) {
//                if(isDestination){
//                    int result = startTransfer(msg, c);
//                    if(result == RCV_OK){
//                        if(this.hasMessage(msg.getId())){
//                            deleteMessage(msg.getId(), true);
//                        }
//                    }
//                }
//                else if ((messagePriority >= 0.4 && socialBetter && qValueBetter) ||
//                        (messagePriority < 0.4 && (socialBetter || qValueBetter))) {
//                    int result = startTransfer(msg, c);
//                    if(result == RCV_OK){
//                        if(this.hasMessage(msg.getId())){
//                            deleteMessage(msg.getId(), true);
//                        }
//                    }
//                }
//            }
        }
    }


    /**
     * Override proses transfer pesan untuk mencatat hop dan menangani pengurangan salinan (copies)
     * hanya untuk pesan asli (bukan hasil replicate).
     *
     * @param m pesan yang akan ditransfer
     * @param con koneksi tujuan transfer
     * @return status hasil transfer (RCV_OK, TRY_LATER, dsb)
     */
    @Override
    protected int startTransfer(Message m, Connection con) {
        int result = super.startTransfer(m, con);
        if(result == RCV_OK){
            //Tambahkan hop
            m.addNodeOnPath(con.getOtherNode(this.getHost()));
        }
//        // Pesan ditolak karena sudah sampai tujuan
//        else if (result == DENIED_OLD && m.getTo() == con.getOtherNode(this.getHost())) {
//            if (this.hasMessage(m.getId())) {
//                System.out.printf("[ACK-DELETE] Pesan %s dihapus karena sudah sampai tujuan %s%n",
//                        m.getId(), m.getTo().getAddress());
//                deleteMessage(m.getId(), false);
//            }
//        }
        return result;
    }



    public void update(){
        super.update();

        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }
        QTableUpdateStrategy update = new QTableUpdateStrategy(this.qtable);
        update.processDelayedAging(this.getHost(), pendingAging);

        this.tryAllMessagesToAllConnections();
    }

//    // Memeriksa apakah node ini adalah penerima akhir dari pesan
//    private boolean isFinalDest(Message m, DTNHost to) {
//        return m.getTo() == to;  // Mengecek apakah penerima pesan adalah node ini
//    }
//
//    // Memeriksa apakah ini adalah pengiriman pertama untuk pesan ini
//    private boolean isFirstDelivery(Message m) {
//        return m.getHopCount() == 0;  // Jika hop count adalah 0, berarti ini pengiriman pertama
//    }

    /**
     * Method ini dipanggil saat pesan berhasil diterima dari node lain.
     * Method ini juga menangani proses sinkronisasi Q-table antara pengirim dan penerima
     * apabila salah satu dari dua kondisi berikut terpenuhi:
     * 1. Node ini adalah penerima akhir pesan.
     * 2. Node ini menerima pesan tersebut untuk pertama kalinya (sebagai relay pertama).
     * Sinkronisasi Q-table dilakukan dengan memanggil strategi sinkronisasi (updateThirdStrategy)
     * yang akan menggabungkan informasi pembelajaran antara pengirim dan penerima.
     *
     * @param id   ID dari pesan yang diterima.
     * @param from Node pengirim pesan.
     * @return Objek Message yang diterima.
     */
    @Override
    public Message messageTransferred(String id, DTNHost from) {
        // Deteksi apakah node ini belum pernah punya pesan ini
        boolean isFirstReception  = !this.hasMessage(id);

        // Panggil logika transfer standar dari super class
        Message msg = super.messageTransferred(id, from);

        // Validasi pesan dan pengirim (untuk menghindari NullPointerException)
        if (msg == null || from.getRouter() == null || !(from.getRouter() instanceof ContextAwareRLRouter)) {
            return msg;
        }

        // Cek apakah node ini adalah tujuan akhir
        boolean isFinalRecipient = msg.getTo() == this.getHost();

        // Sinkronisasi Q-table jika sesuai paper: final recipient atau first relay delivery
        if (isFinalRecipient || isFirstReception ) {
            Qtable senderQtable = ((ContextAwareRLRouter) from.getRouter()).getQtable();
            Qtable receiverQtable = this.getQtable();
            String senderQId = String.valueOf(from.getAddress());
            String receiverQId = String.valueOf(this.getHost().getAddress());
//            String focusDest = msg.getTo().toString();
            QTableUpdateStrategy.updateThirdStrategy(senderQtable, receiverQtable, senderQId, receiverQId);
        }

        return msg;
    }

    @Override
    public ContextAwareRLRouter replicate() {
        return new ContextAwareRLRouter(this);
    }
}
//            String senderID = String.valueOf(from.getAddress());
//            String reciverID = String.valueOf(this.getHost().getAddress());
//            Qtable senderQ = senderRouter.getQtable();
//            Qtable receiverQ = receiverRouter.getQtable();
//
//            System.out.println("Before QTable Sync:");
//            qtable.printQtable(senderID);
//            qtable.printQtable(reciverID);
//            QTableUpdateStrategy.updateQTables(senderQ, receiverQ);
//            System.out.println("After QTable Sync:");
//            qtable.printQtable(senderID);
//            qtable.printQtable(reciverID);
////            System.out.println("Before QTable Sync:");
////            senderQ.printQtable(senderID);
////            receiverQ.printQtable(reciverID);
//////            this.qtable.updateFrom(senderQ);
//
//            Qtable.syncQTables(senderQ, receiverQ, senderID, reciverID);
//
//            System.out.println("After QTable Sync:");
//            senderQ.printQtable(senderID);
//            receiverQ.printQtable(reciverID);