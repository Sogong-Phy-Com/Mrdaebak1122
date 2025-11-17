# Render 배포 단계별 가이드

## 🎯 목표
Render를 사용하여 인터넷 어디서나 접속 가능한 서비스 배포

## 📋 사전 준비

### 1. GitHub 계정 및 저장소
- GitHub 계정이 있어야 합니다
- 저장소가 준비되어 있어야 합니다

### 2. Render 계정
- https://render.com 접속
- "Get Started for Free" 클릭
- GitHub 계정으로 로그인

---

## 🚀 단계별 배포

### 1단계: GitHub에 코드 업로드

#### 1-1. Git 저장소 확인
```bash
cd C:\Users\pando\Desktop\MrDaeBak
git status
```

#### 1-2. 모든 파일 추가
```bash
git add .
```

#### 1-3. 커밋
```bash
git commit -m "Ready for Render deployment"
```

#### 1-4. GitHub에 푸시
```bash
# GitHub 저장소가 없다면 먼저 생성
# GitHub.com에서 "New repository" 클릭하여 생성

git remote add origin https://github.com/yourusername/mrdabak.git
git branch -M main
git push -u origin main
```

**주의**: `yourusername`을 본인의 GitHub 사용자명으로 변경하세요.

---

### 2단계: Render에서 Web Service 생성

#### 2-1. Render 대시보드 접속
- https://dashboard.render.com 접속
- 로그인

#### 2-2. 새 Web Service 생성
1. "New +" 버튼 클릭
2. "Web Service" 선택
3. GitHub 저장소 연결
   - "Connect GitHub" 클릭
   - 저장소 선택: `mrdabak` (또는 본인의 저장소 이름)
   - "Connect" 클릭

#### 2-3. 서비스 설정
다음 정보를 입력:

**Basic Settings:**
- **Name**: `mrdabak-dinner-service` (원하는 이름)
- **Region**: `Singapore` (가장 가까운 지역 선택)
- **Branch**: `main` (또는 `master`)

**Build & Deploy:**
- **Runtime**: `Java`
- **Build Command**: 
  ```
  cd server-java && mvn clean package -DskipTests
  ```
- **Start Command**: 
  ```
  cd server-java && java -jar target/dinner-service-1.0.0.jar
  ```

**Instance Type:**
- **Free** 선택 (무료 티어)

#### 2-4. 환경 변수 설정
"Environment" 섹션에서 다음 환경 변수 추가:

1. **SPRING_PROFILES_ACTIVE**
   - Key: `SPRING_PROFILES_ACTIVE`
   - Value: `production`

2. **JWT_SECRET**
   - Key: `JWT_SECRET`
   - Value: `your-very-strong-secret-key-minimum-256-bits-long` (강력한 키 사용)

3. **PORT** (선택사항)
   - Render가 자동으로 PORT 환경 변수를 제공합니다
   - application.properties에서 `${PORT}` 사용 가능

#### 2-5. 고급 설정 (선택사항)
- **Health Check Path**: `/api/health`
- **Auto-Deploy**: `Yes` (GitHub 푸시 시 자동 배포)

#### 2-6. 생성
- "Create Web Service" 클릭

---

### 3단계: application.properties 수정

Render는 동적 포트를 사용하므로 설정을 수정해야 합니다.

#### 3-1. application.properties 수정
`server-java/src/main/resources/application.properties` 파일 수정:

```properties
# Server Configuration
server.address=0.0.0.0
server.port=${PORT:5000}
spring.application.name=mrdabak-dinner-service

# Database Configuration (SQLite)
spring.datasource.url=jdbc:sqlite:data/mrdabak.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.id.new_generator_mappings=false
spring.jpa.properties.hibernate.jdbc.use_get_generated_keys=false

# JWT Configuration
jwt.secret=${JWT_SECRET:your-secret-key-change-in-production-make-it-long-and-secure}
jwt.expiration=604800000

# CORS Configuration
# Render 배포 시 모든 origin 허용 (프로덕션에서는 제한 권장)
spring.web.cors.allowed-origins=*
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS
spring.web.cors.allowed-headers=*
spring.web.cors.allow-credentials=true
```

