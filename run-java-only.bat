@echo off
echo ====================================
echo 미스터 대박 백엔드 서버 시작
echo ====================================
echo.

cd server-java
if not exist "data" mkdir data
mvn spring-boot:run

pause

