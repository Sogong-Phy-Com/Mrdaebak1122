@echo off
REM Change to batch file directory
cd /d "%~dp0"

echo ====================================
echo Mr. DaeBak Dinner Service Starting
echo ====================================
echo Current directory: %CD%
echo.

REM Check working directories
if not exist "server-java" (
    echo [ERROR] server-java folder not found.
    echo Current location: %CD%
    echo.
    pause
    exit /b 1
)

if not exist "client" (
    echo [ERROR] client folder not found.
    echo Current location: %CD%
    echo.
    pause
    exit /b 1
)

REM Check Java
echo Checking Java installation...
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed.
    echo Please install Java 17 or higher.
    echo.
    pause
    exit /b 1
)
echo [OK] Java check complete

REM Check Maven
echo Checking Maven installation...
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven is not installed.
    echo Please install Maven or use Maven Wrapper.
    echo.
    pause
    exit /b 1
)
echo [OK] Maven check complete

REM Check Node.js
echo Checking Node.js installation...
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed.
    echo Please install Node.js.
    echo.
    pause
    exit /b 1
)
echo [OK] Node.js check complete
echo.

REM Check Python for STT service
echo Checking Python installation...
where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Python 3 is not installed.
    echo Please install Python 3.10 or later for the Whisper STT service.
    echo.
    pause
    exit /b 1
)
echo [OK] Python check complete
echo.

echo [1/4] Starting STT (FastAPI + Whisper) service...
cd stt-service
if not exist "stt-service.log" type nul > stt-service.log
start "Mr. DaeBak STT" cmd /k "python -m uvicorn main:app --host 0.0.0.0 --port 8001"
cd ..
echo STT service log: stt-service\stt-service.log
echo.

echo [2/4] Checking Ollama server...
where ollama >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    curl -s http://localhost:11434/api/version >nul 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo Starting Ollama server window...
        start "Ollama Server" cmd /k "ollama serve"
        timeout /t 5 /nobreak >nul
    ) else (
        echo Ollama server already running.
    )
) else (
    echo [WARNING] Ollama CLI not found. Please install Ollama and run ^"ollama serve^" manually if needed.
)
echo.

echo [3/4] Starting backend server...
cd server-java
if not exist "data" mkdir data
echo Starting Spring Boot application...
start "Mr. DaeBak Backend" cmd /k "mvn clean spring-boot:run"
cd ..

echo [INFO] Waiting for backend server initialization... (15 seconds)
timeout /t 15 /nobreak >nul

echo [4/4] Starting frontend...
cd client
if not exist "node_modules" (
    echo Installing npm packages...
    call npm install --legacy-peer-deps
    if errorlevel 1 (
        echo [ERROR] npm installation failed.
        cd ..
        echo.
        pause
        exit /b 1
    )
)
echo Starting React development server...
start "Mr. DaeBak Frontend" cmd /k "npm start"
cd ..

echo.
echo ====================================
echo Services started successfully!
echo ====================================
echo STT Service : http://localhost:8001/stt/transcribe
echo Ollama API  : http://localhost:11434
echo Backend     : http://localhost:5000
echo Frontend    : http://localhost:3000  (voice order page /voice-order)
echo.
echo Opening browser...
timeout /t 15 /nobreak >nul

REM Open browser
start http://localhost:3000

echo.
echo ====================================
echo Browser opened!
echo ====================================
echo.
echo To stop the services:
echo   - Close the STT / Ollama / backend / frontend windows, or
echo   - Close this window (remaining consoles keep running)
echo.
echo Do not close this window. You can check server status here.
echo ====================================
echo.
pause
