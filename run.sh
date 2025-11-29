#!/bin/bash

echo "===================================="
echo "미스터 대박 디너 서비스 시작"
echo "===================================="
echo ""

# Java와 Maven이 설치되어 있는지 확인
if ! command -v java &> /dev/null; then
    echo "[오류] Java가 설치되어 있지 않습니다."
    echo "Java 17 이상을 설치해주세요."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "[오류] Maven이 설치되어 있지 않습니다."
    echo "Maven을 설치하거나 Maven Wrapper를 사용하세요."
    exit 1
fi

# Python 확인 (STT 서비스)
PYTHON_BIN=""
if command -v python3 &> /dev/null; then
    PYTHON_BIN="python3"
elif command -v python &> /dev/null; then
    PYTHON_BIN="python"
else
    echo "[오류] Python이 설치되어 있지 않습니다."
    echo "음성 주문 STT 서비스를 실행하려면 Python 3.10 이상이 필요합니다."
    exit 1
fi

# curl 확인 (Ollama 상태 점검용)
if ! command -v curl &> /dev/null; then
    echo "[경고] curl 명령을 찾을 수 없습니다. Ollama 상태 확인을 건너뜁니다."
fi

echo "[1/4] STT(FastAPI + Whisper) 서비스 시작..."
cd stt-service || exit 1
STT_LOG_PATH="$(pwd)/stt-service.log"
echo "[INFO] 로그: $STT_LOG_PATH"
$PYTHON_BIN -m uvicorn main:app --host 0.0.0.0 --port 8001 > "$STT_LOG_PATH" 2>&1 &
STT_PID=$!
cd ..

sleep 3
echo "[INFO] STT 서비스 PID: $STT_PID"

echo "[2/4] Ollama(qwen2.5) 서버 확인..."
OLLAMA_PID=""
if command -v curl &> /dev/null && curl -s http://localhost:11434/api/version >/dev/null 2>&1; then
    echo "[INFO] Ollama 서버가 이미 실행 중입니다."
else
    if command -v ollama &> /dev/null; then
        echo "[INFO] Ollama 서버를 백그라운드로 시작합니다."
        ollama serve >/tmp/ollama.log 2>&1 &
        OLLAMA_PID=$!
        sleep 5
        echo "[INFO] Ollama 로그: /tmp/ollama.log"
    else
        echo "[경고] Ollama CLI를 찾을 수 없습니다. qwen2.5 모델이 실행 중인지 직접 확인하세요."
    fi
fi

echo "[3/4] 백엔드 서버 시작 중..."
cd server-java
mkdir -p data
echo "Building and starting Spring Boot application..."
mvn clean spring-boot:run &
BACKEND_PID=$!
cd ..

echo "[INFO] Waiting for backend server initialization... (15 seconds)"
sleep 15

echo "[4/4] 프론트엔드 시작 중..."
cd client
if [ ! -d "node_modules" ]; then
    echo "npm 패키지 설치 중..."
    npm install --legacy-peer-deps
    if [ $? -ne 0 ]; then
        echo "[오류] npm 설치에 실패했습니다."
        exit 1
    fi
fi
npm start &
FRONTEND_PID=$!
cd ..

echo ""
echo "===================================="
echo "서비스가 시작되었습니다!"
echo "===================================="
echo "STT 서비스: http://localhost:8001/stt/transcribe"
echo "Ollama API : http://localhost:11434 (qwen2.5)"
echo "백엔드     : http://localhost:5000"
echo "프론트엔드 : http://localhost:3000 (음성 주문 페이지 /voice-order)"
echo ""
echo "서비스를 종료하려면 Ctrl+C를 누르세요."
echo "===================================="

# 종료 시그널 처리
cleanup() {
    echo ""
    echo "[INFO] 서비스 종료 중..."
    kill $BACKEND_PID $FRONTEND_PID $STT_PID 2>/dev/null
    if [ -n "$OLLAMA_PID" ]; then
        kill $OLLAMA_PID 2>/dev/null
    fi
    exit
}

trap cleanup INT TERM

wait

