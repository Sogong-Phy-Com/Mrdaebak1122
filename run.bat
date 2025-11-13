@echo off
echo ====================================
echo 미스터 대박 디너 서비스 시작
echo ====================================
echo.

REM Java와 Maven이 설치되어 있는지 확인
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [오류] Java가 설치되어 있지 않습니다.
    echo Java 17 이상을 설치해주세요.
    pause
    exit /b 1
)

where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [오류] Maven이 설치되어 있지 않습니다.
    echo Maven을 설치하거나 Maven Wrapper를 사용하세요.
    pause
    exit /b 1
)

echo [1/2] 백엔드 서버 시작 중...
cd server-java
if not exist "data" mkdir data
start "Mr. DaeBak Backend" cmd /k "mvn spring-boot:run"
cd ..

timeout /t 5 /nobreak >nul

echo [2/2] 프론트엔드 시작 중...
cd client
if not exist "node_modules" (
    echo npm 패키지 설치 중...
    call npm install --legacy-peer-deps
    if errorlevel 1 (
        echo [오류] npm 설치에 실패했습니다.
        pause
        exit /b 1
    )
)
start "Mr. DaeBak Frontend" cmd /k "npm start"
cd ..

echo.
echo ====================================
echo 서비스가 시작되었습니다!
echo ====================================
echo 백엔드: http://localhost:5000
echo 프론트엔드: http://localhost:3000
echo.
echo 서비스를 종료하려면 각 창을 닫으세요.
echo ====================================
pause

