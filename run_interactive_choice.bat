@echo off
REM Batch script to compile and run InteractiveNegotiationWithChoice

echo Compiling InteractiveNegotiationWithChoice...

REM Compile with all necessary source files
javac -encoding UTF-8 -source 8 -target 8 -cp ".;gson-2.10.1.jar" src/data/*.java src/models/*.java src/dialogue/*.java src/agents/*.java src/InteractiveNegotiationWithChoice.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b 1
)

echo Compilation successful!
echo.
echo Running InteractiveNegotiationWithChoice...
echo.

REM Run the interactive negotiation
java -cp ".;src;gson-2.10.1.jar" InteractiveNegotiationWithChoice

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Execution failed!
    exit /b 1
)
