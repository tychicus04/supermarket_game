# Test Database Connection
# PowerShell script to build and run database test

Write-Host "=== Building TestDatabase ===" -ForegroundColor Cyan

# Create output directory
New-Item -ItemType Directory -Force -Path "test_db\build" | Out-Null

# Download SQLite JDBC if not exists
if (-not (Test-Path "lib\sqlite-jdbc.jar")) {
    Write-Host "Downloading SQLite JDBC driver..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path "lib" | Out-Null
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar" -OutFile "lib\sqlite-jdbc.jar"
    Write-Host "✅ Downloaded SQLite JDBC" -ForegroundColor Green
}

# Compile
javac -cp "lib\sqlite-jdbc.jar" -d "test_db\build" "test_db\TestDatabase.java"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Compilation successful" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== Running Test ===" -ForegroundColor Cyan
    java -cp "test_db\build;lib\sqlite-jdbc.jar" TestDatabase
} else {
    Write-Host "❌ Compilation failed" -ForegroundColor Red
    exit 1
}
