# 홈(Home) API 명세

기준 코드: `src/main/java/com/swyp/picke/domain/home/controller/HomeController.java`,  
`src/main/java/com/swyp/picke/domain/home/service/HomeService.java`

## 1. 홈 조회

- `GET /api/v1/home`
- 설명: 홈 화면 전체 섹션 데이터를 한 번에 조회

### 응답 구조 (`HomeResponse`)
- `newNotice`: 새 공지 존재 여부
- `editorPicks`: 에디터 픽 배틀 목록
- `trendingBattles`: 트렌딩 배틀 목록
- `bestBattles`: 베스트 배틀 목록
- `todayQuizzes`: 오늘의 퀴즈 목록
- `todayVotes`: 오늘의 투표(Poll) 목록
- `newBattles`: 신규 배틀 목록

---

## 2. todayQuizzes 응답 필드

`HomeTodayQuizResponse`

- `battleId` (실제 Quiz ID)
- `title`
- `summary` (고정 문구)
- `participantsCount`
- `itemA`
- `itemADesc`
- `isCorrectA`
- `itemB`
- `itemBDesc`
- `isCorrectB`

---

## 3. todayVotes 응답 필드

`HomeTodayVoteResponse`

- `battleId` (실제 Poll ID)
- `titlePrefix`
- `titleSuffix`
- `summary` (고정 문구)
- `participantsCount`
- `options[]`
  - `label`
  - `title`

---

## 4. 정렬/노출 메모

- 오늘의 퀴즈/투표는 서버에서 조회 및 정렬을 확정해 응답
- 옵션 순서는 `displayOrder -> label -> id` 기준 오름차순으로 고정
