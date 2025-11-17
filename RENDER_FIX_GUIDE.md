# Render 배포 오류 해결 가이드

## 🔴 현재 문제
Render가 프로젝트를 Node.js로 인식하여 `node server/index.js`를 실행하려고 합니다.
하지만 이 프로젝트는 **Java Spring Boot** 프로젝트입니다.

## ✅ 해결 방법

### 방법 1: Render 대시보드에서 수동 설정 (권장)

1. **Render 대시보드 접속**
   - https://dashboard.render.com
   - 배포 중인 서비스 클릭

2. **Settings 탭으로 이동**

3. **다음 설정 변경:**

   **Runtime:**
   - 현재: `Node` 또는 `Auto-detect`
   - 변경: `Java` 선택

   **Build Command:**
   ```
   cd server-java && mvn clean package -DskipTests
   ```

   **Start Command:**
   ```
   cd server-java && java -jar target/dinner-service-1.0.0.jar
   ```

4. **Environment Variables 추가:**
   - `SPRING_PROFILES_ACTIVE` = `production`
   - `JWT_SECRET` = `your-very-strong-secret-key-here` (강력한 키 사용)

5. **Health Check Path:**
   ```
   /api/health
   ```

6. **저장 후 재배포**
   - "Manual Deploy" → "Deploy latest commit" 클릭

---

### 방법 2: render.yaml 파일 사용

루트 디렉토리에 `render.yaml` 파일이 생성되었습니다.
이 파일을 GitHub에 푸시하면 Render가 자동으로 인식합니다.

**GitHub에 푸시:**
```bash
cd C:\Users\pando\Desktop\MrDaeBak
git add render.yaml
git commit -m "Add render.yaml for Java deployment"
git push
```

**그 다음:**
1. Render 대시보드에서 서비스 삭제
2. "New +" → "Web Service" 클릭
3. GitHub 저장소 연결
4. **"Infrastructure as Code"** 옵션 선택
5. `render.yaml` 파일이 자동으로 인식됨

---

## 🔍 문제 원인

1. **루트 디렉토리에 `package.json`이 있음**
   - `"main": "server/index.js"` 설정 때문에 Render가 Node.js로 인식
   - 하지만 실제로는 Java 프로젝트

2. **`render.yaml`이 루트에 없었음**
   - `server-java/render.yaml`에만 있었음
   - Render는 루트 디렉토리의 `render.yaml`만 인식

---

## 📝 체크리스트

배포 전 확인:
- [ ] Render 대시보드에서 Runtime = `Java` 설정
- [ ] Build Command = `cd server-java && mvn clean package -DskipTests`
- [ ] Start Command = `cd server-java && java -jar target/dinner-service-1.0.0.jar`
- [ ] 환경 변수 설정 (SPRING_PROFILES_ACTIVE, JWT_SECRET)
- [ ] Health Check Path = `/api/health`
- [ ] `render.yaml` 파일이 루트 디렉토리에 있음
- [ ] GitHub에 푸시 완료

---

## 🚀 빠른 해결 (권장)

**가장 빠른 방법:**

1. Render 대시보드 → 서비스 → Settings
2. Runtime을 **Java**로 변경
3. Build Command 입력: `cd server-java && mvn clean package -DskipTests`
4. Start Command 입력: `cd server-java && java -jar target/dinner-service-1.0.0.jar`
5. Environment Variables 추가
6. "Save Changes" 클릭
7. "Manual Deploy" → "Deploy latest commit"

이렇게 하면 즉시 해결됩니다!

