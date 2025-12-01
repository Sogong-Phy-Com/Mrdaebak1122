# Mr. DaeBak - 배포용

이 폴더는 **Render 배포**를 위한 설정입니다.

## 주요 설정

- **API URL**: 환경 변수 또는 자동 감지 (프로덕션 URL)
- **프론트엔드**: Render에서 빌드 및 배포
- **백엔드**: Render에서 Docker로 배포
- **데이터베이스**: Render의 영구 스토리지

## 배포 방법

### Render 배포

1. GitHub에 푸시:
```bash
git add .
git commit -m "Deploy"
git push origin main
```

2. Render 대시보드에서:
   - 자동 배포가 활성화되어 있으면 자동으로 배포됩니다
   - 수동 배포: "Manual Deploy" → "Deploy latest commit"

## 환경 변수 설정 (Render 대시보드)

다음 환경 변수를 Render 대시보드에서 설정하세요:

- `VOICE_LLM_API_KEY`: OpenAI API 키
- `VOICE_LLM_API_URL`: `https://api.openai.com/v1/chat/completions` (기본값)
- `VOICE_LLM_MODEL`: `gpt-4o-mini` (기본값)
- `JWT_SECRET`: 자동 생성됨
- `PORT`: 자동 설정됨

## 기능

- ✅ 음성 주문 (Web Speech API 사용)
- ✅ OpenAI GPT-4o-mini 연동
- ✅ 모든 주문 관리 기능
- ✅ 관리자 기능

## 로컬 개발

로컬 개발은 `MrDaebak-local` 폴더를 사용하세요.