**변경 사항:**
- `server.port=${PORT:5000}`: Render의 PORT 환경 변수 사용
- `jwt.secret=${JWT_SECRET:...}`: 환경 변수에서 JWT Secret 읽기
- CORS: 모든 origin 허용 (프로덕션에서는 도메인 제한 권장)

#### 3-2. 변경사항 커밋 및 푸시
```bash
git add server-java/src/main/resources/application.properties
git commit -m "Configure for Render deployment"
git push
```

---

### 4단계: 배포 확인

#### 4-1. 배포 상태 확인
- Render 대시보드에서 "Events" 탭 확인
- 빌드 및 배포 진행 상황 확인

#### 4-2. 배포 완료 대기
- 첫 배포는 약 5-10분 소요
- "Live" 상태가 되면 완료

#### 4-3. URL 확인
- Render 대시보드에서 제공하는 URL 확인
- 예: `https://mrdabak-dinner-service.onrender.com`

#### 4-4. 테스트
브라우저에서 접속:
- `https://your-app.onrender.com/api/health`
- 응답: `{"status":"ok","message":"Mr. DaeBak API is running"}`

---

### 5단계: 프론트엔드 설정 (선택사항)

#### 5-1. React 앱 빌드
```bash
cd client
npm run build
```

#### 5-2. 빌드된 파일을 static 폴더에 복사
```bash
xcopy /E /I /Y build\* ..\server-java\src\main\resources\static
```

#### 5-3. 커밋 및 푸시
```bash
git add .
git commit -m "Add frontend build"
git push
```

이제 `https://your-app.onrender.com`에서 프론트엔드도 접속 가능합니다.

---

## 🔧 문제 해결

### 빌드 실패
**문제**: Maven 빌드 실패
**해결**:
- Build Command 확인
- Java 버전 확인 (17 필요)
- `mvn clean package -DskipTests` 로컬에서 테스트

### 포트 오류
**문제**: 포트 바인딩 실패
**해결**:
- `application.properties`에서 `server.port=${PORT:5000}` 확인
- Render의 PORT 환경 변수 자동 제공 확인

### 데이터베이스 오류
**문제**: SQLite 파일 권한 오류
**해결**:
- Render는 임시 파일 시스템을 사용하므로 데이터가 유지되지 않을 수 있음
- 프로덕션에서는 PostgreSQL 사용 권장

### CORS 오류
**문제**: 프론트엔드에서 API 호출 실패
**해결**:
- CORS 설정 확인
- 프론트엔드의 API URL이 Render URL로 설정되어 있는지 확인

---

## 📝 체크리스트

배포 전 확인:
- [ ] GitHub에 코드 푸시 완료
- [ ] Render 계정 생성 및 GitHub 연결
- [ ] Web Service 생성
- [ ] Build Command 설정: `cd server-java && mvn clean package -DskipTests`
- [ ] Start Command 설정: `cd server-java && java -jar target/dinner-service-1.0.0.jar`
- [ ] 환경 변수 설정 (SPRING_PROFILES_ACTIVE, JWT_SECRET)
- [ ] application.properties에서 `${PORT}` 사용
- [ ] 배포 완료 및 URL 확인

---

## 🎉 완료!

배포가 완료되면:
- ✅ 인터넷 어디서나 접속 가능
- ✅ HTTPS 자동 지원
- ✅ 무료 티어 사용
- ✅ GitHub 푸시 시 자동 재배포

**접속 URL**: `https://your-app.onrender.com`

---

## ⚠️ 주의사항

### 무료 티어 제한
- 15분 비활성 시 슬립 모드
- 첫 요청 시 깨어나는데 시간 소요 (약 30초)
- 월 750시간 제한

### 데이터베이스
- SQLite는 임시 파일 시스템에 저장되어 재시작 시 삭제될 수 있음
- 프로덕션에서는 Render의 PostgreSQL 사용 권장

### 보안
- JWT_SECRET을 강력한 키로 설정
- 프로덕션에서는 CORS를 특정 도메인으로 제한

---

## 🔄 업데이트 방법

코드를 수정한 후:
```bash
git add .
git commit -m "Update code"
git push
```

Render가 자동으로 재배포합니다!


