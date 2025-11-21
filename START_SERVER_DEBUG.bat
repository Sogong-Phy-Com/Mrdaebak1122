@echo off
echo ====================================
echo 백엔드 서버 시작 (디버그 모드)
echo ====================================
echo.
echo 이 스크립트는 상세한 로그를 표시합니다.
echo.

cd server-java
if not exist "data" mkdir data

echo [1/3] Java 버전 확인...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java가 설치되지 않았습니다!
    echo Java 17 이상을 설치해주세요.
    pause
    exit /b 1
)
echo.

echo [2/3] Maven 버전 확인...
mvn -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven이 설치되지 않았습니다!
    echo Maven을 설치해주세요.
    pause
    exit /b 1
)
echo.

echo [3/3] Spring Boot 서버 시작...
echo.
echo ====================================
echo 서버 로그 (오류 발생 시 여기에 표시됨)
echo ====================================
echo.

mvn clean spring-boot:run

echo.
echo ====================================
echo 서버가 종료되었습니다.
echo ====================================
echo.
echo 위의 오류 메시지를 확인하세요.
echo.
pause

