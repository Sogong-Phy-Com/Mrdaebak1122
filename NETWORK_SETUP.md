# 네트워크 접근 설정 가이드

이 가이드는 같은 와이파이에 있는 다른 기기에서 이 애플리케이션에 접근할 수 있도록 설정하는 방법을 설명합니다.

## 1. 백엔드 서버 설정

### application.properties 설정
`server-java/src/main/resources/application.properties` 파일에 다음 설정이 포함되어 있습니다:
```
server.address=0.0.0.0
server.port=5000
```

이 설정으로 서버는 모든 네트워크 인터페이스에서 요청을 받을 수 있습니다.

## 2. 포트 충돌 해결

포트 5000이 이미 사용 중인 경우:

### Windows에서 포트 사용 확인
```powershell
netstat -ano | findstr :5000
```

### 프로세스 종료
```powershell
taskkill /PID <PID번호> /F
```

예: `taskkill /PID 7996 /F`

## 3. Windows 방화벽 설정

관리자 권한으로 PowerShell을 열고 다음 명령을 실행:

```powershell
netsh advfirewall firewall add rule name="Spring5000" dir=in action=allow protocol=TCP localport=5000
```

포트를 변경한 경우 해당 포트 번호로 변경하세요.

## 4. PC의 IP 주소 확인

```powershell
ipconfig | findstr /i "IPv4"
```

예시 출력:
```
IPv4 Address. . . . . . . . . . : 192.168.45.246
```

이 IP 주소를 기록해두세요.

## 5. 프론트엔드 설정

### .env 파일 생성
`client` 폴더에 `.env` 파일을 생성하고 다음 내용을 추가:

```
REACT_APP_API_URL=http://localhost:5000/api
DANGEROUSLY_DISABLE_HOST_CHECK=true
```

**참고**: `DANGEROUSLY_DISABLE_HOST_CHECK=true`는 네트워크에서 접근할 수 있도록 호스트 체크를 비활성화합니다.

### 다른 기기에서 접근하는 경우
같은 네트워크의 다른 기기에서 접근하려면, 해당 기기에서 접속할 때는 PC의 IP 주소를 사용해야 합니다.

예: `http://192.168.45.246:5000/api`

프론트엔드 코드는 이미 `process.env.REACT_APP_API_URL`을 사용하도록 설정되어 있습니다.

## 6. CORS 설정

백엔드의 CORS 설정은 `application.properties`에서 관리됩니다. 로컬 네트워크 접근을 위해 동적으로 처리되도록 설정되어 있습니다.

## 7. 접속 방법

### 같은 PC에서 접속
- 프론트엔드: `http://localhost:3000`
- 백엔드 API: `http://localhost:5000/api`

### 같은 네트워크의 다른 기기에서 접속
1. PC의 IP 주소 확인 (예: 192.168.45.246)
2. 다른 기기의 브라우저에서 `http://192.168.45.246:3000` 접속
3. 프론트엔드가 백엔드 API를 호출할 때 자동으로 같은 IP를 사용

**주의**: 프론트엔드도 다른 기기에서 접근 가능하게 하려면, React 개발 서버도 0.0.0.0으로 바인딩해야 합니다.

### React 개발 서버를 네트워크에서 접근 가능하게 설정
`package.json`의 start 스크립트를 수정:
```json
"start": "set HOST=0.0.0.0 && react-scripts start"
```

또는 환경 변수로:
```powershell
$env:HOST="0.0.0.0"
npm start
```

## 8. 문제 해결

### 서버가 시작되지 않는 경우
- 포트가 이미 사용 중인지 확인
- 방화벽 규칙이 제대로 추가되었는지 확인
- `server.address=0.0.0.0` 설정이 있는지 확인

### 다른 기기에서 접속이 안 되는 경우
- PC와 다른 기기가 같은 와이파이에 연결되어 있는지 확인
- 방화벽 규칙이 추가되었는지 확인
- IP 주소가 올바른지 확인
- 백엔드 서버가 실행 중인지 확인

### CORS 오류가 발생하는 경우
- `application.properties`의 CORS 설정 확인
- 프론트엔드가 올바른 API URL을 사용하는지 확인

## 9. 다음 단계 (클라우드 배포)

로컬 네트워크 접근이 잘 작동한다면, 다음 단계로 클라우드에 배포할 수 있습니다:
- AWS EC2
- Oracle Cloud Free Tier
- Render
- Railway
- Heroku

클라우드 배포 시 HTTPS, 도메인, 프로덕션 데이터베이스(MySQL/PostgreSQL) 설정이 필요합니다.

