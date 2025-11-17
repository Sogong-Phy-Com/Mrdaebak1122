# Render 배포 후 로그인/회원가입 오류 해결

## ✅ 수정 완료 사항

### 1. CORS 설정 수정
- `SecurityConfig.java`: 모든 origin 허용 (`*`)
- 모든 컨트롤러의 `@CrossOrigin` 어노테이션 제거 (SecurityConfig에서 전역 처리)

### 2. application.properties 수정
- CORS 설정을 모든 origin 허용으로 변경

## 🚀 배포 방법

### 1. 변경사항 커밋 및 푸시
```bash
cd C:\Users\pando\Desktop\MrDaeBak
git add .
git commit -m "Fix CORS for Render deployment"
git push
```

### 2. Render에서 재배포
- Render 대시보드 → 서비스 → "Manual Deploy" → "Deploy latest commit"

## 🔍 추가 확인 사항

### 프론트엔드 API URL
프론트엔드가 Render URL을 사용하도록 설정되어 있는지 확인:

1. **프론트엔드가 같은 Render 서비스에서 서빙되는 경우:**
   - API URL을 상대 경로로 변경: `/api` (또는 빈 문자열)
   - 또는 환경 변수로 설정

2. **프론트엔드가 별도로 배포되는 경우:**
   - `REACT_APP_API_URL` 환경 변수를 Render 백엔드 URL로 설정
   - 예: `REACT_APP_API_URL=https://your-app.onrender.com/api`

### 프론트엔드 재빌드 필요
프론트엔드 코드를 수정했다면:
```bash
cd client
npm run build
# 빌드된 파일을 server-java/src/main/resources/static에 복사
```

## 📝 변경된 파일

1. `server-java/src/main/java/com/mrdabak/dinnerservice/config/SecurityConfig.java`
   - CORS를 모든 origin 허용으로 변경

2. `server-java/src/main/resources/application.properties`
   - CORS 설정 업데이트

3. 모든 컨트롤러 파일
   - `@CrossOrigin` 어노테이션 제거

## ⚠️ 보안 참고사항

현재 설정은 모든 origin을 허용합니다. 프로덕션에서는 특정 도메인만 허용하도록 제한하는 것을 권장합니다:

```java
configuration.setAllowedOriginPatterns(Arrays.asList(
    "https://your-frontend-domain.com",
    "https://*.onrender.com"
));
```

