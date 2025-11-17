# 설치 및 실행 가이드

## 빠른 시작

### 1. 의존성 설치

```bash
npm run install-all
```

이 명령은 루트, 서버, 클라이언트의 모든 의존성을 설치합니다.

### 2. 개발 서버 실행

```bash
npm run dev
```

이 명령은 백엔드 서버(포트 5000)와 프론트엔드 개발 서버(포트 3000)를 동시에 실행합니다.

### 3. 브라우저에서 접속

- 프론트엔드: http://localhost:3000
- 백엔드 API: http://localhost:5000/api

## 개별 실행

### 백엔드만 실행

```bash
cd server
npm install
npm run dev
```

### 프론트엔드만 실행

```bash
cd client
npm install
npm start
```

## 데이터베이스

SQLite 데이터베이스는 `server/data/mrdabak.db`에 자동으로 생성됩니다.
초기 데이터(디너 타입, 메뉴 항목)는 서버 시작 시 자동으로 시드됩니다.

## 환경 변수 (선택사항)

### 서버 환경 변수 (`server/.env`)

```
PORT=5000
JWT_SECRET=your-secret-key-change-in-production
NODE_ENV=development
```

### 클라이언트 환경 변수 (`client/.env`)

```
REACT_APP_API_URL=http://localhost:5000/api
```

## 테스트 계정 생성

1. 웹사이트에서 회원가입
2. 또는 API를 통해 직접 생성:

```bash
curl -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "테스트 사용자",
    "address": "서울시 강남구",
    "phone": "010-1234-5678"
  }'
```

## 직원 계정 생성

직원 계정은 데이터베이스에서 직접 수정하거나, 회원가입 후 데이터베이스에서 role을 'employee'로 변경해야 합니다.

## 문제 해결

### 포트가 이미 사용 중인 경우

다른 포트를 사용하거나 기존 프로세스를 종료하세요.

### 데이터베이스 오류

`server/data` 디렉토리를 삭제하고 서버를 다시 시작하면 데이터베이스가 재생성됩니다.

### 음성 인식이 작동하지 않는 경우

- Chrome 또는 Edge 브라우저 사용 (Web Speech API 지원)
- HTTPS 또는 localhost에서만 작동
- 마이크 권한 확인




