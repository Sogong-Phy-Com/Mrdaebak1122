@echo off
chcp 65001 >nul
REM UTF-8 인코딩 설정

echo ====================================
echo 백엔드 서버 시작 (로그 파일 저장)
echo ====================================
echo.
echo 백엔드 서버를 시작합니다...
echo 서버 로그가 server-java\server.log 파일에 저장됩니다.
echo.

cd server-java
if not exist "data" mkdir data

echo.
echo [INFO] Maven 빌드 및 서버 시작 중...
echo [INFO] 로그 파일: %CD%\server.log
echo.

REM 로그를 파일로 저장
echo [INFO] 로그를 server.log 파일에 저장 중...
mvn clean spring-boot:run > server.log 2>&1
set EXIT_CODE=%ERRORLEVEL%

REM 로그 파일이 생성되었는지 확인
if exist "server.log" (
    echo [OK] 로그 파일이 생성되었습니다: server.log
    echo.
    echo 로그의 마지막 부분:
    echo ====================================
    powershell -Command "if (Test-Path server.log) { Get-Content server.log -Tail 30 }"
    echo ====================================
) else (
    echo [WARNING] 로그 파일이 생성되지 않았습니다.
)

echo.
echo ====================================
echo 서버가 종료되었습니다.
echo ====================================
echo.
echo 로그 파일을 확인하세요: server-java\server.log
echo.

if %EXIT_CODE% NEQ 0 (
    echo [ERROR] 서버 시작 실패! (종료 코드: %EXIT_CODE%)
    echo.
    echo server.log 파일의 마지막 부분을 확인하세요:
    echo.
    powershell -Command "Get-Content server.log -Tail 50"
    echo.
) else (
    echo [INFO] 서버가 정상적으로 종료되었습니다.
    echo.
)

echo.
echo 아무 키나 누르면 창이 닫힙니다...
pause >nul

