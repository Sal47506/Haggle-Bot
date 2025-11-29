cd C:\Users\nuhin\git\barter-enginer

Write-Host "Compiling InteractiveNegotiation..."
javac -cp ".;gson-2.10.1.jar" -source 8 -target 8 -d . src/InteractiveNegotiation.java src/agents/*.java src/dialogue/*.java src/data/*.java src/models/*.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running InteractiveNegotiation..."
    Write-Host ""
    java -cp ".;gson-2.10.1.jar" InteractiveNegotiation
} else {
    Write-Host "Compilation failed!"
    exit 1
}


