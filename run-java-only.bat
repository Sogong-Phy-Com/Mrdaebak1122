@echo off
echo ====================================
echo Mr. DaeBak Backend Server Starting
echo ====================================
echo.

cd server-java
if not exist "data" mkdir data
mvn spring-boot:run

pause
