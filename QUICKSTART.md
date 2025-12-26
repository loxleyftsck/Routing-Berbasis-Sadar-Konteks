# 🚀 CARL-DTN Quick Start Guide

This guide will help you get CARL-DTN up and running in minutes.

---

## Prerequisites

Before you begin, ensure you have:

- ✅ **Java JDK 6 or higher** installed

  ```bash
  # Check Java version
  java -version
  ```

- ✅ **Git** installed (for cloning the repository)
- ✅ **Terminal/Command Prompt** access

---

## Step 1: Clone the Repository

```bash
git clone https://github.com/loxleyftsck/Routing-Berbasis-Sadar-Konteks.git
cd Routing-Berbasis-Sadar-Konteks
```

---

## Step 2: Compile the Project

### Windows

```cmd
compile.bat
```

### Linux/macOS

```bash
chmod +x compile.bat
./compile.bat
```

**Expected Output:**

```
Compiling...
Done!
```

If you see compilation errors, verify your Java version and `JAVA_HOME` environment variable.

---

## Step 3: Run Your First Simulation

### Option A: GUI Mode (Recommended for First-Time Users)

**Windows:**

```cmd
one.bat src\ContextAwareGroupRL_settings.txt
```

**Linux/macOS:**

```bash
./one.sh src/ContextAwareGroupRL_settings.txt
```

**What you'll see:**

- A graphical window showing the simulation world
- Nodes moving according to the configured movement model
- Messages being created, forwarded, and delivered
- Real-time statistics

**GUI Controls:**

- **Pause/Play**: Control simulation flow
- **Speed**: Adjust simulation speed (1x, 10x, etc.)
- **Zoom**: Mouse wheel to zoom in/out
- **Node Selection**: Click on nodes to view their state

### Option B: Batch Mode (Headless, for Multiple Runs)

Run 5 simulations with different random seeds:

```bash
# Linux/macOS
./one.sh -b 5 src/ContextAwareGroupRL_settings.txt

# Windows
one.bat -b 5 src\ContextAwareGroupRL_settings.txt
```

**Batch mode** is useful for:

- Collecting statistical averages
- Running parameter sweeps
- Automated testing

---

## Step 4: Understanding Results

After the simulation completes, check the `/reports` directory:

```
reports/
├── MessageStatsReport_ContextAwareGroupRL.txt    # Overall statistics
├── DeliveredMessagesReport_ContextAwareGroupRL.txt
├── CreatedMessagesReport_ContextAwareGroupRL.txt
└── ContactTimesReport_ContextAwareGroupRL.txt
```

### Key Metrics in `MessageStatsReport`

```
delivery_prob: 0.7234           # 72.34% of messages delivered
overhead_ratio: 1.456           # 1.456 copies forwarded per delivery
latency_avg: 3456.2            # Average delivery time (seconds)
hopcount_avg: 4.2              # Average hops per delivered message
buffertime_avg: 2134.5         # Average time in buffer (seconds)
```

### Interpreting Results

**Good Performance:**

- **Delivery Probability** > 0.7 (70%)
- **Overhead Ratio** < 2.0
- **Latency** depends on scenario (lower is better)

**Compare with Baselines:**

```bash
# Run Epidemic routing
./one.sh -b 5 src/CARL_Test_Epidemic_settings.txt

# Run PRoPHET routing
./one.sh -b 5 src/CARL_Test_Prophet_settings.txt

# Run Spray and Wait routing
./one.sh -b 5 src/CARL_Test_SprayAndWait_settings.txt
```

---

## Step 5: Exploring Different Scenarios

### Scenario 1: Varying Buffer Sizes

Test how buffer size affects performance:

```bash
# 5MB buffer
./one.sh -b 3 src/ContextAwareGroupRL_5MBsettings.txt

# 10MB buffer
./one.sh -b 3 src/ContextAwareGroupRL_10MBsettings.txt

# 20MB buffer
./one.sh -b 3 src/ContextAwareGroupRL_20MBsettings.txt
```

