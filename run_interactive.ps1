cd C:\Users\nuhin\git\barter-enginer

if (-not (Test-Path "gson-2.10.1.jar")) {
    Write-Host "Downloading Gson..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -OutFile "gson-2.10.1.jar"
}

Write-Host "Compiling with Q-Learning..."
javac -cp ".;gson-2.10.1.jar" -source 8 -target 8 -d . src/data/*.java src/models/*.java src/dialogue/*.java src/agents/*.java src/InteractiveNegotiation.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running Interactive Negotiation with Q-Learning..."
    Write-Host ""
    java -cp ".;gson-2.10.1.jar" InteractiveNegotiation
} else {
    Write-Host "Compilation failed!"
    exit 1
}
