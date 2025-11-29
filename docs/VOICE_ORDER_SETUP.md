## 1. 기존 도메인 구조 요약

- **주요 모델**
  - `Order`, `OrderItem` (`server-java/src/main/java/com/mrdabak/dinnerservice/model/`)  
    - 주문 상태/결제 상태, 디너 타입, 서빙 스타일, 배달 주소/시간 저장.
  - `DinnerType`, `DinnerMenuItem`, `MenuItem`
    - `DataInitializer`에서 4가지 디너(발렌타인, 프렌치, 잉글리시, 샴페인 축제)와 기본 구성 저장.
  - `DeliverySchedule`, `EmployeeWorkAssignment`
    - 배달 스케줄 및 직원 할당 관리.
- **서빙 스타일** (`MenuController#getServingStyles`)
  - `simple`, `grand`, `deluxe` (샴페인 축제 디너는 grand/deluxe만 허용 – `OrderService#createOrderInternal`).
- **주문/결제/배달 API 위치**
  - 주문 생성/조회/수정: `OrderController` (`/api/orders`, `/api/orders/{id}/modify`)
  - 결제 처리: `PaymentController#processPayment` (`/api/payment/process`)
  - 배달/상태 업데이트: `EmployeeController` (`/api/employee/orders`, `/api/employee/delivery-schedule`)

## 2. 음성 주문 아키텍처

```
브라우저 (VoiceOrderPage)
  ├─ 녹음(Web Audio/MediaRecorder) + 텍스트 입력
  ├─ POST /api/voice-orders/transcribe -> STT 서비스(Whisper small, FastAPI)
  ├─ POST /api/voice-orders/utterance  -> Spring VoiceOrderController
  └─ POST /api/voice-orders/confirm    -> OrderService + Payment flow

Spring Boot (server-java)
  ├─ controller/VoiceOrderController : 세션 시작, 음성→텍스트, LLM 대화, 주문 확정
  ├─ voice/service/*
  │    ├─ SpeechToTextClient : FastAPI STT 호출
  │    ├─ VoiceOrderAssistantClient : Ollama Qwen2.5 chat API 호출
  │    ├─ VoiceConversationService/SessionService : 세션/대화 관리
  │    ├─ VoiceOrderMapper/CheckoutService : 주문 DTO 변환 + OrderService 연동
  │    └─ VoiceMenuCatalogService : 디너/메뉴 메타데이터 & LLM 시스템 프롬프트
  └─ 기존 OrderService/Payment 흐름 재사용 (결제 상태는 voice-bot-card 로 즉시 paid 처리)

FastAPI STT (stt-service)
  ├─ `POST /stt/transcribe` : Whisper small, language=ko, initial_prompt 에 도메인 키워드 삽입
  └─ requirements: `openai-whisper`, `fastapi`, `uvicorn`
```

LLM 시스템 프롬프트 핵심:
- 한국어 존댓말, “미스터 대박 음성 상담원” 자아고정.
- 도메인 외 질문은 정중히 거절 후 다시 주문 컨텍스트로 유도.
- 응답 형식: `assistant_message` + ```json order_state_json``` (디너, 스타일, 메뉴 조정, 주소, 날짜/시간, readyForConfirmation 플래그 등).
- menuAdjustments.item 키는 `champagne | wine | coffee | steak | salad | eggs | bacon | bread | baguette` 중 하나만 사용.

## 3. 실행 및 환경 설정

### 3.1 STT 서비스
```bash
cd stt-service
python -m venv .venv
. .venv/bin/activate           # Windows  : .\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001
```
환경 변수:
- `WHISPER_MODEL` (기본 `small`)
- `DOMAIN_PROMPT` (예: “발렌타인 디너, 프렌치 디너, …”)

### 3.2 Ollama + Qwen2.5
```bash
ollama pull qwen2.5:latest
ollama serve                             # 11434 포트 기본
```

### 3.3 Spring/React 환경 변수 (`server-java` 기준)
`application.properties`에서 다음 값을 환경 변수로 오버라이드 가능:
```
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL_NAME=qwen2.5:latest
STT_SERVICE_URL=http://localhost:8001/stt/transcribe
VOICE_ORDER_SESSION_TTL_MINUTES=45
VOICE_ORDER_HISTORY_LIMIT=40
```

### 3.4 서비스 실행
1. 통합 스크립트 사용
   - macOS/Linux: `./run.sh`
   - Windows: `run.bat`
   - 두 스크립트 모두 STT 서비스와 Ollama 서버(미실행 시)를 자동으로 기동한 뒤 백엔드/프론트엔드를 띄운다.
