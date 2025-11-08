#!/bin/bash
# Build and run database test

echo "=== Building TestDatabase ==="

# Create output directory
mkdir -p test_db/build

# Compile
javac -cp "lib/sqlite-jdbc.jar" -d test_db/build test_db/TestDatabase.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
    echo ""
    echo "=== Running Test ==="
    java -cp "test_db/build:lib/sqlite-jdbc.jar" TestDatabase
else
    echo "❌ Compilation failed"
    exit 1
fi
