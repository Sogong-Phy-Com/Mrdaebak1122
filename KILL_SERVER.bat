@echo off
echo ====================================
echo 포트 5000 사용 중인 프로세스 종료
echo ====================================
echo.

echo 포트 5000을 사용하는 프로세스를 찾는 중...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5000" ^| findstr "LISTENING"') do (
    echo 프로세스 ID %%a 종료 중...
    taskkill /PID %%a /F >nul 2>&1
    if errorlevel 1 (
        echo 프로세스 %%a 종료 실패 (이미 종료되었거나 권한이 없습니다)
    ) else (
        echo 프로세스 %%a 종료 완료
    )
)

echo.
echo 모든 Java 프로세스 종료 중...
taskkill /F /IM java.exe >nul 2>&1
if errorlevel 1 (
    echo Java 프로세스가 없거나 이미 종료되었습니다.
) else (
    echo Java 프로세스 종료 완료
)

echo.
echo ====================================
echo 완료!
echo ====================================
echo.
echo 이제 START_SERVER_SIMPLE.bat를 실행하세요.
echo.
pause

