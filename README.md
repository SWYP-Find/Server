# 🚀 SWYP APP 4th Server

## 🌿 Git Flow Strategy

### main (배포/Production) 브랜치
- 최종 배포 가능한 상태만 유지
- dev에서 충분히 테스트된 내용만 Merge

### dev (개발/Dev) 브랜치
- 다음 배포를 위해 개발된 기능들이 모이는 곳
- 항상 빌드 및 배포 가능한 상태 유지

### feat/#1 (기능/Feature) 브랜치
- dev에서 분기하여 새로운 기능 개발
- 작업 완료 후 dev로 PR

## 🪵 Branch Naming Convention
- 형식: `{header}/{issue-number}`
- 예시: `feat/#5` `fix/#11` `refactor/#23`

## 💡 Issue & Pull Request (PR) Rules

### ✅ Issue 체크리스트
- Assignees에 본인을 선택했나요?
- 컨벤션에 맞는 Type 선택

### ✅ PR 체크리스트
- Reviewer에 팀원들을 선택했나요?
- Assignees에 본인을 선택했나요?
- 컨벤션에 맞는 Labels 선택
- Development에 이슈 연동
- Merge 브랜치 확인 (dev)
- 컨벤션 준수
- 로컬 실행 시 에러 없음
- 팀원에게 PR 공유

### 📢 공유 및 Merge 조건
- 환경 설정 파일 변경 시 하단 URL에 업데이트 (application.yml, .env 등)
    - https://www.notion.so/30c383619265807d9110c1e7711196ac?source=copy_link
- PR 생성 직후 디스코드에 링크 공유
- 1명 이상이 확인 후 리뷰 & Merge 진행
- Merge: Squash and Merge 사용
    - 제목 예시) #2 [chore] 이슈 및 PR 템플릿 추가
- Merge 완료 후 브랜치 자동 삭제

## ⌨️ Commit Message Convention

| Category | Type | Description |
|----------|------|------------|
| 🚀 Feature | ✨ Feat | 새로운 기능 추가 |
| 🚀 Feature | 💥 Breaking Change | 하위 호환성 없는 큰 API 변경 |
| 🐞 Bug | 🐛 Fix | 버그 수정 |
| 🐞 Bug | 🚑 Hotfix | 긴급 치명적 버그 수정 |
| 🛠 Task | 🔨 Chore | 빌드 업무, 패키지 설정, .gitignore 수정 등 |
| 🛠 Task | 👷 CICD | CI/CD 파이프라인 및 배포 관련 |
| 🛠 Task | ♻️ Refactor | 코드 리팩토링 (기능 변경 없음) |
| 🛠 Task | 💄 Style | 코드 포맷팅, 세미콜론 누락 등 (로직 변경 없음) |
| 🛠 Task | 💡 Comment | 주석 추가 및 변경 |
| 🛠 Task | 🏷️ Rename | 파일/폴더명 수정 또는 이동 |
| 🛠 Task | 🔥 Remove | 파일 삭제 |
| 🛠 Task | 🎉 Init | 프로젝트 초기 세팅 |
| 🛠 Task | 🔀 Merge | 브랜치 병합 |
| 🛠 Task | 🧪 Test | 테스트 코드 추가/수정 |
| 🛠 Task | 📚 Docs | 문서 수정 (README, Wiki 등) |

## 🔄 Work Flow

### 1. 최신 코드 불러오기 (Update)
- 작업 전 항상 `dev` 브랜치 최신 상태 유지
- 작업 중일 때 (Stash 활용):
```
git stash               # 작업 중인 코드 임시 저장
git pull origin dev     # 원격 dev 브랜치 내용 당겨오기
git stash pop           # 임시 저장했던 코드 다시 가져오기
```
- 작업 중인 내용이 없을 때:
```
git pull origin dev     # 원격 dev 브랜치 내용 당겨오기
```
### 2. 브랜치 생성
- 작업 목적에 맞는 독립 브랜치 생성 후 작업 시작
```
# 형식: {type}/#{issue-number}
git checkout -b feat/#1
```
### 3. 작업 및 커밋 (Commit)
```
- IDE Commit UI 활용
- git add . 또는 논리적 단위 커밋
- 커밋 메시지 : Commit Message Convention 준수
```

### 4. 푸시 (Push)
- 작업 완료 후 브랜치를 원격 저장소에 업로드
```
git push origin {생성한-브랜치-명}
# 예: git push origin feat/#1
```

## 📂 Project Structure

```
com.swyp.app
├── AppApplication.java
│
├── domain
│
└── infra
```