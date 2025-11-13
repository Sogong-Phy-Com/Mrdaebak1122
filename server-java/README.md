# 미스터 대박 서버 (Java Spring Boot)

Java Spring Boot로 구축된 미스터 대박 디너 서비스의 백엔드 API 서버입니다.

## 요구사항

- Java 17 이상
- Maven 3.6 이상

## 실행 방법

### 방법 1: Maven 사용

```bash
mvn spring-boot:run
```

### 방법 2: JAR 파일 빌드 후 실행

```bash
mvn clean package
java -jar target/dinner-service-1.0.0.jar
```

### 방법 3: 루트 디렉토리의 실행 스크립트 사용

Windows:
```bash
run-java-only.bat
```

Linux/Mac:
```bash
chmod +x run-java-only.sh
./run-java-only.sh
```

## 포트

기본 포트: 5000

포트를 변경하려면 `src/main/resources/application.properties` 파일에서 수정하세요.

## 데이터베이스

SQLite 데이터베이스는 `data/mrdabak.db`에 자동으로 생성됩니다.
초기 데이터(디너 타입, 메뉴 항목)는 서버 시작 시 자동으로 시드됩니다.

## API 엔드포인트

- `GET /api/health` - 헬스 체크
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `GET /api/menu/dinners` - 디너 목록
- `GET /api/menu/items` - 메뉴 항목 목록
- `GET /api/menu/serving-styles` - 서빙 스타일 목록
- `GET /api/orders` - 주문 목록 (인증 필요)
- `POST /api/orders` - 주문 생성 (인증 필요)

## 환경 변수

`src/main/resources/application.properties` 파일에서 설정을 변경할 수 있습니다.

## 빌드

```bash
mvn clean package
```

빌드된 JAR 파일은 `target/dinner-service-1.0.0.jar`에 생성됩니다.

