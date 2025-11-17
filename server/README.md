# 미스터 대박 서버

Node.js + Express + TypeScript로 구축된 미스터 대박 디너 서비스의 백엔드 API 서버입니다.

## 주요 기능

- RESTful API
- JWT 인증
- SQLite 데이터베이스
- 주문 관리
- 결제 처리

## 실행

```bash
npm install
npm run dev
```

서버는 [http://localhost:5000](http://localhost:5000)에서 실행됩니다.

## 환경 변수

`.env` 파일 생성:
```
PORT=5000
JWT_SECRET=your-secret-key-change-in-production
NODE_ENV=development
```

## 데이터베이스

SQLite 데이터베이스는 `data/mrdabak.db`에 자동으로 생성됩니다.
초기 데이터(디너 타입, 메뉴 항목)는 자동으로 시드됩니다.




