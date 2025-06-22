package routing.testContext;

import core.SimClock;
/**
 * Kelas EncounterInfo menyimpan informasi mengenai encounter (pertemuan)
 * antara node dalam jaringan Delay-Tolerant Network (DTN).
 * Informasi ini mencakup waktu pertemuan, kapasitas buffer yang tersisa,
 * energi yang tersisa, dan durasi koneksi.
 */
public class EncounterInfo {
    public double MeetingTime;
    public int RemainingBuffer;
    public double RemainingEnergy;
    public double connDuration;

    public double lastSeenTime; // Waktu terakhir terlihat dalam waktu simulasi

    /**
     * Membuat objek EncounterInfo.
     * @param meetingTime Waktu saat encounter terjadi.
     * @param remainingBuffer Kapasitas buffer yang tersisa pada node yang ditemui.
     * @param remainingEnergy Energi yang tersisa pada node yang ditemui.
     * @param connDuration Durasi koneksi dengan node yang ditemui.
     */
    public EncounterInfo(double meetingTime, int remainingBuffer, double remainingEnergy, double connDuration) {
        this.RemainingBuffer = remainingBuffer;
        this.RemainingEnergy = remainingEnergy;
        this.MeetingTime = meetingTime;
        this.connDuration = connDuration;

        this.lastSeenTime = SimClock.getTime();
    }
}
