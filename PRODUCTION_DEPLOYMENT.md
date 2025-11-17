# 프로덕션 배포 가이드

## ✅ 완료된 설정

1. **React 앱 빌드**: `client/build` 폴더에 빌드 완료
2. **Spring Boot 정적 리소스**: `server-java/src/main/resources/static`에 복사 완료
3. **WebConfig**: React Router 지원 설정 완료
4. **SecurityConfig**: 정적 리소스 접근 허용 설정 완료

## 🌐 포트 번호 없이 접속하기

### 옵션 1: 포트 80 사용 (권장, 관리자 권한 필요)

포트 80은 HTTP의 기본 포트이므로 포트 번호를 입력하지 않아도 됩니다.

#### 설정 방법

1. **application.properties 수정**:
   ```
   server.port=80
   ```

2. **관리자 권한으로 서버 실행**:
   - CMD나 PowerShell을 **관리자 권한**으로 실행
   - `cd server-java`
   - `mvn spring-boot:run`

3. **방화벽 설정** (관리자 권한 필요):
   ```powershell
   netsh advfirewall firewall add rule name="HTTP80" dir=in action=allow protocol=TCP localport=80
   ```

4. **접속**:
   - PC에서: `http://mrdaebakdinnerdelivery.com` 또는 `http://10.0.3.111`
   - 휴대폰에서: `http://10.0.3.111`

#### 주의사항
- 포트 80은 관리자 권한이 필요합니다
- 다른 웹서버(IIS 등)가 포트 80을 사용 중이면 충돌할 수 있습니다

---

### 옵션 2: 포트 5000 유지 (현재 설정)

포트 5000을 유지하면서 접속:

- PC에서: `http://mrdaebakdinnerdelivery.com:5000`
- 휴대폰에서: `http://10.0.3.111:5000`

---

### 옵션 3: Nginx 리버스 프록시 (고급)

Nginx를 사용하여 포트 80에서 받아서 포트 5000으로 프록시:

1. **Nginx 설치**
2. **설정 파일** (`nginx.conf`):
   ```nginx
   server {
       listen 80;
       server_name mrdaebakdinnerdelivery.com;

       location / {
           proxy_pass http://localhost:5000;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

---

## 🚀 현재 상태

- **빌드된 React 앱**: `server-java/src/main/resources/static`에 배치됨
- **Spring Boot 서버**: 포트 5000에서 실행
- **접속 방법**: `http://mrdaebakdinnerdelivery.com:5000` 또는 `http://10.0.3.111:5000`

## 📝 다음 단계

### 포트 번호 없이 접속하려면:

1. **포트 80으로 변경** (관리자 권한 필요):
   - `application.properties`에서 `server.port=80` 설정
   - 관리자 권한으로 서버 실행
   - 방화벽 규칙 추가

2. **서버 재시작**:
   ```bash
   cd server-java
   mvn spring-boot:run
   ```

3. **접속**:
   - `http://mrdaebakdinnerdelivery.com` (포트 번호 없음!)

---

## ⚠️ 중요 사항

- **React 앱 재빌드**: 프론트엔드 코드를 수정한 후에는 다시 빌드하고 static 폴더에 복사해야 합니다
- **빌드 스크립트**: 
  ```bash
  cd client
  npm run build
  xcopy /E /I /Y build\* ..\server-java\src\main\resources\static
  ```

- **API URL**: 빌드된 React 앱은 상대 경로(`/api`)를 사용하므로 별도 설정 불필요

---

## 🔧 문제 해결

### 포트 80이 이미 사용 중인 경우
```powershell
netstat -ano | findstr :80
```
사용 중인 프로세스 확인 후 종료하거나 다른 포트 사용

### 정적 파일이 로드되지 않는 경우
- `server-java/src/main/resources/static` 폴더 확인
- `WebConfig.java` 설정 확인
- 서버 재시작


