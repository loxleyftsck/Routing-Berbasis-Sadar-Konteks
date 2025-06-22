package routing.contextAware.Crips;

import core.DTNHost;

/**
 * CripsContextAware
 * Kelas ini mengevaluasi konteks fisik dan sosial untuk routing pada Delay Tolerant Network (DTN).
 * Digunakan untuk menentukan kelayakan melakukan forwarding pesan berdasarkan buffer, energi,
 * popularitas, dan tie strength.
 */
public class CripsContextAware {

    // ==============================
    // ENUM: Level Klasifikasi
    // ==============================

    public enum BufferLevel { LOW, MEDIUM, HIGH }
    public enum EnergyLevel { LOW, MEDIUM, HIGH }
    public enum AbilityNode { VBAD, BAD, GOOD, PERFECT }

    public enum PopLevel { SLOW, MED, FAST }
    public enum TieLevel { POOR, FAIR, GOOD }
    public enum SocialImportance { BAD, GOOD, PERFECT }
    public enum transferOpportunity { LOW, MED, HIGH, VHIGH }

    // ==============================
    // 1. Klasifikasi Buffer & Energi
    // ==============================

    public static BufferLevel classifyBuffer(double value) {
        if (value <= 0.4) return BufferLevel.LOW;
        else if (value <= 0.7) return BufferLevel.MEDIUM;
        else return BufferLevel.HIGH;
    }

    public static EnergyLevel classifyEnergy(double value) {
        if (value <= 0.4) return EnergyLevel.LOW;
        else if (value <= 0.7) return EnergyLevel.MEDIUM;
        else return EnergyLevel.HIGH;
    }

    // ==============================
    // 2. Evaluasi Kemampuan Node (Ability)
    // ==============================

    public static AbilityNode evaluateAbility(BufferLevel buffer, EnergyLevel energy) {
        if (buffer == BufferLevel.HIGH && energy == EnergyLevel.HIGH) return AbilityNode.PERFECT;
        if (buffer == BufferLevel.HIGH && energy == EnergyLevel.MEDIUM) return AbilityNode.PERFECT;
        if (buffer == BufferLevel.HIGH && energy == EnergyLevel.LOW) return AbilityNode.BAD;
        if (buffer == BufferLevel.MEDIUM && energy == EnergyLevel.HIGH) return AbilityNode.PERFECT;
        if (buffer == BufferLevel.MEDIUM && energy == EnergyLevel.MEDIUM) return AbilityNode.GOOD;
        if (buffer == BufferLevel.MEDIUM && energy == EnergyLevel.LOW) return AbilityNode.BAD;
        if (buffer == BufferLevel.LOW && energy == EnergyLevel.HIGH) return AbilityNode.GOOD;
        if (buffer == BufferLevel.LOW && energy == EnergyLevel.MEDIUM) return AbilityNode.BAD;
        if (buffer == BufferLevel.LOW && energy == EnergyLevel.LOW) return AbilityNode.VBAD;

        return AbilityNode.VBAD; // fallback default
    }

    // ==============================
    // 3. Klasifikasi Sosial
    // ==============================

    public static PopLevel classifyPopularity(double value) {
        if (value <= 0.4) return PopLevel.SLOW;
        else if (value <= 0.7) return PopLevel.MED;
        else return PopLevel.FAST;
    }

    public static TieLevel classifyTieStrength(double value) {
        if (value <= 0.4) return TieLevel.POOR;
        else if (value <= 0.7) return TieLevel.FAIR;
        else return TieLevel.GOOD;
    }

    // ==============================
    // 4. Evaluasi Kepentingan Sosial
    // ==============================

    public static SocialImportance evaluateSocial(PopLevel pop, TieLevel tie) {
        if (pop == PopLevel.FAST && tie == TieLevel.GOOD) return SocialImportance.PERFECT;
        if (pop == PopLevel.FAST && tie != TieLevel.POOR) return SocialImportance.GOOD;
        if (pop == PopLevel.MED && tie != TieLevel.POOR) return SocialImportance.GOOD;
        if (pop == PopLevel.SLOW && tie == TieLevel.GOOD) return SocialImportance.GOOD;
        if ((pop == PopLevel.MED || pop == PopLevel.SLOW) && tie == TieLevel.POOR) return SocialImportance.BAD;
        return SocialImportance.BAD;
    }

    public static int socialImportanceBinary(SocialImportance si) {
        return (si == SocialImportance.GOOD || si == SocialImportance.PERFECT) ? 1 : 0;
    }

    // ==============================
    // 5. Evaluasi Kesempatan Transfer
    // ==============================

    public static transferOpportunity evaluateTransferOpportunity(AbilityNode ab, SocialImportance soc) {
        if (ab == AbilityNode.PERFECT && soc != SocialImportance.BAD) return transferOpportunity.VHIGH;
        if (ab == AbilityNode.PERFECT && soc == SocialImportance.BAD) return transferOpportunity.MED;
        if (ab == AbilityNode.GOOD && soc == SocialImportance.PERFECT) return transferOpportunity.HIGH;
        if (ab == AbilityNode.GOOD && soc == SocialImportance.GOOD) return transferOpportunity.HIGH;
        if (ab == AbilityNode.GOOD && soc == SocialImportance.BAD) return transferOpportunity.LOW;
        if (ab == AbilityNode.BAD && soc == SocialImportance.PERFECT) return transferOpportunity.MED;
        return transferOpportunity.LOW;
    }

    public static int transferOppBinary(transferOpportunity opp) {
        return (opp == transferOpportunity.MED || opp == transferOpportunity.HIGH || opp == transferOpportunity.VHIGH) ? 1 : 0;
    }

    // ==============================
    // 6. Evaluasi Crisp Tetangga (Routing Decision)
    // ==============================

    public double evaluateNeighbor(DTNHost host, DTNHost neighbor,
                                   int freeBufferNeighbor, int remainingEnergyNeighbor,
                                   double popularityNeighbor, double tieStrengthNeighbor) {

        // Normalisasi buffer & energi
        double bufferKb = freeBufferNeighbor / 1024.0;
        double maxBufferKB = 10 * 1024.0;
        double normalizedBuffer = Math.min(bufferKb / maxBufferKB, 1.0);

        double maxEnergy = 500.0;
        double normalizedEnergy = Math.min(remainingEnergyNeighbor / maxEnergy, 1.0);

        // Evaluasi Ability
        BufferLevel bufferLevel = classifyBuffer(normalizedBuffer);
        EnergyLevel energyLevel = classifyEnergy(normalizedEnergy);
        AbilityNode abilityLevel = evaluateAbility(bufferLevel, energyLevel);

        // Evaluasi Social Importance
        PopLevel popularityLevel = classifyPopularity(popularityNeighbor);
        TieLevel tieStrengthLevel = classifyTieStrength(tieStrengthNeighbor);
        SocialImportance socialImportance = evaluateSocial(popularityLevel, tieStrengthLevel);

        // Evaluasi Transfer Opportunity
        transferOpportunity opp = evaluateTransferOpportunity(abilityLevel, socialImportance);
        return transferOppBinary(opp); // 1 = boleh transfer, 0 = tidak
    }

    // ==============================
    // 7. Evaluasi Sosial Diri Sendiri
    // ==============================

    public int evaluateSelf(DTNHost host, double popularity, double tieStrength) {
        PopLevel pop = classifyPopularity(popularity);
        TieLevel tie = classifyTieStrength(tieStrength);
        SocialImportance social = evaluateSocial(pop, tie);
        return socialImportanceBinary(social); // 1 = layak, 0 = tidak
    }
}
