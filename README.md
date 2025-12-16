# HaggleBot (Final Project)

HaggleBot is a Java command-line negotiation simulator. A human provides seller inputs (item name and prices), and an AI buyer negotiates using a small **Q-learning** policy and generates dialogue using either:

- **Markov n-gram generation** (fast, sometimes noisy)
- **TF-IDF + cosine similarity retrieval** (more on-topic, still imperfect)

## Requirements

- **Java (JDK 8+)**
- **Maven 3.6+** (optional, recommended)
- Dataset file: `data/craigslist_bargains/train.json`

## Run (Windows / Linux / macOS)

### Option 1: Run with Maven (recommended)

```bash
cd barter-enginer
mvn -q compile exec:java -Dexec.mainClass=InteractiveNegotiation
```

## Run on Windows (PowerShell)

### Option A: Use the provided script (recommended)

```powershell
.\run_interactive.ps1
```

### Option B: Run without scripts

This option compiles with `javac` and runs with `java`.

```powershell
cd C:\path\to\barter-enginer

# Download Gson (only if gson-2.10.1.jar is not already present)
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -OutFile "gson-2.10.1.jar"

# Compile
$outDir = "target\\classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$cp = "$outDir;gson-2.10.1.jar"
javac -cp $cp -source 8 -target 8 -d $outDir src/data/*.java src/models/*.java src/dialogue/*.java src/agents/*.java src/InteractiveNegotiation.java

# Run
java -cp $cp InteractiveNegotiation
```

## Run on Linux/macOS (Terminal)

### Run on Linux/macOS (no scripts)

```bash
cd barter-enginer

# Download Gson (only if gson-2.10.1.jar is not already present)
curl -L -o gson-2.10.1.jar "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"

# Compile
mkdir -p target/classes
javac -cp "target/classes:gson-2.10.1.jar" -source 8 -target 8 -d target/classes \
  src/data/*.java src/models/*.java src/dialogue/*.java src/agents/*.java src/InteractiveNegotiation.java

# Run
java -cp "target/classes:gson-2.10.1.jar" InteractiveNegotiation
```

## Using the program

At startup, the program prompts for:

1. **Item name** (example: `soda`)
2. **Seller asking price**
3. **Buyer reservation price** (maximum the buyer will pay)
4. **Buyer target price** (buyer’s ideal starting point)
5. **Dialogue generator selection** (Markov vs TF-IDF)

The negotiation continues until a deal is accepted, the buyer walks away, the round limit is reached, or the seller types `quit` / `exit`.
