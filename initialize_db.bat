@echo off
REM Initialize the database schema
cd /d %~dp0

echo Initializing database schema...
echo.

REM Compile the initialization program
javac -encoding UTF-8 -cp "lib\sqlite-jdbc.jar;SupermarketServer\src;Shared\src" SupermarketServer\src\InitializeDatabase.java
if errorlevel 1 (
  echo [ERROR] Compilation failed
  exit /b 1
)

REM Run the initialization
java -cp "lib\sqlite-jdbc.jar;SupermarketServer\src;Shared\src" InitializeDatabase
if errorlevel 1 (
  echo [ERROR] Initialization failed
  exit /b 1
)

echo.
echo [OK] Done! You can now run the server.
pause

