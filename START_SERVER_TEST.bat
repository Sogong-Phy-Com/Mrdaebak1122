@echo off
echo ====================================
echo 백엔드 서버 테스트 시작
echo ====================================
echo.

cd server-java
if not exist "data" mkdir data

echo [INFO] 서버를 시작합니다...
echo [INFO] 오류가 발생하면 이 창에 표시됩니다.
echo [INFO] 서버가 시작되면 "Started DinnerServiceApplication" 메시지가 보입니다.
echo.
echo ====================================
echo.

mvn spring-boot:run

echo.
echo ====================================
echo 서버가 종료되었습니다.
echo ====================================
echo.
echo 아무 키나 누르면 창이 닫힙니다...
pause >nul

