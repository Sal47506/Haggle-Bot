cd C:\Users\nuhin\git\barter-enginer

if (-not (Test-Path "gson-2.10.1.jar")) {
    Write-Host "Downloading Gson..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -OutFile "gson-2.10.1.jar"
}

# Note: DJL sentence-transformers models are downloaded at runtime from HuggingFace Hub
# No additional jars needed beyond what's in lib/

Write-Host "Compiling with Q-Learning..."
$cp = ".;gson-2.10.1.jar;lib/api-0.29.0.jar;lib/model-zoo-0.29.0.jar;lib/tokenizers-0.29.0.jar;lib/pytorch-engine-0.29.0.jar;lib/slf4j-api-2.0.9.jar;lib/slf4j-simple-2.0.9.jar"
javac -cp $cp -source 8 -target 8 -d . src/data/*.java src/models/*.java src/dialogue/*.java src/agents/*.java src/InteractiveNegotiation.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running Interactive Negotiation with Q-Learning..."
    Write-Host ""
    java -cp $cp InteractiveNegotiation
} else {
    Write-Host "Compilation failed!"
    exit 1
}
