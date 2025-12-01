# Mr. DaeBak STT Microservice

FastAPI 기반 Whisper Small 한국어 음성 인식 마이크로서비스입니다.  
웹 프론트엔드에서 업로드한 음성을 텍스트로 변환해 Spring Boot 백엔드가 사용할 수 있도록 합니다.

## 1. 요구 사항

- Python 3.10 이상
- FFmpeg (whisper가 다양한 코덱을 처리할 수 있도록 설치 권장)

## 2. 설치

### Linux/macOS
```bash
cd stt-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### Windows (PowerShell)
```powershell
cd stt-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### Windows (CMD)
```cmd
cd stt-service
python -m venv .venv
.venv\Scripts\activate.bat
pip install -r requirements.txt
```

**참고**: PowerShell에서 실행 정책 오류가 발생하면 다음 명령어를 실행하세요:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## 3. 실행

```bash
uvicorn main:app --host 0.0.0.0 --port 8001
```

환경 변수를 통해 Whisper 모델과 초기 프롬프트를 조정할 수 있습니다.

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `WHISPER_MODEL` | `small` | 사용할 whisper 모델명 |
| `DOMAIN_PROMPT` | (샘플 값) | 도메인 용어 힌트 |

## 4. API

- `POST /stt/transcribe`
  - **요청**: `multipart/form-data`, 필드명 `file`
  - **응답**
    ```json
    {
      "text": "샴페인 축제 디너로 부탁드립니다."
    }
    ```

## 5. Whisper 초기 프롬프트

`DOMAIN_PROMPT`에는 아래와 같은 도메인 핵심 용어를 포함시키는 것이 좋습니다.

```
발렌타인 디너, 프렌치 디너, 잉글리시 디너, 샴페인 축제 디너,
심플 스타일, 그랜드 스타일, 디럭스 스타일,
와인, 샴페인, 커피, 스테이크, 샐러드, 바게트빵
```

## 6. 개발 팁

- whisper 모델은 최초 로드 시 시간이 걸리므로 애플리케이션 기동 시 한 번만 로드하도록 구현했습니다.
- 기본적으로 한국어(`language="ko"`)로 인식하며, 필요 시 `task="transcribe"`로 변경 가능합니다.


