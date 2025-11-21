@echo off
echo ====================================
echo 데이터베이스 재설정
echo ====================================
echo.
echo 경고: 이 작업은 모든 데이터를 삭제합니다!
echo.
pause

echo 데이터베이스 파일 삭제 중...
if exist "server-java\data\mrdabak.db" (
    del /f "server-java\data\mrdabak.db"
    echo mrdabak.db 삭제 완료
)
if exist "server-java\data\orders.db" (
    del /f "server-java\data\orders.db"
    echo orders.db 삭제 완료
)
if exist "server-java\data\mrdabak.db-wal" (
    del /f "server-java\data\mrdabak.db-wal"
    echo mrdabak.db-wal 삭제 완료
)
if exist "server-java\data\mrdabak.db-shm" (
    del /f "server-java\data\mrdabak.db-shm"
    echo mrdabak.db-shm 삭제 완료
)
if exist "server-java\data\orders.db-wal" (
    del /f "server-java\data\orders.db-wal"
    echo orders.db-wal 삭제 완료
)
if exist "server-java\data\orders.db-shm" (
    del /f "server-java\data\orders.db-shm"
    echo orders.db-shm 삭제 완료
)

echo.
echo 데이터베이스 파일 삭제 완료!
echo 서버를 재시작하면 새로운 데이터베이스가 생성됩니다.
echo.
pause

