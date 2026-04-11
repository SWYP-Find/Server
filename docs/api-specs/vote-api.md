# 투표(Vote) API 명세

기준 코드: `src/main/java/com/swyp/picke/domain/vote/controller/VoteController.java`

## 1. 퀴즈 투표

### 1.1 퀴즈 응답 제출
- `POST /api/v1/battles/{battleId}/quiz-vote`
- 요청 본문:
```json
{
  "optionId": 1
}
```

### 1.2 내 퀴즈 투표 조회
- `GET /api/v1/battles/{battleId}/quiz-vote/me`

> 참고: 현재 경로 변수 이름은 `battleId`지만 내부적으로 `quizId`로 사용됩니다.

---

## 2. Poll 투표

### 2.1 Poll 선택 제출
- `POST /api/v1/battles/{battleId}/poll-vote`
- 요청 본문:
```json
{
  "optionId": 1
}
```

### 2.2 내 Poll 투표 조회
- `GET /api/v1/battles/{battleId}/poll-vote/me`

> 참고: 현재 경로 변수 이름은 `battleId`지만 내부적으로 `pollId`로 사용됩니다.

---

## 3. 배틀 사전/사후 투표

### 3.1 사전 투표
- `POST /api/v1/battles/{battleId}/votes/pre`
- 요청 본문:
```json
{
  "optionId": 1
}
```

### 3.2 사후 투표
- `POST /api/v1/battles/{battleId}/votes/post`
- 요청 본문:
```json
{
  "optionId": 1
}
```

### 3.3 TTS 청취 완료
- `POST /api/v1/battles/{battleId}/votes/tts-complete`

### 3.4 배틀 투표 통계
- `GET /api/v1/battles/{battleId}/vote-stats`

### 3.5 내 배틀 투표 이력
- `GET /api/v1/battles/{battleId}/votes/me`

---

## 4. 관리자 투표 데이터 정리 API

### 4.1 배틀 투표 기록 삭제
- `DELETE /api/v1/admin/votes/battle/{battleId}`

### 4.2 퀴즈 투표 기록 삭제
- `DELETE /api/v1/admin/votes/quiz/{battleId}`

### 4.3 Poll 투표 기록 삭제
- `DELETE /api/v1/admin/votes/poll/{battleId}`

---

## 5. 응답 DTO 메모

- 퀴즈 투표 응답: `QuizVoteResponse`
  - `selectedOptionId`, `totalCount`, `stats[].isCorrect` 포함
- Poll 투표 응답: `PollVoteResponse`
  - `selectedOptionId`, `totalCount`, `stats[].ratio` 포함
- 배틀 투표 응답: `VoteResultResponse`, `VoteStatsResponse`, `MyVoteResponse`
