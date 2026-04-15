# Poll API 명세

기준 코드:  
`src/main/java/com/swyp/picke/domain/poll/controller/PollController.java`  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminPollController.java`

## 1. 사용자 API

### 1.1 Poll 목록
- `GET /api/v1/polls`
- 쿼리 파라미터:
  - `page` (기본값: `1`)
  - `size` (기본값: `10`)

### 1.2 Poll 상세
- `GET /api/v1/polls/{pollId}`

---

## 2. 관리자 API

### 2.1 Poll 생성
- `POST /api/v1/admin/polls`
- 요청 본문(`AdminPollCreateRequest`) 주요 필드:
  - `titlePrefix`
  - `titleSuffix`
  - `status`
  - `options[]`
    - `label` (`A`, `B`, ...)
    - `title`

### 2.2 Poll 목록
- `GET /api/v1/admin/polls`
- 쿼리 파라미터:
  - `page`
  - `size`
  - `status` (선택)

### 2.3 Poll 상세
- `GET /api/v1/admin/polls/{pollId}`

### 2.4 Poll 수정
- `PATCH /api/v1/admin/polls/{pollId}`
- 요청 본문(`AdminPollUpdateRequest`) 구조는 생성과 동일

### 2.5 Poll 삭제
- `DELETE /api/v1/admin/polls/{pollId}`

---

## 3. 필드 정책 메모

- Poll은 태그를 사용하지 않음
- Poll 투표 결과 비율은 Vote API의 `poll-vote` 경로에서 조회
