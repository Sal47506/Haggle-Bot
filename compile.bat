@echo off
echo Compiling Barter Engine...

if not exist "bin" mkdir bin

javac -cp "lib/*" -source 8 -target 8 -d bin src/data/*.java src/models/*.java src/dialogue/*.java src/agents/*.java src/*.java

if %errorlevel% == 0 (
    echo.
    echo Compilation successful!
    echo.
    echo To run interactive negotiation:
    echo   run_interactive_maven.bat
) else (
    echo.
    echo Compilation failed!
)

