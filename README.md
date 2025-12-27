# 🛰️ CARL-DTN: Context-Aware Reinforcement Learning for Delay Tolerant Networks

[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)](https://www.java.com/)
[![ONE Simulator](https://img.shields.io/badge/ONE%20Simulator-v1.4.1-blue)](https://akeranen.github.io/the-one/)
[![License](https://img.shields.io/badge/License-GPL%20v3-green.svg)](LICENSE.txt)
[![Research](https://img.shields.io/badge/Research-DTN%20Routing-purple)](https://github.com/loxleyftsck/Routing-Berbasis-Sadar-Konteks)

> **An intelligent routing protocol for Delay Tolerant Networks that combines Context-Aware decision making with Q-Learning reinforcement learning to optimize message delivery in challenged network environments.**

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Problem Statement](#-problem-statement)
- [Solution Architecture](#-solution-architecture)
- [Core Features](#-core-features)
- [Technical Implementation](#-technical-implementation)
- [Simulation & Evaluation](#-simulation--evaluation)
- [Getting Started](#-getting-started)
- [Project Structure](#-project-structure)
- [Research Context](#-research-context)
- [Future Work](#-future-work)
- [License](#-license)

---

## 🎯 Overview

**CARL-DTN** (Context-Aware Reinforcement Learning for DTN) is a novel routing protocol designed for **Delay Tolerant Networks (DTNs)**, where traditional end-to-end connectivity cannot be assumed. This project implements an intelligent routing mechanism that:

- 📊 **Evaluates node context** using physical (battery, buffer), social (popularity, tie-strength), and message properties (TTL, hop-count)
- 🧠 **Learns optimal routing decisions** through Q-Learning reinforcement learning
- 🔄 **Adapts to network dynamics** by adjusting message copy counts based on local density
- ⚡ **Prioritizes critical messages** using CRIPS evaluation

Built on top of **The ONE Simulator** (Opportunistic Network Environment), CARL-DTN provides a complete framework for simulating and evaluating context-aware routing strategies in challenged network scenarios.

---

## 🔍 Problem Statement

### Delay Tolerant Networks (DTN)

DTNs operate in environments where:

- **No guaranteed end-to-end connectivity** exists
- **Frequent network partitions** occur
- **Long propagation delays** are common
- **Limited resources** (battery, buffer, bandwidth) constrain operations

Examples include: deep-space communications, wildlife tracking networks, disaster recovery scenarios, vehicular networks, and remote sensor deployments.

### The Challenge

Traditional routing protocols (AODV, DSR, OLSR) fail in DTN environments because they assume:

1. Contemporaneous end-to-end paths exist
2. Link status can be determined quickly
3. Routing decisions can be made immediately

### Research Gap

Existing DTN routing protocols have limitations:

| Protocol | Limitation |
|----------|-----------|
| **Epidemic** | Excessive resource consumption due to flooding |
| **PRoPHET** | Relies solely on delivery predictability, ignores node context |
| **Spray and Wait** | Fixed spray count, doesn't adapt to network dynamics |

**CARL-DTN addresses these gaps** by:

- ✅ Incorporating multi-dimensional context awareness (physical + social + message)
- ✅ Learning optimal forwarding decisions through reinforcement learning
- ✅ Dynamically adapting behavior based on network density
- ✅ Prioritizing messages based on CRIPS linguistic evaluation

---

## 🏗️ Solution Architecture

### System Architecture

<img width="1024" height="617" alt="image" src="https://github.com/user-attachments/assets/f26a1c3a-c795-4c23-b4f5-93306a51406c" />


### CRIPS Context Evaluation Flow

<img width="1024" height="559" alt="image" src="https://github.com/user-attachments/assets/05b9e553-b38e-4242-93b0-e01e526336a7" />


### Key Components

#### 1. **LinkManager** - Encounter Detection

- Detects encountered nodes (ENS - Encountered Node Set)
- Tracks connection duration and frequency
- Calculates local network density

#### 2. **CripsController** - Context Evaluation

Evaluates three dimensions of context using **CRIPS** (Context-aware Routing using Intelligent Prediction System):

**Physical Context:**

- **Battery Level** (%)
- **Buffer Utilization** (%)

**Social Context:**

- **Popularity** - How many unique nodes have encountered this node
- **Tie Strength** - Duration and frequency of encounters with specific nodes

**Message Context:**

- **Time-to-Live (TTL)** - Remaining message lifetime
- **Hop Count** - Number of forwards

Output: **Transfer Opportunity Score** → Linguistic value (Perfect, Good, Moderate, Bad)

#### 3. **QLearningAgent** - Adaptive Learning

- Maintains Q-Table: `Q(message_destination, next_hop_candidate)`
- Updates Q-values based on Transfer Opportunity reward
- Implements decay mechanism when connections fail
- Synchronizes Q-Table knowledge between nodes during encounters

**Update Strategy:**

```
<img width="718" height="47" alt="image" src="https://github.com/user-attachments/assets/4d2f2465-9091-4697-9ffa-2887d03320e6" />

```

<img width="843" height="627" alt="image" src="https://github.com/user-attachments/assets/dc9cc23c-ec9f-4e5a-89ea-7198a9841cc4" />


#### 4. **RoutingEngine** - Next-Hop Selection

Selects best relay node by:

1. Filtering eligible candidates (not message source, has buffer space)
2. Ranking by Q-value for message destination
3. Considering Transfer Opportunity score as tiebreaker

#### 5. **CopyController** - Density-Based Replication

- Calculates message copies based on ENS density
- High density → Fewer copies (spray conservatively)
- Low density → More copies (increase delivery probability)

#### 6. **BufferManager** - Priority Queue Management

- Assigns CRIPS priority score to each message based on message context evaluation
- Drops lowest-priority messages when buffer is full
- Ensures critical messages (high urgency, low TTL) are retained

---

## ✨ Core Features

### 🔬 Multi-Dimensional Context Awareness

CARL-DTN makes forwarding decisions based on comprehensive context evaluation:

```
Context Awareness = f(Physical, Social, Message)
```

- **Physical**: Ensures nodes with sufficient resources are prioritized
- **Social**: Leverages social network properties for better relay selection
- **Message**: Prioritizes time-critical and low-hop-count messages

### 🧠 Q-Learning Integration

- **State**: Message destination
- **Action**: Choose next-hop relay
- **Reward**: Transfer Opportunity score (from CRIPS)
- **Policy**: Exploit learned Q-values with exploration via Transfer Opportunity

**Advantages:**

- Learns from experience over time
- Adapts to changing network conditions
- Shares knowledge between nodes during encounters

### 📡 ENS (Encountered Node Set) Mechanism

Tracks for each encountered node:

- **Total Encounters**: Count
- **Last Encounter Time**: Timestamp
- **Connection Duration**: Cumulative
- **Encounter Frequency**: Rate of meetings

**Used for:**

- Calculating social metrics (popularity, tie-strength)
- Estimating network density
- Adaptive copy control

### 🎛️ Adaptive Copy Control

Instead of fixed spray count (Spray and Wait), CARL-DTN calculates:

```java
int copies = calculateCopiesBasedOnDensity(networkDensity);
```

- **Dense network** → Fewer copies (avoid congestion)
- **Sparse network** → More copies (increase probability)

### 📋 CRIPS Priority-Based Buffer Management

Messages are prioritized using CRIPS evaluation:

- **Inputs**: TTL remaining, hop count, message size
- **Output**: Priority score (Urgent, Normal, Low)
- **Drop Policy**: Lowest priority first (FIFO within same priority)

---

## ⚙️ Technical Implementation

### Technology Stack

- **Language**: Java 6+
- **Simulator**: The ONE v1.4.1 (Opportunistic Network Environment)
- **CRIPS Framework**: FCL-based CRIPS evaluation engine
- **Build Tool**: Batch script (`compile.bat`)

### Directory Structure

```
src/
├── routing/
│   └── contextAware/
│       ├── ContextAwareRLRouter.java      # Main routing logic
│       ├── ContextMessage/
│       │   ├── MessageListTable.java      # Message priority tracking
│       │   └── MessagePriority.java       # Priority definitions
│       ├── Crips/
│       │   ├── CripsContextAware.java     # Node context evaluation (FLC)
│       │   └── CripsContextMsg.java       # Message context evaluation (FLC)
│       ├── DensityMCopies/
│       │   └── NetworkDensityCalculator.java  # Adaptive copy control
│       ├── ENS/
│       │   ├── EncounteredNode.java       # Single encounter record
│       │   ├── EncounteredNodeSet.java    # ENS collection
│       │   └── ConnectionDuration.java    # Duration tracking
│       └── SocialCharacteristic/
│           ├── Popularity.java            # Popularity metric
│           └── TieStrength.java           # Tie strength calculation
├── reinforcementLearning_ContextAware/
│   ├── Qtable.java                        # Q-Table data structure
│   └── QTableUpdateStrategy.java          # Q-learning update logic
└── fcl/                                   # Fuzzy Control Language files
    ├── ContextAware.fcl                   # Physical + Social context
    ├── ContextMsg.fcl                     # Message context
    └── KeanggotaanFuzzy.fcl              # Fuzzy membership definitions
```

### Key Classes

| Class | Responsibility |
|-------|---------------|
| `ContextAwareRLRouter` | Main router implementing message handling, Q-learning updates, buffer management |
| `CripsContextAware` | Evaluates physical and social context using CRIPS |
| `CripsContextMsg` | Evaluates message context (TTL, hop-count) |
| `Qtable` | Stores and retrieves Q-values for (destination, next-hop) pairs |
| `QTableUpdateStrategy` | Implements Q-learning update equation |
| `EncounteredNodeSet` | Maintains ENS for a node, calculates density |
| `NetworkDensityCalculator` | Computes message copies based on ENS density |
| `MessageListTable` | Tracks CRIPS priority scores for all buffered messages |

---

## 🧪 Simulation & Evaluation

### Simulation Platform

**The ONE Simulator** provides:

- Realistic mobility models
- Configurable network interfaces
- Message routing simulation
- Comprehensive reporting modules

### Evaluation Scenarios

#### 1️⃣ **Time-Based Simulation**

- **Duration**: 5,000s - 43,000s
- **Metrics**: Message delivery ratio, overhead ratio over time
- **Goal**: Evaluate performance evolution as simulation progresses

#### 2️⃣ **Buffer Size Variation**

- **Buffer Sizes**: 5MB, 10MB, 15MB, 20MB, 25MB, 30MB, 35MB, 40MB
- **Metrics**: Delivery ratio, buffer drop rate
- **Goal**: Understand resource-performance tradeoff

#### 3️⃣ **TTL Variation**

- **TTL Values**: 120, 140, 180, 240, 300, 360, 420 minutes
- **Metrics**: Message expiration rate, delivery success
- **Goal**: Optimize TTL settings for different scenarios

### Movement Models

| Model | Description | Use Case |
|-------|-------------|----------|
| **MBM** (Map-Based Movement) | Nodes move along predefined paths | Urban vehicular networks |
| **RWP** (Random Waypoint) | Random destination selection | General DTN scenarios |
| **SPMBM** (Shortest Path Map-Based) | Dijkstra-based path planning | Realistic pedestrian movement |
| **RWK** (Random Walk) | Brownian motion-like movement | Wildlife tracking |

### Baseline Comparisons

CARL-DTN is compared against:

- **Epidemic Routing**: Simple flooding, baseline for maximum delivery
- **PRoPHET**: Probabilistic routing based on delivery predictability
- **Spray and Wait**: Controlled flooding with fixed copy limit

### Performance Metrics

- **Delivery Probability**: % of messages successfully delivered
- **Overhead Ratio**: (Relayed - Delivered) / Delivered
- **Average Latency**: Time from creation to delivery
- **Buffer Time**: Average time messages spend in buffers
- **Hop Count**: Average forwards per delivered message

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK)** 6 or higher
- **The ONE Simulator** v1.4.1 (included in this repository)
- **Git** (for cloning)

### Installation

1. **Clone the repository:**

```bash
git clone https://github.com/loxleyftsck/Routing-Berbasis-Sadar-Konteks.git
cd Routing-Berbasis-Sadar-Konteks
```

1. **Compile the project:**

```bash
# Windows
compile.bat

# Linux/macOS
chmod +x compile.bat
./compile.bat
```

1. **Verify compilation:**
Check that `.class` files are created in the project directories.

### Quick Start

#### Running Your First Simulation

**GUI Mode (Interactive):**

```bash
# Windows
one.bat ContextAwareGroupRL_settings.txt

# Linux/macOS
./one.sh ContextAwareGroupRL_settings.txt
```

**Batch Mode (Headless):**

```bash
# Run 5 simulation instances with different random seeds
./one.sh -b 5 ContextAwareGroupRL_settings.txt
```

#### Configuration Files

Key configuration files in `/src`:

- `ContextAwareGroupRL_settings.txt` - Main CARL-DTN configuration
- `ContextAwareGroupRL_KeanggotaanFuzzy.txt` - CRIPS membership settings
- `ContextAwareGroupRL_5MBsettings.txt` - 5MB buffer scenario
- `ContextAwareGroupRLTTL300_settings.txt` - TTL=300 scenario
- `CARL_Test_Epidemic_settings.txt` - Epidemic baseline
- `CARL_Test_Prophet_settings.txt` - PRoPHET baseline
- `CARL_Test_SprayAndWait_settings.txt` - Spray and Wait baseline

#### Viewing Results

Simulation reports are saved in `/reports` directory:

- `MessageStatsReport` - Delivery probability, overhead, latency
- `DeliveredMessagesReport` - Details of delivered messages
- `CreatedMessagesReport` - Message creation log
- `ContactTimesReport` - Node encounter durations

For detailed instructions, see **[QUICKSTART.md](QUICKSTART.md)**.

---

## 📂 Project Structure

```
Routing-Berbasis-Sadar-Konteks/
├── src/                          # Source code
│   ├── routing/                  # Routing protocols
│   │   ├── contextAware/         # CARL-DTN implementation
│   │   └── ...                   # Baseline protocols (Epidemic, PRoPHET, etc.)
│   ├── reinforcementLearning_ContextAware/  # Q-Learning implementation
│   ├── core/                     # ONE Simulator core
│   ├── movement/                 # Movement models
│   ├── report/                   # Report generators
│   └── fcl/                      # Fuzzy Control Language definitions
├── data/                         # Map data (WKT format)
├── lib/                          # External libraries (jFuzzyLogic, etc.)
├── reports/                      # Simulation output (generated)
├── compile.bat                   # Compilation script
├── one.bat / one.sh              # Execution scripts
├── README.md                     # This file
├── QUICKSTART.md                 # Quick start guide
├── ARCHITECTURE.md               # Detailed architecture documentation
└── LICENSE.txt                   # GPL v3 License
```

---

## 🎓 Research Context

### Academic Background

This project is built upon research in:

- **Delay Tolerant Networking (DTN)**: RFC 4838, Bundle Protocol
- **Opportunistic Networking**: Store-Carry-Forward paradigm
- **Reinforcement Learning**: Q-Learning, Temporal Difference Learning
- **CRIPS Evaluation**: CRIPS (Context-aware Routing using Intelligent Prediction System)
- **Social Network Analysis**: Tie strength, centrality metrics

### Key Research Contributions

1. **Multi-Dimensional Context Integration**: First protocol to combine physical, social, and message context in a unified CRIPS framework for DTN routing

2. **Adaptive Q-Learning**: Novel Q-learning approach where reward is derived from CRIPS context evaluation rather than binary delivery success

3. **Density-Aware Copy Control**: Dynamic spray count calculation based on real-time ENS density estimation

4. **CRIPS Priority Buffer Management**: Linguistic priority assignment for intelligent buffer management under resource constraints

### Related Work

- **PRoPHET** (Lindgren et al., 2003): Probabilistic routing
- **MaxProp** (Burgess et al., 2006): Priority-based routing
- **Bubble Rap** (Hui et al., 2008): Social-based routing
- **SimBetTS** (Daly & Haahr, 2009): Similarity and betweenness centrality

**CARL-DTN differentiates** by integrating RL-based learning with comprehensive context awareness and adaptive resource management.

---

## 🔮 Future Work

### Planned Enhancements

- [ ] **Deep Q-Learning (DQN)**: Replace tabular Q-learning with neural network approximation
- [ ] **Multi-Criteria Context**: Add geographic, temporal, and energy-harvesting contexts
- [ ] **Distributed Learning**: Implement federated Q-learning across node clusters
- [ ] **Real-World Deployment**: Port to Android/embedded devices for field testing
- [ ] **Security Mechanisms**: Implement trust-based forwarding and anomaly detection
- [ ] **Performance Optimization**: Parallel Q-table updates, incremental CRIPS evaluation

### Research Directions

- Comparative study with recent ML-based DTN protocols (LSTM, GNN-based)
- Scalability analysis for large-scale networks (1000+ nodes)
- Energy efficiency optimization for IoT/sensor network deployments
- Integration with Named Data Networking (NDN) architecture

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE.txt](LICENSE.txt) file for details.

The ONE Simulator is also GPL v3 licensed. Original ONE Simulator: <https://akeranen.github.io/the-one/>

---

## 🙏 Acknowledgments

- **The ONE Simulator Team** - For providing an excellent DTN simulation platform
- **FCL Framework** - For the CRIPS evaluation engine
- Research community in DTN, Opportunistic Networking, and Reinforcement Learning

---

## 📧 Contact & Contributions

**Repository**: [github.com/loxleyftsck/Routing-Berbasis-Sadar-Konteks](https://github.com/loxleyftsck/Routing-Berbasis-Sadar-Konteks)

Contributions, issues, and feature requests are welcome!

**For academic inquiries or collaboration**, please open an issue with the `[RESEARCH]` tag.

---

<div align="center">

**Built with ❤️ for advancing DTN research**

⭐ **Star this repository if you find it useful!** ⭐

</div>
