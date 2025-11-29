# GitHub 푸시 설정 가이드

## 1. GitHub에서 새 저장소 생성
1. GitHub.com에 로그인
2. "New repository" 클릭
3. Repository name: `mr-dinner-service`
4. Description: `A comprehensive Java-based dinner delivery service system`
5. Public 또는 Private 선택
6. "Create repository" 클릭

## 2. Git 설정 명령어

### 기본 설정 (한 번만 실행)
```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

### 저장소 초기화 및 파일 추가
```bash
# 현재 디렉토리에서 실행
git add .

# 커밋
git commit -m "Initial commit: Mr. Dinner Service - Complete Java OOP implementation"

# GitHub 저장소 연결 (yourusername을 실제 사용자명으로 변경)
git remote add origin https://github.com/yourusername/mr-dinner-service.git

# 메인 브랜치로 설정
git branch -M main

# GitHub에 푸시
git push -u origin main
```

## 3. 빠른 실행 명령어 (한 번에 실행)

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
git add .
git commit -m "Initial commit: Mr. Dinner Service - Complete Java OOP implementation"
git remote add origin https://github.com/yourusername/mr-dinner-service.git
git branch -M main
git push -u origin main
```

## 4. 프로젝트 구조
```
mr-dinner-service/
├── README.md                 # 프로젝트 설명서
├── LICENSE                   # MIT 라이선스
├── CONTRIBUTING.md          # 기여 가이드
├── .gitignore               # Git 무시 파일
├── pom.xml                  # Maven 설정 (선택사항)
├── build.bat               # Windows 빌드 스크립트
├── run.bat                 # Windows 실행 스크립트
└── com/
    └── mrdinner/
        ├── app/
        │   └── Main.java
        ├── domain/
        │   ├── common/
        │   ├── customer/
        │   ├── menu/
        │   ├── order/
        │   ├── staff/
        │   ├── delivery/
        │   ├── payment/
        │   └── inventory/
        └── service/
```

## 5. GitHub 저장소 설정

### Topics 추가 (저장소 페이지에서)
- `java`
- `object-oriented-programming`
- `domain-driven-design`
- `restaurant-management`
- `delivery-service`
- `educational-project`

### README.md 미리보기
GitHub에서 자동으로 README.md가 홈페이지에 표시됩니다.

## 6. 추가 기능

### GitHub Actions 설정 (선택사항)
`.github/workflows/ci.yml` 파일을 생성하여 자동 빌드/테스트 설정 가능

### Issues 템플릿
`.github/ISSUE_TEMPLATE/` 폴더에 이슈 템플릿 생성 가능

### Pull Request 템플릿
`.github/pull_request_template.md` 파일 생성 가능

## 7. 푸시 후 확인사항

1. ✅ README.md가 제대로 표시되는지 확인
2. ✅ 모든 Java 파일이 업로드되었는지 확인
3. ✅ .gitignore가 제대로 작동하는지 확인
4. ✅ 라이선스가 표시되는지 확인
5. ✅ Topics가 추가되었는지 확인

## 8. 문제 해결

### 인증 오류
```bash
# Personal Access Token 사용 (GitHub에서 생성)
git remote set-url origin https://yourusername:your_token@github.com/yourusername/mr-dinner-service.git
```

### 푸시 거부
```bash
# 강제 푸시 (주의: 기존 히스토리 삭제됨)
git push -f origin main
```

### 원격 저장소 변경
```bash
# 원격 저장소 URL 변경
git remote set-url origin https://github.com/newusername/new-repo.git
```

---

**주의**: `yourusername`을 실제 GitHub 사용자명으로 변경하세요!
