# 서버 시작 문제 해결 가이드

## 수정된 내용

1. **User 모델의 approval_status 필드**
   - `nullable = false` 제거 (기존 데이터베이스 호환성)
   - 기본값 "approved" 설정

2. **DataInitializer 업데이트**
   - 기존 사용자의 `approvalStatus`를 자동으로 "approved"로 업데이트
   - 새로 생성되는 계정에 `approvalStatus` 설정

3. **Order 모델 업데이트**
   - `cooking_employee_id`, `delivery_employee_id` 필드 추가

## 서버 시작 방법

1. **간단한 방법:**
   ```
   START_SERVER_SIMPLE.bat 실행
   ```

2. **디버그 모드:**
   ```
   START_SERVER_DEBUG.bat 실행
   ```

3. **로그 확인:**
   ```
   START_SERVER_WITH_LOG.bat 실행
   ```

## 문제 해결

### 서버가 즉시 종료되는 경우

1. **포트 5000이 이미 사용 중인 경우:**
   ```powershell
   netstat -ano | findstr :5000
   ```
   프로세스 ID를 확인하고 종료:
   ```powershell
   taskkill /PID [프로세스ID] /F
   ```

2. **데이터베이스 파일 권한 문제:**
   - `server-java/data/` 폴더의 권한 확인
   - 필요시 데이터베이스 파일 삭제 후 재시작 (데이터 초기화됨)

3. **컴파일 오류:**
   ```powershell
   cd server-java
   mvn clean compile
   ```

4. **Bean 정의 오류:**
   - 서버 로그에서 "BeanDefinitionOverrideException" 또는 "NoSuchBeanDefinitionException" 확인
   - 위의 수정 사항이 제대로 적용되었는지 확인

## 확인 사항

서버가 정상적으로 시작되면 다음 메시지가 보입니다:
```
Started DinnerServiceApplication in X.XXX seconds
```

서버가 시작되지 않으면:
1. 오류 메시지 확인
2. 위의 문제 해결 방법 시도
3. 필요시 데이터베이스 파일 삭제 후 재시작

