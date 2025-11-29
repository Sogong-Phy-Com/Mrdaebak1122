@echo off
echo Building Mr. Dinner Service...
echo.

REM Clean previous build
if exist *.class (
    echo Cleaning previous build...
    del /q *.class
    for /r /d %%d in (*) do (
        if exist "%%d\*.class" del /q "%%d\*.class"
    )
)

echo Compiling Java source files...

REM Compile all Java files with Java 8 compatibility
javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\common\*.java
if %errorlevel% neq 0 (
    echo Error compiling common domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\customer\*.java
if %errorlevel% neq 0 (
    echo Error compiling customer domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\menu\*.java
if %errorlevel% neq 0 (
    echo Error compiling menu domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\order\*.java
if %errorlevel% neq 0 (
    echo Error compiling order domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\staff\*.java
if %errorlevel% neq 0 (
    echo Error compiling staff domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\delivery\*.java
if %errorlevel% neq 0 (
    echo Error compiling delivery domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\payment\*.java
if %errorlevel% neq 0 (
    echo Error compiling payment domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\domain\inventory\*.java
if %errorlevel% neq 0 (
    echo Error compiling inventory domain classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\service\*.java
if %errorlevel% neq 0 (
    echo Error compiling service classes
    pause
    exit /b 1
)

javac -d . -source 8 -target 8 -encoding UTF-8 com\mrdinner\app\*.java
if %errorlevel% neq 0 (
    echo Error compiling application classes
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo.
echo To run the application, use:
echo java com.mrdinner.app.Main
echo.
pause
