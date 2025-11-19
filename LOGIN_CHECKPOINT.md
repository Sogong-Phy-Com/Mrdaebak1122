# 로그인 체크포인트

## 상태 정보
- **커밋 해시**: `28fefb0`
- **커밋 메시지**: "Rebuild frontend to use relative API URLs for HTTPS"
- **날짜**: 2025-01-17

## 현재 상태
- ✅ 로그인 기능 정상 작동
- ✅ Mixed Content 오류 해결 (상대 경로 `/api` 사용)
- ✅ 프론트엔드가 HTTPS 환경에서 정상 작동
- 🔧 주문 기능 SQLite 잠금 오류 수정 중

## 주요 변경사항
1. 프론트엔드 API URL을 상대 경로로 변경
   - HTTPS 환경: `/api` 사용
   - 로컬 개발: `http://localhost:5000/api` 사용

2. SQLite 데이터베이스 설정 개선
   - WAL 모드 활성화
   - Busy timeout 설정 (30초)
   - Connection pool 최적화

3. 주문 생성 서비스에 재시도 로직 추가
   - SQLite 잠금 오류 시 최대 5회 재시도
   - Exponential backoff 적용

## 이 체크포인트로 돌아가기
```bash
git reset --hard 28fefb0
git push --force
```

## 참고
- 로그인은 정상 작동 중
- 주문 기능만 수정 필요 (SQLite 잠금 문제)

