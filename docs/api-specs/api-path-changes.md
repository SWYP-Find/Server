# API 경로 변경/추가 요약 (프론트 전달용)

아래는 **사용자용 API 기준**으로, 기존 대비 바뀐 경로와 새로 분리/추가된 경로를 정리한 문서입니다.  
관리자(`admin`) 경로는 제외했습니다.

## 1. 변경된 경로 (기존 → 현재)

### 1.1 콘텐츠 조회

| 기존(통합 Battle 타입 분기) | 현재(도메인 분리) |
|---|---|
| `GET /api/v1/battles?type=QUIZ` | `GET /api/v1/quizzes` |
| `GET /api/v1/battles/{battleId}` (QUIZ 상세) | `GET /api/v1/quizzes/{quizId}` |
| `GET /api/v1/battles?type=POLL` | `GET /api/v1/polls` |
| `GET /api/v1/battles/{battleId}` (POLL 상세) | `GET /api/v1/polls/{pollId}` |

### 1.2 투표 제출/조회

| 기존(통합 투표 처리) | 현재(도메인별 투표) |
|---|---|
| `POST /api/v1/battles/{battleId}/votes/...` (퀴즈 선택 제출에 재사용) | `POST /api/v1/battles/{battleId}/quiz-vote` |
| `GET /api/v1/battles/{battleId}/votes/me` (퀴즈 결과 확인에 재사용) | `GET /api/v1/battles/{battleId}/quiz-vote/me` |
| `POST /api/v1/battles/{battleId}/votes/...` (Poll 선택 제출에 재사용) | `POST /api/v1/battles/{battleId}/poll-vote` |
| `GET /api/v1/battles/{battleId}/votes/me` (Poll 결과 확인에 재사용) | `GET /api/v1/battles/{battleId}/poll-vote/me` |

## 2. 추가된 경로 (프론트에서 새로 호출 필요)

- `GET /api/v1/quizzes`
- `GET /api/v1/quizzes/{quizId}`
- `GET /api/v1/polls`
- `GET /api/v1/polls/{pollId}`
- `POST /api/v1/battles/{battleId}/quiz-vote`
- `GET /api/v1/battles/{battleId}/quiz-vote/me`
- `POST /api/v1/battles/{battleId}/poll-vote`
- `GET /api/v1/battles/{battleId}/poll-vote/me`

## 3. 유지되는 경로 (변경 없음)

- 배틀 전용 투표:
  - `POST /api/v1/battles/{battleId}/votes/pre`
  - `POST /api/v1/battles/{battleId}/votes/post`
  - `GET /api/v1/battles/{battleId}/vote-stats`
  - `GET /api/v1/battles/{battleId}/votes/me`

- 배틀 조회:
  - `GET /api/v1/battles`
  - `GET /api/v1/battles/{battleId}`
  - `GET /api/v1/battles/today`

## 4. 참고

- `quiz-vote`, `poll-vote` 경로의 Path Variable 이름은 코드상 `battleId`로 되어 있지만, 내부적으로는 각각 `quizId`, `pollId`로 처리됩니다.