2. 수동 실행 순서 (필요 시)
   - STT 서비스 실행
   - Ollama(qwen2.5) 실행
   - Spring Boot: `cd server-java && ./mvnw spring-boot:run`
   - React: `cd client && npm start`
3. 브라우저 접속: `http://localhost:3000/voice-order`

## 4. 테스트 시나리오 (수동)

1. 고객 로그인 후 “음성 주문” 진입 → 자동으로 LLM 인사 메시지 수신.
2. 고객 발언: “맛있는 디너 추천해 주세요.”
3. 상담원: 기념일 질문 → 고객 “내일이 어머님 생신이에요.”
4. 상담원: 프렌치 or 샴페인 축제 디너 추천.
5. 고객: “샴페인 축제 디너 좋겠어요.”
6. 상담원: 디럭스 제안 → 고객 “네, 디럭스 스타일 좋아요.”
7. 고객: “바게트빵 6개, 샴페인 2병으로 변경.”
8. 상담원: 요약 재확인.
9. 고객: “맞아요. 추가로 필요한 건 없어요.”
10. 상담원: 날짜/시간/주소/연락처 확인, 최종 JSON에서 `readyForConfirmation=true`.
11. “주문 확정하기” 버튼 활성화 → 누르면 실제 Order + Payment 완료, 상담원 마지막 멘트/요약 표시.

## 5. 추가 참고

- `docs/VOICE_ORDER_SETUP.md` 본 파일에서 도메인 정보와 새 플로우 요약.
- FastAPI STT + Ollama endpoint 주소가 변경되면 `.env` 또는 환경 변수로 주입.
- LLM JSON 파싱 실패 시에도 상담 답변은 그대로 UI에 노출되며, 서버 로그로 원인 확인 가능.

## 6. 2025-11-29 업데이트 요약

### STT 서비스
- Whisper가 한글 경로의 임시 파일을 처리하지 못하던 문제를 해결했습니다. STT 서비스는 이제 `stt-service/tmp/` (환경 변수 `STT_TMP_DIR`로 변경 가능)에 ASCII 경로의 임시 파일을 만들고 처리 후 삭제합니다.
- `imageio-ffmpeg`를 추가해 별도 FFmpeg 설치 없이도 오디오 디코딩이 가능하며, 서비스 시작 시 자동으로 PATH에 등록됩니다.
- `pip install -r requirements.txt`를 다시 실행한 뒤 `uvicorn main:app --host 0.0.0.0 --port 8001`으로 재기동해야 합니다.
- 생성되는 `tmp/`, `stt-service.log`, `__pycache__/`, `.venv/` 등은 `.gitignore`에 추가되어 저장소에 포함되지 않습니다.

### Ollama / LLM
- `/api/voice-orders/start`가 `qwen2.5:latest` 모델을 호출하는데, 로컬에 모델이 없으면 404가 발생합니다. 반드시 아래 명령으로 모델을 내려받고 서버를 실행하세요.
  ```bash
  ollama pull qwen2.5:latest
  ollama serve
  ```

### 프론트엔드 UX
- 음성 녹음은 발화 종료 후 1.5초 동안 무음이 감지되면 자동으로 중지되고, 오디오가 즉시 업로드됩니다.
- 사용자가 말한 텍스트는 LLM 응답과 무관하게 먼저 채팅창에 표시되며, 이후 서버에서 돌아온 메시지로 치환됩니다.

## 7. 배포 전 체크리스트
1. `stt-service`에서 `pip install -r requirements.txt && uvicorn main:app --host 0.0.0.0 --port 8001`.
2. Ollama에서 `ollama pull qwen2.5:latest` 후 `ollama serve`.
3. `run.bat` 또는 `./run.sh`로 Spring Boot/React + STT + Ollama를 한 번에 실행하거나, 3.4절 순서로 수동 실행.
4. 브라우저 `http://localhost:3000/voice-order` → 마이크 권한 허용 → “🎙️ 음성 녹음” 버튼 테스트.
5. 깃에는 코드·문서만 커밋하고, `.venv/`, `tmp/`, `stt-service.log` 등 로컬 산출물은 포함하지 않음.

위 내용을 README나 PR 노트에 붙여넣으면 오늘 작업한 내역과 설치/환경 정보를 한눈에 공유할 수 있습니다.


