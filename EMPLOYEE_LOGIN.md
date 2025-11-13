# 직원/관리자 로그인 가이드

## 기본 직원 계정

서버 시작 시 자동으로 생성되는 기본 직원 계정:

### 관리자 계정
- **이메일**: `admin@mrdabak.com`
- **비밀번호**: `admin123`
- **역할**: employee

### 직원 계정 1
- **이메일**: `employee1@mrdabak.com`
- **비밀번호**: `emp123`
- **역할**: employee

### 직원 계정 2
- **이메일**: `employee2@mrdabak.com`
- **비밀번호**: `emp123`
- **역할**: employee

## 로그인 방법

1. 웹사이트에서 로그인 페이지 접속
2. 위의 직원 계정 중 하나로 로그인
3. 직원으로 로그인하면 자동으로 **직원 대시보드**로 이동합니다

## 직원 대시보드 기능

- 모든 주문 목록 조회
- 주문 상태 필터링 (대기 중, 조리 중, 준비 완료, 배달 중, 배달 완료)
- 주문 상태 업데이트
- 주문 배정

## 새로운 직원 계정 생성

### 방법 1: API 사용 (직원 권한 필요)

직원으로 로그인한 후:

```bash
POST /api/admin/create-employee
Content-Type: application/json
Authorization: Bearer {직원_토큰}

{
  "email": "newemployee@mrdabak.com",
  "password": "password123",
  "name": "새 직원",
  "address": "서울시 강남구",
  "phone": "010-1234-5678"
}
```

### 방법 2: 데이터베이스 직접 수정

SQLite 데이터베이스에서 직접 사용자 생성:

```sql
INSERT INTO users (email, password, name, address, phone, role)
VALUES ('newemployee@mrdabak.com', '{암호화된_비밀번호}', '새 직원', '서울시 강남구', '010-1234-5678', 'employee');
```

**주의**: 비밀번호는 BCrypt로 암호화되어야 합니다.

## 보안 주의사항

- 프로덕션 환경에서는 기본 계정 비밀번호를 반드시 변경하세요
- 직원 계정 생성은 관리자 권한이 필요합니다
- JWT 토큰은 7일간 유효합니다

