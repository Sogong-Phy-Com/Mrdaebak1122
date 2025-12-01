import logging
import os
import tempfile
from pathlib import Path

import uvicorn
import whisper
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

logger = logging.getLogger("stt-service")
logging.basicConfig(level=logging.INFO)


class TranscriptionResponse(BaseModel):
    text: str


WHISPER_MODEL = os.getenv("WHISPER_MODEL", "small")
DOMAIN_PROMPT = os.getenv(
    "DOMAIN_PROMPT",
    "미스터 대박 디너 서비스, 발렌타인 디너, 프렌치 디너, 잉글리시 디너, "
    "샴페인 축제 디너, 심플 스타일, 그랜드 스타일, 디럭스 스타일, "
    "바게트빵, 샴페인, 와인, 커피, 스테이크, 샐러드, 에그 스크램블, 베이컨",
)

TEMP_DIR = Path(os.getenv("STT_TMP_DIR", Path(__file__).resolve().parent / "tmp"))
TEMP_DIR.mkdir(parents=True, exist_ok=True)

try:
    import imageio_ffmpeg

    ffmpeg_binary = imageio_ffmpeg.get_ffmpeg_exe()
    ffmpeg_dir = os.path.dirname(ffmpeg_binary)
    if ffmpeg_dir not in os.environ.get("PATH", ""):
        os.environ["PATH"] = ffmpeg_dir + os.pathsep + os.environ.get("PATH", "")
    logger.info("FFmpeg registered for Whisper: %s", ffmpeg_binary)
except Exception as exc:  # pragma: no cover
    logger.warning("Bundled FFmpeg unavailable: %s", exc)

model = whisper.load_model(WHISPER_MODEL)

app = FastAPI(title="Mr. DaeBak STT Service", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/stt/transcribe", response_model=TranscriptionResponse)
async def transcribe(file: UploadFile = File(...)):
    try:
        contents = await file.read()
        if not contents:
            raise HTTPException(status_code=400, detail="빈 오디오 파일입니다.")

        suffix = Path(file.filename or "audio.webm").suffix or ".webm"
        tmp_handle = tempfile.NamedTemporaryFile(delete=False, suffix=suffix, dir=TEMP_DIR)
        try:
            tmp_handle.write(contents)
            tmp_handle.flush()
            tmp_path = tmp_handle.name
        finally:
            tmp_handle.close()

        try:
            result = model.transcribe(
                tmp_path,
                language="ko",
                task="transcribe",
                initial_prompt=DOMAIN_PROMPT,
                temperature=0,
                condition_on_previous_text=True,
                without_timestamps=True,
            )
        finally:
            try:
                os.unlink(tmp_path)
            except OSError:
                logger.warning("임시 파일 삭제 실패: %s", tmp_path)

        text = result.get("text", "").strip()
        return TranscriptionResponse(text=text)
    except HTTPException:
        raise
    except Exception as exc:  # pylint: disable=broad-except
        logger.exception("Transcription failed")
        raise HTTPException(status_code=500, detail=f"Transcription failed: {exc}") from exc


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", "8001")))


