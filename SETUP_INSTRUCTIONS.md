# 네트워크 접근 설정 완료 가이드

## ✅ 완료된 설정

1. **백엔드 서버 바인딩**: `server.address=0.0.0.0` 설정 완료
2. **포트 충돌 해결**: 포트 5000을 사용하던 프로세스 종료 완료
3. **CORS 설정**: 로컬 네트워크 IP 범위 허용 완료
4. **방화벽 규칙**: 포트 5000 허용 규칙 추가 완료

## 📋 추가 작업 필요

### 1. 프론트엔드 .env 파일 생성

`client` 폴더에 `.env` 파일을 생성하고 다음 내용을 추가하세요:

```
REACT_APP_API_URL=http://localhost:5000/api
DANGEROUSLY_DISABLE_HOST_CHECK=true
```

**다른 기기에서 접근하는 경우**: PC의 IP 주소를 사용하세요.
예: `REACT_APP_API_URL=http://192.168.45.246:5000/api`

**참고**: `DANGEROUSLY_DISABLE_HOST_CHECK=true`는 네트워크에서 접근할 수 있도록 호스트 체크를 비활성화합니다.

### 2. React 개발 서버를 네트워크에서 접근 가능하게 설정

현재 `package.json`의 start 스크립트가 `HOST=0.0.0.0`으로 설정되어 있습니다.

**로컬에서만 접근하려면**:
```bash
npm run start:local
```

**네트워크에서 접근 가능하게 하려면**:
```bash
npm start
```

### 3. 서버 재시작

백엔드 서버를 재시작하세요:
```bash
cd server-java
mvn spring-boot:run
```

## 🌐 접속 방법

### 같은 PC에서 접속
- 프론트엔드: `http://localhost:3000`
- 백엔드 API: `http://localhost:5000/api`

### 같은 네트워크의 다른 기기에서 접속

1. **PC의 IP 주소 확인** (이미 확인됨: `192.168.45.246`)
2. **다른 기기의 브라우저에서 접속**: `http://192.168.45.246:3000`
3. **프론트엔드 .env 파일 수정**: `REACT_APP_API_URL=http://192.168.45.246:5000/api`
4. **React 서버 재시작**: `npm start`

## 🔧 문제 해결

### 서버가 시작되지 않는 경우
```powershell
# 포트 사용 확인
netstat -ano | findstr :5000

# 프로세스 종료 (필요한 경우)
taskkill /PID <PID번호> /F
```

### 방화벽 규칙 확인
```powershell
netsh advfirewall firewall show rule name="Spring5000"
```

### 다른 기기에서 접속이 안 되는 경우
1. PC와 다른 기기가 같은 와이파이에 연결되어 있는지 확인
2. 방화벽 규칙이 추가되었는지 확인
3. 백엔드 서버가 실행 중인지 확인
4. 프론트엔드 .env 파일의 API URL이 올바른지 확인

## 📝 참고사항

- IP 주소는 네트워크에 따라 변경될 수 있습니다. IP가 변경되면 `.env` 파일도 업데이트하세요.
- 개발 환경에서는 보안을 위해 프로덕션 배포 시 CORS 설정을 제한하세요.
- 클라우드 배포 시 HTTPS, 도메인, 프로덕션 데이터베이스 설정이 필요합니다.

## 🚀 다음 단계

로컬 네트워크 접근이 잘 작동한다면, 다음 단계로 클라우드에 배포할 수 있습니다:
- AWS EC2
- Oracle Cloud Free Tier
- Render
- Railway
- Heroku

자세한 내용은 `NETWORK_SETUP.md` 파일을 참고하세요.

