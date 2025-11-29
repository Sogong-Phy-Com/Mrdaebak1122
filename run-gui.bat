@echo off
echo Starting Mr. Dinner Service GUI...
echo.

REM Check if classes exist
if not exist com\mrdinner\app\Main.class (
    echo Classes not found. Building first...
    call build.bat
    if %errorlevel% neq 0 (
        echo Build failed. Exiting...
        pause
        exit /b 1
    )
)

echo Compiling GUI classes...
javac -d . -source 8 -target 8 com\mrdinner\gui\*.java

if %errorlevel% neq 0 (
    echo GUI compilation failed. Exiting...
    pause
    exit /b 1
)

echo Starting Mr. Dinner Service GUI...
echo.

java com.mrdinner.gui.MainGUI

echo.
echo GUI application closed.
pause
