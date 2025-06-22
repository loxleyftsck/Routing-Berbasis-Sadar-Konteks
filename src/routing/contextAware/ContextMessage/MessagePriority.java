package routing.contextAware.ContextMessage;

import core.Message;

/**
 * MessagePriority
 * Kelas pembungkus (wrapper) untuk mengasosiasikan sebuah Message
 * dengan nilai prioritasnya (biasanya 0.0 atau 1.0).
 *
 * Cocok digunakan saat ingin menyimpan list pesan beserta nilai
 * prioritasnya, misalnya untuk sorting atau evaluasi manual.
 */
public class MessagePriority {

    public Message message;
    public double priority;

    /**
     * Konstruktor untuk membungkus objek Message dan prioritasnya.
     * @param message Objek pesan DTN.
     * @param priority Nilai prioritas pesan (0.0 sampai 1.0).
     */
    public MessagePriority(Message message, double priority) {
        this.message = message;
        this.priority = priority;
    }
}
