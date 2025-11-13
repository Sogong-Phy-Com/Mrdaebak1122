# 미스터 대박 디너 서비스 (Java 버전)

Java Spring Boot로 구축된 미스터 대박 디너 서비스입니다.

## 빠른 시작

### Windows

1. `run.bat` 파일을 더블클릭하거나 명령 프롬프트에서 실행:
```bash
run.bat
```

### Linux/Mac

1. 실행 권한 부여:
```bash
chmod +x run.sh
```

2. 실행:
```bash
./run.sh
```

## 수동 실행

### 백엔드만 실행

Windows:
```bash
run-java-only.bat
```

Linux/Mac:
```bash
chmod +x run-java-only.sh
./run-java-only.sh
```

### 프론트엔드만 실행

```bash
cd client
npm install
npm start
```

## 요구사항

### 백엔드
- Java 17 이상
- Maven 3.6 이상

### 프론트엔드
- Node.js 16 이상
- npm 또는 yarn

## 프로젝트 구조

```
MrDaeBak/
├── server-java/          # Java Spring Boot 백엔드
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/mrdabak/dinnerservice/
│   │       │       ├── controller/    # REST 컨트롤러
│   │       │       ├── service/       # 비즈니스 로직
│   │       │       ├── repository/    # 데이터 접근
│   │       │       ├── model/         # 엔티티
│   │       │       ├── dto/           # 데이터 전송 객체
│   │       │       └── config/       # 설정
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── client/                # React 프론트엔드
├── run.bat                # 전체 서비스 실행 (Windows)
├── run.sh                 # 전체 서비스 실행 (Linux/Mac)
├── run-java-only.bat      # 백엔드만 실행 (Windows)
└── run-java-only.sh       # 백엔드만 실행 (Linux/Mac)
```

## 주요 기능

- ✅ 회원가입/로그인 (JWT 인증)
- ✅ 디너 메뉴 조회
- ✅ 주문 생성 및 관리
- ✅ 단골 고객 할인 (10%)
- ✅ 음성 인식 주문 (프론트엔드)
- ✅ 주문 내역 조회

## 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** (JWT 인증)
- **Spring Data JPA**
- **SQLite** 데이터베이스
- **Maven** 빌드 도구

### Frontend
- **React 18**
- **TypeScript**
- **Axios** HTTP 클라이언트

## API 엔드포인트

### 인증
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인

### 메뉴
- `GET /api/menu/dinners` - 디너 목록
- `GET /api/menu/items` - 메뉴 항목 목록
- `GET /api/menu/serving-styles` - 서빙 스타일 목록

### 주문
- `GET /api/orders` - 주문 목록 (인증 필요)
- `POST /api/orders` - 주문 생성 (인증 필요)

## 데이터베이스

SQLite 데이터베이스는 `server-java/data/mrdabak.db`에 자동으로 생성됩니다.
초기 데이터는 서버 시작 시 자동으로 시드됩니다.

## 문제 해결

### Java 버전 확인
```bash
java -version
```
Java 17 이상이 필요합니다.

### Maven 버전 확인
```bash
mvn -version
```

### 포트 충돌
기본 포트 5000이 사용 중인 경우, `server-java/src/main/resources/application.properties`에서 포트를 변경하세요.

## 빌드

### 백엔드 JAR 파일 빌드
```bash
cd server-java
mvn clean package
```

빌드된 JAR 파일: `server-java/target/dinner-service-1.0.0.jar`

### 프론트엔드 빌드
```bash
cd client
npm run build
```

빌드된 파일: `client/build/`

## 라이선스

이 프로젝트는 교육 목적으로 제작되었습니다.

