@echo off
chcp 65001 >nul
echo ====================================
echo 서버 로그 확인
echo ====================================
echo.

if not exist "server-java\server.log" (
    echo [ERROR] 로그 파일이 없습니다.
    echo 서버를 먼저 실행해주세요.
    echo.
    pause
    exit /b 1
)

echo 서버 로그의 마지막 100줄을 표시합니다:
echo.
echo ====================================
echo.

powershell -Command "Get-Content server-java\server.log -Tail 100 -Encoding UTF8"

echo.
echo ====================================
echo.
echo 전체 로그를 보려면:
echo notepad server-java\server.log
echo.
pause

