@echo off
REM 창이 닫히지 않도록 설정
setlocal enabledelayedexpansion

echo ====================================
echo 백엔드 서버 시작
echo ====================================
echo.
echo 백엔드 서버를 시작합니다...
echo 서버가 시작되면 "Started DinnerServiceApplication" 메시지가 보입니다.
echo.
echo 이 창을 닫지 마세요!
echo.

cd /d "%~dp0server-java"
if not exist "data" mkdir data

echo.
echo [INFO] Maven 빌드 및 서버 시작 중...
echo [INFO] 오류가 발생하면 이 창에 표시됩니다.
echo [INFO] 서버가 시작되면 이 창에 로그가 표시됩니다.
echo.
echo ====================================
echo.

REM Maven 실행 및 오류 처리
call mvn clean spring-boot:run
set EXIT_CODE=%ERRORLEVEL%

echo.
echo ====================================

if %EXIT_CODE% NEQ 0 (
    echo [ERROR] 서버 시작 실패! (종료 코드: %EXIT_CODE%)
    echo ====================================
    echo.
    echo 가능한 원인:
    echo 1. Java가 설치되지 않았거나 버전이 맞지 않음 (Java 17 이상 필요)
    echo 2. Maven이 설치되지 않음
    echo 3. 포트 5000이 이미 사용 중
    echo 4. 데이터베이스 파일 권한 문제
    echo 5. 컴파일 오류
    echo.
    echo 해결 방법:
    echo 1. TEST_COMPILE.bat를 실행하여 컴파일 오류 확인
    echo 2. START_SERVER_DEBUG.bat를 실행하여 상세 로그 확인
    echo 3. 위의 오류 메시지를 확인
    echo.
) else (
    echo [INFO] 서버가 정상적으로 종료되었습니다.
    echo ====================================
    echo.
)

echo.
echo 아무 키나 누르면 창이 닫힙니다...
pause >nul

