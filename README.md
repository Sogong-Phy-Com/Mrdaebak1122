# Mr. DaeBak Dinner Service

## 폴더 구조

이 프로젝트는 두 개의 폴더로 구분되어 있습니다:

### 📦 MrDaebak (배포용)
- **용도**: Render 배포용
- **위치**: `C:\Users\pando\Desktop\MrDaebak`
- **설정**: 프로덕션 환경 (Render URL 자동 감지)
- **문서**: `README-DEPLOY.md` 참조

### 💻 MrDaebak-local (로컬 개발용)
- **용도**: 로컬 개발 및 테스트
- **위치**: `C:\Users\pando\Desktop\MrDaebak-local`
- **설정**: 로컬 환경 (`http://localhost:5000`)
- **문서**: `README-LOCAL.md` 참조

## 주요 기능

- ✅ 음성 주문 (Web Speech API)
- ✅ OpenAI GPT-4o-mini 연동
- ✅ 주문 관리 시스템
- ✅ 관리자 대시보드

## 빠른 시작

### 로컬 개발
```bash
cd C:\Users\pando\Desktop\MrDaebak-local
run-local.bat
```

### 배포
Render 대시보드에서 자동 배포 또는 `MrDaebak` 폴더에서 수동 배포

## 기능적 차이

**없습니다!** 두 폴더는 동일한 기능을 제공하며, 단지 환경 설정만 다릅니다:
- 로컬: `localhost:5000` 사용
- 배포: Render URL 사용
