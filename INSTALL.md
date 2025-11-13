# 설치 가이드

## 사전 요구사항

### 백엔드 (Java)
- **Java 17 이상** 설치 필요
- **Maven 3.6 이상** 설치 필요

Java 설치 확인:
```bash
java -version
```

Maven 설치 확인:
```bash
mvn -version
```

### 프론트엔드
- **Node.js 16 이상** 설치 필요
- **npm** (Node.js와 함께 설치됨)

Node.js 설치 확인:
```bash
node -v
npm -v
```

## 빠른 시작

### Windows 사용자

1. `run.bat` 파일을 더블클릭하거나 명령 프롬프트에서 실행:
```bash
run.bat
```

### Linux/Mac 사용자

1. 실행 권한 부여:
```bash
chmod +x run.sh
```

2. 실행:
```bash
./run.sh
```

## 수동 설치 및 실행

### 1. 백엔드 설정

```bash
cd server-java
mvn clean install
mvn spring-boot:run
```

백엔드는 `http://localhost:5000`에서 실행됩니다.

### 2. 프론트엔드 설정

새 터미널에서:
```bash
cd client
npm install
npm start
```

프론트엔드는 `http://localhost:3000`에서 실행됩니다.

## 문제 해결

### Java가 설치되지 않은 경우

1. [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 또는 [OpenJDK](https://openjdk.org/) 다운로드
2. Java 17 이상 버전 설치
3. 환경 변수 설정 (JAVA_HOME)

### Maven이 설치되지 않은 경우

1. [Apache Maven](https://maven.apache.org/download.cgi) 다운로드
2. 압축 해제 후 환경 변수 설정 (MAVEN_HOME, PATH)

### Node.js가 설치되지 않은 경우

1. [Node.js 공식 사이트](https://nodejs.org/)에서 다운로드
2. LTS 버전 설치 권장

### 포트 충돌

기본 포트가 사용 중인 경우:

**백엔드 포트 변경:**
`server-java/src/main/resources/application.properties` 파일에서:
```properties
server.port=5001
```

**프론트엔드 포트 변경:**
`client/package.json`에서 `"start"` 스크립트 수정:
```json
"start": "PORT=3001 react-scripts start"
```

또는 환경 변수:
```bash
PORT=3001 npm start
```

## 빌드

### 백엔드 JAR 파일 빌드

```bash
cd server-java
mvn clean package
```

빌드된 JAR 파일: `server-java/target/dinner-service-1.0.0.jar`

실행:
```bash
java -jar target/dinner-service-1.0.0.jar
```

### 프론트엔드 빌드

```bash
cd client
npm run build
```

빌드된 파일: `client/build/`

## 데이터베이스

SQLite 데이터베이스는 `server-java/data/mrdabak.db`에 자동으로 생성됩니다.
초기 데이터는 서버 시작 시 자동으로 시드됩니다.

데이터베이스를 초기화하려면 `server-java/data/` 디렉토리를 삭제하고 서버를 다시 시작하세요.

