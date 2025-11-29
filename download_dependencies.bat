@echo off
echo Downloading Maven dependencies...

if not exist "lib" mkdir lib

cd lib

echo Downloading Gson...
if not exist "gson-2.10.1.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'gson-2.10.1.jar'"
)

echo Downloading DJL API...
if not exist "api-0.29.0.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ai/djl/api/0.29.0/api-0.29.0.jar' -OutFile 'api-0.29.0.jar'"
)

echo Downloading DJL Tokenizers...
if not exist "tokenizers-0.29.0.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ai/djl/huggingface/tokenizers/0.29.0/tokenizers-0.29.0.jar' -OutFile 'tokenizers-0.29.0.jar'"
)

echo Downloading DJL PyTorch Engine...
if not exist "pytorch-engine-0.29.0.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ai/djl/pytorch/pytorch-engine/0.29.0/pytorch-engine-0.29.0.jar' -OutFile 'pytorch-engine-0.29.0.jar'"
)

echo Downloading DJL Model Zoo...
if not exist "model-zoo-0.29.0.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ai/djl/model-zoo/0.29.0/model-zoo-0.29.0.jar' -OutFile 'model-zoo-0.29.0.jar'"
)

echo Downloading SLF4J API...
if not exist "slf4j-api-2.0.9.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar' -OutFile 'slf4j-api-2.0.9.jar'"
)

echo Downloading SLF4J Simple...
if not exist "slf4j-simple-2.0.9.jar" (
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar' -OutFile 'slf4j-simple-2.0.9.jar'"
)

cd ..

echo.
echo Dependencies downloaded to lib/ folder
echo.
echo To compile, use:
echo   javac -cp "lib/*" -d bin src/**/*.java
echo.
echo To run:
echo   java -cp "bin;lib/*" InteractiveNegotiation

