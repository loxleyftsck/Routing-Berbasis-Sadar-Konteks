# 🛰️ CARL-DTN: Context-Aware RL Routing untuk Delay Tolerant Network

Repositori ini berisi implementasi protokol **routing berbasis Reinforcement Learning (Q-learning)** dan evaluasi konteks menggunakan **CRIPS**

## 🧠 Konsep Utama

- **DTN (Delay Tolerant Network)**: Jaringan tanpa jaminan koneksi end-to-end.
- **Store-Carry-Forward**: Node menyimpan pesan dan meneruskannya saat ada koneksi.
- **Context-Aware Routing**: Keputusan forwarding berdasarkan:
  - Node Ability (battery, buffer)
  - Social Utility (tie-strength, popularity)
  - Message Context (TTL, hop-count)
- **FCRIPS: Menentukan kualitas node secara linguistik (Good, Bad, Perfect).
- **Q-Learning**: Belajar nilai optimal untuk next-hop.


## ⚙️ Komponen Sistem

| Modul             | Fungsi |
|------------------|--------|
| `LinkManager`    | Mendeteksi ENS (Encountered Node Set) dan menghitung density |
| `CripsController`| Evaluasi konteks fisik, sosial, dan pesan  |
| `QLearningAgent` | Update Q-Table berdasarkan reward dan nilai Crips |
| `RoutingEngine`  | Pilih relay node terbaik |
| `CopyController` | Kontrol jumlah salinan berdasarkan density |
| `BufferManager`  | Prioritas pesan (Urgent, Normal, Low)|


## 📁 Struktur Folder

```
src/
├── routing/
│   └── contextAware/
│       ├── ContextAwareRLRouter.java
│       ├── ContextMessage/
│       │   ├── MessageListTable.java
│       │   └── MessagePriority.java
│       ├── Crips/
│       │   ├── CripsContextAware.java
│       │   └── CripsContextMsg.java
│       ├── DensityMCopies/
│       │   └── NetworkDensityCalculator.java
│       ├── ENS/
│       │   ├── EncounteredNode.java
│       │   ├── EncounteredNodeSet.java
│       │   └── ConnectionDuration.java
│       └── SocialCharacteristic/
│           ├── Popularity.java
│           └── TieStrength.java
├── reinforcementLearning_ContextAware/
│   ├── Qtable.java
│   └── QTableUpdateStrategy.java
```

## Fitur Utama

- **Evaluasi Konteks CRIPS**:
  - Evaluasi Fisik (Battery, Buffer)
  - Evaluasi Sosial (Popularity, Tie Strength)
  - Evaluasi Pesan (TTL, Hop Count)
  - Transfer Opportunity (gabungan Evaluasi Fisik + Evaluasi Sosial)
- **Q-Learning Adaptif**:
  - Update saat encounter (`QTableUpdateStrategy`)
  - Sinkronisasi antar node saat transfer
  - Decay nilai Q saat koneksi hilang
- **Kontrol Salinan Pesan**:
  - Gunakan `NetworkDensityCalculator` (ENS-aware)
  - Atur jumlah salinan berdasarkan density lokal
- **Buffer Management**:
  - Prioritaskan pesan urgensi tinggi terlebih dahulu
  - Hindari overload buffer dengan pesan usang
