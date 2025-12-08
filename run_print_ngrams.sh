#!/bin/bash
cd /mnt/c/Users/nuhin/git/barter-enginer

if [ ! -f gson-2.10.1.jar ]; then
    echo "Downloading Gson..."
    wget -q https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
fi

echo "Compiling..."
javac -cp ".:gson-2.10.1.jar" -d . src/data/*.java 2>&1

if [ $? -eq 0 ]; then
    echo "Running PrintNGrams..."
    java -cp ".:gson-2.10.1.jar" data.PrintNGrams 2>&1
else
    echo "Compilation failed!"
fi










