package routing.contextAware.ContextMessage;

import core.DTNHost;
import core.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * MessageListTable
 * Kelas ini digunakan untuk menyimpan dan mengelola skor prioritas pesan untuk tiap host.
 * Setiap pesan diidentifikasi berdasarkan ID-nya dan diberikan skor prioritas (biasanya 0.0 atau 1.0).
 */
public class MessageListTable {

    private DTNHost host;
    public final Map<String, Double> messagePriorityMap;

    /**
     * Konstruktor MessageListTable.
     * @param host Node DTN saat ini yang memiliki tabel pesan.
     */
    public MessageListTable(DTNHost host) {
        this.host = host;
        this.messagePriorityMap = new HashMap<>();
    }

    /**
     * Update atau set nilai prioritas dari sebuah pesan.
     * @param message Pesan yang akan diperbarui prioritasnya.
     * @param priority Nilai prioritas (misal 1.0 = layak disebar, 0.0 = tidak penting).
     */
    public void updateMessagePriority(Message message, double priority) {
        messagePriorityMap.put(message.getId(), priority);
    }

    /**
     * Ambil nilai prioritas dari pesan tertentu.
     * Jika tidak ditemukan, akan mengembalikan default 0.0.
     * @param message Pesan yang akan dicek.
     * @return Nilai prioritas (double).
     */
    public double getPriority(Message message) {
        return messagePriorityMap.getOrDefault(message.getId(), 0.0);
    }

    /**
     * Hapus entri prioritas dari pesan tertentu.
     * @param message Pesan yang akan dihapus dari tabel.
     */
    public void removeMessage(Message message) {
        messagePriorityMap.remove(message.getId());
    }

    /**
     * Cek apakah pesan tertentu sudah tercatat dalam tabel prioritas.
     * @param message Pesan yang dicek keberadaannya.
     * @return True jika ada, false jika tidak.
     */
    public boolean containsMessage(Message message) {
        return messagePriorityMap.containsKey(message.getId());
    }
}
