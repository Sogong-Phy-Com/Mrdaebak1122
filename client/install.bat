@echo off
echo npm 패키지 설치 중...
npm install --legacy-peer-deps
if errorlevel 1 (
    echo [오류] npm 설치에 실패했습니다.
    pause
    exit /b 1
)
echo 설치 완료!
pause




