@echo off
echo ====================================
echo 컴파일 테스트
echo ====================================
echo.
echo 프로젝트를 컴파일하여 오류를 확인합니다.
echo.

cd server-java
echo [1/2] Maven 의존성 다운로드 중...
call mvn dependency:resolve
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 의존성 다운로드 실패!
    pause
    exit /b 1
)
echo [OK] 의존성 다운로드 완료
echo.

echo [2/2] 프로젝트 컴파일 중...
call mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ====================================
    echo [ERROR] 컴파일 실패!
    echo ====================================
    echo.
    echo 위의 오류 메시지를 확인하세요.
    echo.
    pause
    exit /b 1
)
echo.
echo ====================================
echo [OK] 컴파일 성공!
echo ====================================
echo.
echo 이제 START_SERVER.bat를 실행할 수 있습니다.
echo.
pause