### Scenario 2: Varying TTL (Time-to-Live)

Test message lifetime impact:

```bash
# TTL = 300 minutes
./one.sh -b 3 src/ContextAwareGroupRLTTL300_settings.txt

# TTL = 420 minutes
./one.sh -b 3 src/ContextAwareGroupRLTTL420_settings.txt
```

### Scenario 3: Different Movement Models

```bash
# Map-Based Movement
./one.sh -b 3 src/ContextAwareGroupRLMBM_settings.txt

# Random Waypoint
./one.sh -b 3 src/ContextAwareGroupRLRWP_settings.txt

# Shortest Path Map-Based
./one.sh -b 3 src/ContextAwareGroupRLSPMB_settings.txt
```

---

## Customizing Simulations

### Editing Configuration Files

Configuration files (`.txt`) use key-value pairs:

```properties
# Example: src/ContextAwareGroupRL_settings.txt

Scenario.name = ContextAwareGroupRL
Scenario.simulateConnections = true
Scenario.updateInterval = 0.1
Scenario.endTime = 43200          # 12 hours in seconds

# Node groups
Scenario.nrofHostGroups = 1
Group.router = ContextAwareRLRouter
Group.bufferSize = 15M            # 15 MB buffer
Group.transmitSpeed = 250k        # 250 kbps
Group.nrofHosts = 40              # 40 nodes
```

### Key Parameters to Experiment With

| Parameter | Description | Typical Values |
|-----------|-------------|----------------|
| `Group.nrofHosts` | Number of nodes | 20, 40, 80, 100 |
| `Group.bufferSize` | Buffer capacity | 5M, 10M, 15M, 20M |
| `Group.msgTtl` | Message TTL (minutes) | 120, 240, 360, 480 |
| `Group.transmitSpeed` | Bandwidth | 125k, 250k, 500k |
| `Scenario.endTime` | Simulation duration | 21600, 43200, 86400 |

---

## Troubleshooting

### Issue: "java: command not found"

**Solution:** Install Java JDK 6+ and add to PATH.

**Verify:**

```bash
java -version
javac -version
```

### Issue: Compilation Errors

**Check:**

1. Java version is 6 or higher
2. All `.jar` files exist in `/lib` directory
3. No file path issues (use forward slashes `/` even on Windows in config files)

**Clean and recompile:**

```bash
# Remove old class files
find . -name "*.class" -type f -delete

# Recompile
./compile.bat
```

### Issue: Simulation Freezes or Crashes

**Possible Causes:**

- Insufficient memory for large simulations
- Too many nodes or messages

**Solution:**
Increase Java heap size:

```bash
# Edit one.sh or one.bat to add:
java -Xmx2048m -cp ...   # Allocates 2GB memory
```

### Issue: No Reports Generated

**Check:**

1. `Report.reportDir` is set in config file
2. Directory has write permissions
3. Reports are enabled in settings:

   ```properties
   Report.nrofReports = 3
   Report.report1 = MessageStatsReport
   Report.report2 = DeliveredMessagesReport
   Report.report3 = ContactTimesReport
   ```

---

## Next Steps

✅ **Run baseline comparisons** to see CARL-DTN advantages  
✅ **Experiment with different scenarios** (buffer, TTL, movement models)  
✅ **Read [ARCHITECTURE.md](ARCHITECTURE.md)** for deep technical understanding  
✅ **Modify Q-learning parameters** (`alpha`, `gamma`) in `QTableUpdateStrategy.java`  
✅ **Customize CRIPS fuzzy rules** in `/src/fcl/*.fcl` files  

---

## Additional Resources

- **Full Documentation**: [README.md](README.md)
- **Technical Architecture**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **ONE Simulator Manual**: [The ONE Documentation](https://akeranen.github.io/the-one/)
- **Configuration Help**: `default_settings.txt` (contains all available settings)

---

**Happy Simulating! 🚀**

If you encounter issues not covered here, please open an issue on GitHub.
