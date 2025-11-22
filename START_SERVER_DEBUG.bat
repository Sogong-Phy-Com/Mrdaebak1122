@echo off
chcp 65001 >nul
echo ====================================
echo Backend Server Start (Debug Mode)
echo ====================================
echo.
echo This script shows detailed logs.
echo.

cd /d "%~dp0server-java"
if not exist "data" mkdir data

echo [1/3] Checking Java version...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed!
    echo Please install Java 17 or higher.
    pause
    exit /b 1
)
echo.

echo [2/3] Checking Maven version...
mvn -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven is not installed!
    echo Please install Maven.
    pause
    exit /b 1
)
echo.

echo [3/3] Starting Spring Boot server...
echo.
echo ====================================
echo Server Logs (Errors will appear here)
echo ====================================
echo.

mvn clean spring-boot:run

echo.
echo ====================================
echo Server stopped.
echo ====================================
echo.
echo Check the error messages above.
echo.
pause
