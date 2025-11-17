# 클라우드 배포 가이드 - 인터넷에서 접속 가능

## 🌐 목표
다른 와이파이(인터넷 어디서나)에서도 접속 가능하도록 배포

## 🚀 옵션 1: Render (권장, 무료)

### 장점
- 무료 티어 제공
- 자동 HTTPS 설정
- GitHub 연동으로 자동 배포
- 커스텀 도메인 지원

### 배포 단계

#### 1. GitHub에 코드 푸시
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/yourusername/mrdabak.git
git push -u origin main
```

#### 2. Render 계정 생성
- https://render.com 접속
- GitHub 계정으로 로그인

#### 3. 새 Web Service 생성
1. "New +" → "Web Service" 클릭
2. GitHub 저장소 연결
3. 설정:
   - **Name**: mrdabak-dinner-service
   - **Environment**: Java
   - **Build Command**: `cd server-java && mvn clean package`
   - **Start Command**: `cd server-java && java -jar target/dinner-service-1.0.0.jar`
   - **Instance Type**: Free

#### 4. 환경 변수 설정
Render 대시보드에서:
- `SPRING_PROFILES_ACTIVE=production`
- `JWT_SECRET=your-secret-key-here` (강력한 키 사용)

#### 5. 자동 배포
- GitHub에 푸시하면 자동으로 배포됩니다
- 배포 후 URL: `https://your-app.onrender.com`

---

## 🚂 옵션 2: Railway (무료 티어)

### 배포 단계

#### 1. Railway 계정 생성
- https://railway.app 접속
- GitHub 계정으로 로그인

#### 2. 새 프로젝트 생성
1. "New Project" 클릭
2. "Deploy from GitHub repo" 선택
3. 저장소 선택

#### 3. 설정
- **Root Directory**: `server-java`
- **Build Command**: `mvn clean package`
- **Start Command**: `java -jar target/dinner-service-1.0.0.jar`

#### 4. 환경 변수
Railway 대시보드에서 환경 변수 설정

---

## ☁️ 옵션 3: Oracle Cloud Free Tier (영구 무료)

### 장점
- 영구 무료 티어
- VM 인스턴스 제공
- 완전한 제어

### 배포 단계

#### 1. Oracle Cloud 계정 생성
- https://www.oracle.com/cloud/free/ 접속
- 계정 생성 (신용카드 필요하지만 무료)

#### 2. VM 인스턴스 생성
1. "Create Instance" 클릭
2. 설정:
   - **Image**: Ubuntu 22.04
   - **Shape**: VM.Standard.A1.Flex (ARM, 무료)
   - **SSH Key**: 생성 및 다운로드

#### 3. 서버 설정
```bash
# SSH 접속
ssh -i your-key.key ubuntu@your-ip

# Java 설치
sudo apt update
sudo apt install openjdk-17-jdk maven -y

# 코드 업로드 및 실행
git clone https://github.com/yourusername/mrdabak.git
cd mrdabak/server-java
mvn clean package
java -jar target/dinner-service-1.0.0.jar
```

---

## 🔧 옵션 4: ngrok (빠른 테스트용)

### 장점
- 즉시 사용 가능
- 로컬 서버를 인터넷에 노출
- 무료 티어 제공

### 사용 방법

#### 1. ngrok 설치
- https://ngrok.com/download 접속
- 다운로드 및 설치

#### 2. 계정 생성 및 인증
```bash
ngrok config add-authtoken your-token
```

#### 3. 터널 생성
```bash
ngrok http 5000
```

#### 4. 접속
- ngrok이 제공하는 URL 사용 (예: `https://abc123.ngrok.io`)
- 이 URL은 인터넷 어디서나 접속 가능

---

## 📝 배포 전 준비사항

### 1. application.properties 수정
프로덕션 환경 설정:
```properties
# 프로덕션 환경
spring.profiles.active=production

# 데이터베이스 (SQLite는 프로덕션에 부적합, PostgreSQL 권장)
spring.datasource.url=jdbc:postgresql://localhost:5432/mrdabak
spring.datasource.username=your-username
spring.datasource.password=your-password

# JWT Secret (강력한 키 사용)
jwt.secret=your-very-strong-secret-key-here-minimum-256-bits

# CORS (프로덕션 도메인으로 제한)
spring.web.cors.allowed-origins=https://your-domain.com
```

### 2. React 앱 빌드 및 API URL 수정
```bash
cd client
# .env.production 파일 생성
echo "REACT_APP_API_URL=https://your-api-url.com/api" > .env.production
npm run build
```

### 3. 데이터베이스 마이그레이션
SQLite → PostgreSQL 권장:
- 프로덕션에서는 PostgreSQL 사용
- 데이터 마이그레이션 스크립트 작성

---

## 🎯 빠른 시작 (ngrok)

가장 빠르게 테스트하려면:

1. **ngrok 다운로드**: https://ngrok.com/download
2. **계정 생성 및 토큰 발급**
3. **로컬 서버 실행** (포트 5000)
4. **ngrok 실행**:
   ```bash
   ngrok http 5000
   ```
5. **제공된 URL 공유**: `https://abc123.ngrok.io`

---

## 🔒 보안 고려사항

1. **HTTPS 필수**: 프로덕션에서는 반드시 HTTPS 사용
2. **JWT Secret**: 강력한 키 사용 (최소 256비트)
3. **CORS**: 프로덕션 도메인으로 제한
4. **데이터베이스**: SQLite 대신 PostgreSQL/MySQL 사용
5. **환경 변수**: 민감한 정보는 환경 변수로 관리

---

## 📊 비교표

| 서비스 | 무료 티어 | HTTPS | 자동 배포 | 커스텀 도메인 |
|--------|----------|-------|----------|--------------|
| Render | ✅ | ✅ | ✅ | ✅ |
| Railway | ✅ (제한적) | ✅ | ✅ | ✅ |
| Oracle Cloud | ✅ (영구) | ❌ (직접 설정) | ❌ | ✅ |
| ngrok | ✅ (제한적) | ✅ | ❌ | ❌ |

---

## 🚀 추천 순서

1. **빠른 테스트**: ngrok 사용
2. **무료 배포**: Render 사용
3. **장기 운영**: Oracle Cloud 또는 AWS


