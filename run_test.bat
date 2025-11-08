@echo off
REM Compile and run the TestDatabase with sqlite jdbc jar on classpath
cd /d %~dp0
echo Running TestDatabase...
javac -cp lib\sqlite-jdbc.jar test_db\TestDatabase.java
if errorlevel 1 (
  echo Compile failed
  exit /b 1
)
java -cp lib\sqlite-jdbc.jar;test_db TestDatabase
exit /b 0
