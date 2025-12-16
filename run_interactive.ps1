Set-Location $PSScriptRoot

if (-not (Test-Path "gson-2.10.1.jar")) {
    Write-Host "Downloading Gson..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -OutFile "gson-2.10.1.jar"
}

Write-Host "Compiling..."
$outDir = "target\\classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$cp = "$outDir;gson-2.10.1.jar"
javac -cp $cp -source 8 -target 8 -d $outDir src/data/*.java src/models/*.java src/dialogue/*.java src/agents/*.java src/InteractiveNegotiation.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running Interactive Negotiation..."
    Write-Host ""
    java -cp $cp InteractiveNegotiation
} else {
    Write-Host "Compilation failed!"
    exit 1
}
