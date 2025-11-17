# 빠른 배포 가이드 - 다른 와이파이에서 접속

## 🚀 가장 빠른 방법: ngrok (5분)

### 1단계: ngrok 다운로드
- https://ngrok.com/download
- Windows용 다운로드 및 압축 해제

### 2단계: 계정 생성
- https://dashboard.ngrok.com/signup
- 무료 계정 생성
- 인증 토큰 복사

### 3단계: ngrok 설정
```bash
# ngrok 실행 파일이 있는 폴더로 이동
cd C:\path\to\ngrok

# 인증 토큰 설정
ngrok config add-authtoken YOUR_AUTH_TOKEN
```

### 4단계: 로컬 서버 실행
```bash
cd server-java
mvn spring-boot:run
```

### 5단계: ngrok 터널 생성
새 터미널에서:
```bash
ngrok http 5000
```

### 6단계: URL 공유
ngrok이 제공하는 URL (예: `https://abc123.ngrok.io`)을 공유하면:
- ✅ 인터넷 어디서나 접속 가능
- ✅ 다른 와이파이에서도 접속 가능
- ✅ HTTPS 자동 지원

---

## ☁️ 영구 배포: Render (무료)

### 1단계: GitHub에 코드 업로드
```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/yourusername/mrdabak.git
git push -u origin main
```

### 2단계: Render 설정
1. https://render.com 접속
2. "New +" → "Web Service"
3. GitHub 저장소 연결
4. 설정:
   - **Name**: mrdabak
   - **Environment**: Java
   - **Build Command**: `cd server-java && mvn clean package -DskipTests`
   - **Start Command**: `cd server-java && java -jar target/dinner-service-1.0.0.jar`
   - **Instance Type**: Free

### 3단계: 환경 변수
- `SPRING_PROFILES_ACTIVE=production`
- `JWT_SECRET=your-strong-secret-key`

### 4단계: 배포 완료
- 자동으로 배포됨
- URL: `https://your-app.onrender.com`
- 이제 인터넷 어디서나 접속 가능!

---

## 📱 접속 방법

### ngrok 사용 시
- URL: `https://abc123.ngrok.io` (ngrok이 제공)
- 휴대폰, 다른 PC 등 어디서나 접속 가능

### Render 사용 시
- URL: `https://your-app.onrender.com`
- 영구적으로 접속 가능

---

## ⚠️ 중요 사항

### ngrok
- 무료 버전은 URL이 재시작 시 변경됨
- 세션 시간 제한 있음
- 테스트용으로 적합

### Render
- 무료 티어는 15분 비활성 시 슬립 모드
- 첫 요청 시 깨어나는데 시간 소요
- 프로덕션용으로 적합

---

## 🔧 문제 해결

### ngrok 연결 안 됨
- 로컬 서버가 실행 중인지 확인
- 포트 5000이 열려있는지 확인

### Render 배포 실패
- 빌드 로그 확인
- Java 버전 확인 (17 필요)
- Maven 빌드 성공 여부 확인


