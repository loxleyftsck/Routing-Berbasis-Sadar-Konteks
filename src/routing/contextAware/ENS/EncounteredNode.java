package routing.contextAware.ENS;

import core.SimClock;

/**
 * EncounteredNode
 * Representasi dari node yang pernah ditemui oleh node saat ini,
 * menyimpan informasi seperti waktu encounter, energi, buffer, dan durasi koneksi.
 * Data ini digunakan untuk menghitung tie-strength, popularitas, dan validitas konteks sosial.
 */
public class EncounteredNode {

    private String nodeId;
    private long encounterTime;
    private double remainingEnergy;  // Dalam persen (0–100)
    private int bufferSize;          // Dalam MB
    private long connectionDuration; // Dalam detik

    private double popularity = 0.0;
    private int encounterCount = 0;

    private static final double TTLENS = 3600.0; // TTL data encounter (1 jam)

    // ========================= Constructor ========================= //

    /**
     * Konstruktor EncounteredNode saat encounter pertama.
     */
    public EncounteredNode(String nodeId, long encounterTime,
                           double remainingEnergy, int bufferSize, long connectionDuration) {
        this.nodeId = nodeId;
        this.encounterCount = 0;
        update(encounterTime, remainingEnergy, bufferSize, connectionDuration);
    }

    /**
     * Perbarui encounter dengan info terbaru.
     */
    public void update(long encounterTime, double remainingEnergy,
                       int bufferSize, long connectionDuration) {
        this.encounterTime = encounterTime;
        this.remainingEnergy = remainingEnergy;
        this.bufferSize = bufferSize;
        this.connectionDuration = connectionDuration;
    }

    /**
     * Tambahkan durasi koneksi dari encounter baru.
     */
    public void updateConnectionDuration(long duration) {
        this.connectionDuration += duration;
    }

    // ===================== Relevansi & Validasi ===================== //

    /**
     * Mengecek apakah encounter sudah kadaluarsa berdasarkan TTL.
     */
    public boolean isExpired() {
        return (SimClock.getTime() - this.encounterTime) > TTLENS;
    }

    /**
     * Bandingkan relevansi encounter saat ini dengan encounter lain.
     * Urutan prioritas: encounterTime > connectionDuration > energy > buffer.
     */
    public boolean isMoreRelevantThan(EncounteredNode other) {
        if (this.encounterTime > other.encounterTime) return true;
        if (this.encounterTime == other.encounterTime) {
            if (this.connectionDuration > other.connectionDuration) return true;
            if (this.connectionDuration == other.connectionDuration) {
                if (this.remainingEnergy > other.remainingEnergy) return true;
                return this.bufferSize > other.bufferSize;
            }
        }
        return false;
    }

    /**
     * Membuat duplikat EncounteredNode (deep copy).
     */
    public EncounteredNode clone() {
        EncounteredNode clone = new EncounteredNode(this.nodeId, this.encounterTime,
                this.remainingEnergy, this.bufferSize, this.connectionDuration);
        clone.setPopularity(this.popularity);
        clone.encounterCount = this.encounterCount;
        return clone;
    }

    // ===================== Social Characteristics ===================== //

    public double getPopularity() {
        return popularity;
    }

    public void setPopularity(double popularity) {
        this.popularity = popularity;
    }

    public int getEncounterCount() {
        return encounterCount;
    }

    public void incrementEncounterCount() {
        this.encounterCount++;
    }

    // ===================== Getters ===================== //

    public String getNodeId() {
        return nodeId;
    }

    public long getEncounterTime() {
        return encounterTime;
    }

    public double getRemainingEnergy() {
        return remainingEnergy;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public long getConnectionDuration() {
        return connectionDuration;
    }

    // ===================== Setters ===================== //

    public void setEncounterTime(long encounterTime) {
        this.encounterTime = encounterTime;
    }

    public void setRemainingEnergy(double remainingEnergy) {
        this.remainingEnergy = remainingEnergy;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setConnectionDuration(long connectionDuration) {
        this.connectionDuration = connectionDuration;
    }

    // ===================== Debug ===================== //

    @Override
    public String toString() {
        return "NodeID: " + nodeId +
                ", Encounter Time: " + encounterTime +
                ", Energy: " + remainingEnergy + "%" +
                ", Buffer: " + bufferSize + "MB" +
                ", Duration: " + connectionDuration + "s";
    }
}
