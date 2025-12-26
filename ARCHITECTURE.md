# 🏗️ CARL-DTN Technical Architecture

This document provides a deep-dive into the technical architecture, algorithms, and implementation details of the CARL-DTN routing protocol.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Core Components](#core-components)
3. [Context Evaluation (CRIPS)](#context-evaluation-crips)
4. [Q-Learning Implementation](#q-learning-implementation)
5. [Message Flow & Lifecycle](#message-flow--lifecycle)
6. [Algorithms & Pseudocode](#algorithms--pseudocode)
7. [Data Structures](#data-structures)
8. [Configuration & Parameters](#configuration--parameters)

---

## System Overview

### Architectural Layers

```
┌───────────────────────────────────────────────┐
│         Application Layer (Messages)          │
└───────────────────────────────────────────────┘
                      ↓
┌───────────────────────────────────────────────┐
│       ContextAwareRLRouter (Routing)          │
│  ┌─────────────┐  ┌──────────────────────┐   │
│  │ Q-Learning  │  │  CRIPS Evaluation    │   │
│  │   Agent     │←→│  (CRIPS Evaluation)      │   │
│  └─────────────┘  └──────────────────────┘   │
│  ┌─────────────┐  ┌──────────────────────┐   │
│  │   ENS       │  │  Buffer Manager      │   │
│  │  Manager    │  │  (Priority Queue)    │   │
│  └─────────────┘  └──────────────────────┘   │
└───────────────────────────────────────────────┘
                      ↓
┌───────────────────────────────────────────────┐
│        Network Layer (Connections)            │
└───────────────────────────────────────────────┘
```

### Design Principles

1. **Modular Architecture**: Each component has a single, well-defined responsibility
2. **Context-Driven Decision Making**: All routing decisions consider multi-dimensional context
3. **Adaptive Learning**: System learns and improves from experience
4. **Resource Awareness**: Buffer and energy constraints guide behavior
5. **Distributed Intelligence**: Each node makes autonomous decisions

---

## Core Components

### 1. ContextAwareRLRouter

**File**: `src/routing/contextAware/ContextAwareRLRouter.java`

**Responsibilities:**

- Message creation, reception, and forwarding
- Connection event handling (connection up/down)
- Buffer management and message dropping
- Integration of all sub-components

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `changedConnection(Connection con)` | Handles connection state changes (up/down) |
| `createNewMessage(Message m)` | Creates new message with copy control |
| `update()` | Main routing loop - tries to forward messages |
| `makeRoomForMessage(int size)` | Drops low-priority messages to free buffer |
| `updateQValueOnConUp()` | Updates Q-Table when encountering a node |

---

### 2. Encountered Node Set (ENS)

**Files:**

- `EncounteredNodeSet.java` - Collection of all encountered nodes
- `EncounteredNode.java` - Single encounter record
- `ConnectionDuration.java` - Duration tracking

**Purpose:**
Track encounter history to calculate:

- **Popularity**: Number of unique nodes encountered
- **Tie Strength**: Duration and frequency of encounters
- **Network Density**: Local connectivity estimate

**Data Tracked per Encounter:**

```java
class EncounteredNode {
    String nodeId;              // Encountered node ID
    int encounterCount;         // Number of times encountered
    double lastEncounterTime;   // Timestamp of last encounter
    double totalDuration;       // Cumulative connection time
    List<ConnectionDuration> durations;  // Individual encounter durations
}
```

**Network Density Calculation:**

```java
density = encounterCount / simulationTimeElapsed
```

---

### 3. CRIPS Context Evaluator

**Files:**

- `CripsContextAware.java` - Physical + Social context
- `CripsContextMsg.java` - Message context
- `fcl/ContextAware.fcl` - CRIPS evaluation rules

**Purpose:**
Evaluate node suitability as a relay using CRIPS.

---

### 4. Q-Learning Agent

**Files:**

- `Qtable.java` - Q-value storage
- `QTableUpdateStrategy.java` - Update logic

**Purpose:**
Learn optimal next-hop selection through reinforcement learning.

**Q-Table Structure:**

```
Q-Table: Map<String destination, Map<String nextHop, Double qValue>>
```

**Example:**

```
Q("node10", "node5") = 0.85   → High Q-value, prefer node5 for dest node10
Q("node10", "node7") = 0.32   → Low Q-value, avoid node7 for dest node10
```

---

### 5. Buffer Manager

**Files:**

- `MessageListTable.java` - Priority tracking
- `MessagePriority.java` - Priority definitions

**Purpose:**
Manage limited buffer space by prioritizing important messages.

**Priority Levels (CRIPS Output):**

- **Urgent** (score > 0.7): Critical messages, never drop
- **Normal** (0.3 - 0.7): Standard priority
- **Low** (< 0.3): Droppable when buffer is full

---

### 6. Copy Controller

**File:** `NetworkDensityCalculator.java`

**Purpose:**
Adaptively determine number of message copies based on network density.

**Algorithm:**

```java
if (density > HIGH_THRESHOLD) {
    copies = MIN_COPIES;  // Dense network, spray conservatively
} else if (density < LOW_THRESHOLD) {
    copies = MAX_COPIES;  // Sparse network, spray extensively
} else {
    copies = interpolate(density);  // Linear scaling
}
```

---

## Context Evaluation (CRIPS)

### CRIPS Evaluation Framework

CARL-DTN uses **FCL (Fuzzy Control Language)** files for CRIPS evaluation.

### Physical + Social Context (FLC1)

**Inputs:**

1. **Battery Level** (0-100%)
   - Linguistic values: `Low`, `Medium`, `High`
2. **Buffer Utilization** (0-100%)
   - Linguistic values: `Empty`, `Normal`, `Full`
3. **Popularity** (0-1, normalized)
   - Linguistic values: `Unpopular`, `Average`, `Popular`
4. **Tie Strength** (0-1, normalized)
   - Linguistic values: `Weak`, `Moderate`, `Strong`

**Output:**

- **Transfer Opportunity** (0-1)
  - Linguistic values: `Bad`, `Moderate`, `Good`, `Perfect`

**Example CRIPS Rules:**

```fcl
RULE 1: IF battery IS High AND buffer IS NOT Full AND popularity IS Popular
        THEN transferOpportunity IS Perfect

RULE 2: IF battery IS Low OR buffer IS Full
        THEN transferOpportunity IS Bad

RULE 3: IF tieStrength IS Strong AND battery IS Medium
        THEN transferOpportunity IS Good
```

### Message Context (FLC2)

**Inputs:**

1. **TTL Remaining** (0-100%, normalized)
   - Linguistic values: `Expired`, `Low`, `Medium`, `High`
2. **Hop Count** (normalized)
   - Linguistic values: `Few`, `Normal`, `Many`

**Output:**

- **Message Priority** (0-1)
  - Linguistic values: `Low`, `Normal`, `Urgent`

**Example CRIPS Rules:**

```fcl
RULE 1: IF ttlRemaining IS Low AND hopCount IS Few
        THEN priority IS Urgent

RULE 2: IF ttlRemaining IS High AND hopCount IS Many
        THEN priority IS Low

RULE 3: IF ttlRemaining IS Medium
        THEN priority IS Normal
```

---

## Q-Learning Implementation

### Q-Learning Fundamentals

**Goal:** Learn the value `Q(destination, next_hop)` representing the expected utility of forwarding a message destined for `destination` to node `next_hop`.

**Update Equation:**

```
Q(dest, neighbor) ← Q(dest, neighbor) + α × [reward - Q(dest, neighbor)]
```

Where:

- `α` (alpha): Learning rate (0 < α ≤ 1) - Default: 0.3
- `reward`: Transfer Opportunity score from CRIPS (0-1)

### When Q-Values are Updated

#### 1. Connection UP Event

When two nodes meet:

```java
void handleConnectionUp(DTNHost neighbor) {
    // Evaluate context
    double reward = evaluateTransferOpportunity(neighbor);
    
    // Update Q-values for all messages
    for (Message m : getMessages()) {
        String dest = m.getTo().toString();
        qtable.updateQValue(dest, neighbor.toString(), reward, alpha);
    }
}
```

#### 2. Message Transfer Success

When a message is successfully forwarded:

```java
void transferSuccessful(Message m, DTNHost to) {
    // Positive reinforcement
    double bonus = 0.1;
    qtable.updateQValue(m.getTo().toString(), to.toString(), bonus, alpha);
}
```

#### 3. Connection DOWN Event (Decay)

When a connection is lost without transfer:

```java
void handleConnectionDown(DTNHost neighbor) {
    // Decay Q-values slightly (negative feedback)
    double penalty = -0.05;
    qtable.decayQValues(neighbor.toString(), penalty);
}
```

### Q-Table Synchronization

When two nodes meet, they **share** their Q-Tables:

```java
void synchronizeQTable(Qtable neighborQTable) {
    for (String dest : neighborQTable.getAllDestinations()) {
        for (String nextHop : neighborQTable.getNextHops(dest)) {
            double neighborQ = neighborQTable.getQValue(dest, nextHop);
            double myQ = this.qtable.getQValue(dest, nextHop);
            
            // Average the Q-values (collaborative learning)
            double newQ = (neighborQ + myQ) / 2.0;
            this.qtable.setQValue(dest, nextHop, newQ);
        }
    }
}
```

**Benefit:** Accelerates learning by sharing knowledge across the network.

---

## Message Flow & Lifecycle

### 1. Message Creation

```
User/App → createNewMessage(Message m)
    ↓
Evaluate Network Density (via ENS)
    ↓
Determine Copy Count (CopyController)
    ↓
Insert into Buffer (BufferManager)
    ↓
Assign CRIPS Priority (CRIPSContextMsg)
```

### 2. Message Forwarding Decision

```
Node Encounter Event
    ↓
Evaluate Transfer Opportunity (CRIPSContextAware)
    ↓
Update Q-Table (QLearningAgent)
    ↓
For each message in buffer:
    ↓
    Check if neighbor is eligible candidate
    ↓
    Retrieve Q(destination, neighbor)
    ↓
    Rank all neighbors by Q-value
    ↓
    Select best neighbor
    ↓
    Attempt Transfer
```

### 3. Message Reception

```
Receive Message from Neighbor
    ↓
Check if I am the destination → DELIVER
    ↓
Check Buffer Space
    ↓
If full → Drop Lowest Priority Message (makeRoomForMessage)
    ↓
Insert Message into Buffer
    ↓
Assign CRIPS Priority
```

### 4. Message Dropping

```
Buffer Full + Cannot Drop Any Message
    ↓
Sort messages by CRIPS priority (ascending)
    ↓
Drop lowest priority message (not being sent)
    ↓
Repeat until sufficient space or no droppable messages
```

---

## Algorithms & Pseudocode

### Algorithm 1: Next-Hop Selection

```python
def selectNextHop(message, availableNeighbors):
    destination = message.getDestination()
    candidates = []
    
    for neighbor in availableNeighbors:
        # Filter ineligible nodes
        if neighbor == message.getSource():
            continue
        if not neighbor.hasBufferSpace():
            continue
        
        # Get Q-value
        qValue = qtable.get(destination, neighbor)
        candidates.append((neighbor, qValue))
    
    if len(candidates) == 0:
        return None
    
    # Sort by Q-value (descending)
    candidates.sort(key=lambda x: x[1], reverse=True)
    
    # Return best candidate
    return candidates[0][0]
```

### Algorithm 2: Adaptive Copy Control

```python
def calculateCopies(encounterCount, simulationTime):
    density = encounterCount / simulationTime
    
    MIN_COPIES = 3
    MAX_COPIES = 15
    LOW_DENSITY_THRESHOLD = 0.01
    HIGH_DENSITY_THRESHOLD = 0.1
    
    if density < LOW_DENSITY_THRESHOLD:
        return MAX_COPIES
    elif density > HIGH_DENSITY_THRESHOLD:
        return MIN_COPIES
    else:
        # Linear interpolation
        range = HIGH_DENSITY_THRESHOLD - LOW_DENSITY_THRESHOLD
        offset = density - LOW_DENSITY_THRESHOLD
        ratio = offset / range
        copies = MAX_COPIES - ratio * (MAX_COPIES - MIN_COPIES)
        return int(copies)
```

### Algorithm 3: Buffer Management

```python
def makeRoomForMessage(requiredSize):
    freeSpace = bufferSize - currentBufferUsage
    
    if freeSpace >= requiredSize:
        return True
    
    # Get all messages sorted by priority (ascending)
    messages = sortByCripsPriority(getAllMessages())
    
    for msg in messages:
        if msg.isBeingTransferred():
            continue  # Cannot drop
        
        dropMessage(msg)
        freeSpace += msg.getSize()
        
        if freeSpace >= requiredSize:
            return True
    
    return False  # Cannot free enough space
```

---

## Data Structures

### Q-Table

```java
class Qtable {
    // Q(destination, nextHop) → qValue
    Map<String, Map<String, Double>> qTable;
    
    double getQValue(String dest, String nextHop);
    void updateQValue(String dest, String nextHop, double reward, double alpha);
    void decayQValues(String nextHop, double penalty);
    Set<String> getAllDestinations();
}
```

### ENS (Encountered Node Set)

```java
class EncounteredNodeSet {
    Map<String, EncounteredNode> encounters;
    
    void addEncounter(String nodeId, double timestamp);
    void updateEncounter(String nodeId, double duration);
    int getEncounterCount();
    double getNetworkDensity(double currentTime);
    double getPopularity();  // Unique nodes encountered
    double getTieStrength(String nodeId);
}
```

### Message List Table

```java
class MessageListTable {
    Map<String, Double> messagePriorities;  // messageId → CRIPS priority
    
    void addMessage(String messageId, double priority);
    double getPriority(String messageId);
    void updatePriority(String messageId, double newPriority);
}
```

---

## Configuration & Parameters

### Router Settings

```properties
# Router class
Group.router = ContextAwareRLRouter

# Q-Learning parameters
QLearning.alpha = 0.3              # Learning rate
QLearning.gamma = 0.9              # Discount factor (not used in current impl)

# Copy control
CopyControl.minCopies = 3
CopyControl.maxCopies = 15
CopyControl.lowDensityThreshold = 0.01
CopyControl.highDensityThreshold = 0.1

# CRIPS evaluation files
CRIPS.contextAwareFile = fcl/ContextAware.fcl
CRIPS.contextMsgFile= fcl/ContextMsg.fcl
```

### Node Group Settings

```properties
Group.nrofHosts = 40               # Number of nodes
Group.bufferSize = 15M             # 15 MB buffer
Group.transmitSpeed = 250k         # 250 kbps
Group.transmitRange = 10           # 10 meters
Group.msgTtl = 300                 # 300 minutes TTL
```

### Simulation Settings

```properties
Scenario.endTime = 43200           # 12 hours
Scenario.updateInterval = 0.1      # 100ms time step
Scenario.nrofHostGroups = 1
```

---

## Performance Considerations

### Time Complexity

| Operation | Complexity |
|-----------|-----------|
| Q-Value Lookup | O(1) (HashMap) |
| Next-Hop Selection | O(n) where n = neighbor count |
| CRIPS Evaluation | O(1) (rule-based) |
| Buffer Management | O(m log m) where m = messages in buffer |

### Space Complexity

- **Q-Table**: O(d × n) where d = destinations, n = nodes
- **ENS**: O(e) where e = unique encounters
- **Message Buffer**: O(b) where b = buffer size

### Optimization Strategies

1. **Lazy CRIPS Evaluation**: Only evaluate when decision needed
2. **Q-Table Pruning**: Remove stale entries for unreachable destinations
3. **Incremental Priority Update**: Update only changed messages
4. **Connection Caching**: Cache Transfer Opportunity scores

---

## Extension Points

### Adding New Context Dimensions

1. Create new CRIPS input variables in `.fcl` files
2. Implement context extraction in router
3. Add rules combining new context with existing ones

### Custom Q-Learning Strategies

Extend `QTableUpdateStrategy.java`:

```java
public class CustomQTableUpdate extends QTableUpdateStrategy {
    @Override
    public void updateQValue(String dest, String nextHop, double reward) {
        // Custom update logic
    }
}
```

### Alternative CRIPS Evaluation

Replace FCL files in `/fcl` directory with custom rules.

---

## References

- **Q-Learning**: Watkins, C. J., & Dayan, P. (1992). Q-learning. Machine learning, 8(3-4), 279-292.
- **CRIPS Framework**: Zadeh, L. A. (1965). Fuzzy sets. Information and control, 8(3), 338-353.
- **DTN Architecture**: RFC 4838 - Delay-Tolerant Networking Architecture

---

**For implementation questions or contributions, see the main [README.md](README.md).**
