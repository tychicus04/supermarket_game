@echo off
REM Add sample data to Supermarket Game database
REM This script works with Docker container

echo ============================================================
echo SUPERMARKET GAME - ADD SAMPLE DATA
echo ============================================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop first.
    pause
    exit /b 1
)

echo Choose method to add data:
echo.
echo 1. Use Python script (Recommended)
echo 2. Use SQL script via Docker
echo 3. Add data directly in Docker container
echo 4. Exit
echo.
set /p choice="Enter your choice (1-4): "

if "%choice%"=="1" goto python_method
if "%choice%"=="2" goto sql_method
if "%choice%"=="3" goto docker_method
if "%choice%"=="4" goto end

:python_method
echo.
echo Running Python script...
echo.
python scripts\add_sample_data.py
if errorlevel 1 (
    echo.
    echo ERROR: Failed to run Python script
    echo Make sure Python 3 is installed
    pause
    exit /b 1
)
goto success

:sql_method
echo.
echo Running SQL script via Docker...
echo.
docker exec -i supermarket-game-server sqlite3 /app/data/supermarket_game.db < scripts\sample_data.sql
if errorlevel 1 (
    echo.
    echo ERROR: Failed to execute SQL script
    echo Make sure Docker container is running
    pause
    exit /b 1
)
goto success

:docker_method
echo.
echo Opening SQLite shell in Docker container...
echo You can manually run SQL commands.
echo.
echo Example commands:
echo   INSERT INTO users (username, password_hash) VALUES ('testuser', '482c811da5d5b4bc6d497ffa98491e38');
echo   INSERT INTO scores (username, score) VALUES ('testuser', 1500);
echo   SELECT * FROM users;
echo   .quit  (to exit)
echo.
pause
docker exec -it supermarket-game-server sqlite3 /app/data/supermarket_game.db
goto end

:success
echo.
echo ============================================================
echo SUCCESS! Sample data has been added.
echo ============================================================
echo.
echo You can now login with:
echo   Username: alice, bob, charlie, diana, eve, etc.
echo   Password: password123
echo.
echo To view the data, run:
echo   python scripts\query_db.py
echo.

:end
pause

