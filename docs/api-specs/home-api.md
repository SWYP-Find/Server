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
- `mostDiscussedBattles`: 댓글 수가 많은 배틀 목록
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
  - `imageUrl` (철학자 아이콘 이미지 URL, 미등록 시 `null`)

---

## 4. 정렬/노출 메모

- 오늘의 퀴즈/투표는 서버에서 조회 및 정렬을 확정해 응답
- 옵션 순서는 `displayOrder -> label -> id` 기준 오름차순으로 고정
- `bestBattles[].badgeLabel`은 `BEST`, `trendingBattles[].badgeLabel`은 `TRENDING`, `mostDiscussedBattles[].badgeLabel`은 `최다 토론`이다.
- 배틀 카드 응답은 태그와 소요 시간(초)을 포함한다. 에디터 픽에도 `audioDuration`을 제공한다.
- `mostDiscussedBattles`는 공개 배틀 중 댓글 수 내림차순으로 최대 3건이며 댓글이 없는 배틀은 제외한다.
