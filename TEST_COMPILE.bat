@echo off
chcp 65001 >nul
echo ====================================
echo Compilation Test
echo ====================================
echo.
echo Compiling project to check for errors.
echo.

cd /d "%~dp0server-java"
echo [1/2] Downloading Maven dependencies...
call mvn dependency:resolve
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Dependency download failed!
    pause
    exit /b 1
)
echo [OK] Dependencies downloaded
echo.

echo [2/2] Compiling project...
call mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ====================================
    echo [ERROR] Compilation failed!
    echo ====================================
    echo.
    echo Check the error messages above.
    echo.
    pause
    exit /b 1
)
echo.
echo ====================================
echo [OK] Compilation successful!
echo ====================================
echo.
echo You can now run START_SERVER.bat
echo.
pause
