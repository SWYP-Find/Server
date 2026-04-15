# 배틀 주제 제안(Battle Proposal) API 명세

기준 코드: `src/main/java/com/swyp/picke/domain/battle/controller/BattleProposalController.java`,  
`src/main/java/com/swyp/picke/domain/battle/controller/AdminBattleProposalController.java`

---

## 1. 사용자 API

### 1.1 배틀 주제 제안 등록
- `POST /api/v1/battles/proposals`
- 설명: 유저가 배틀 주제를 제안합니다. 제안 시 30크레딧이 차감됩니다.
- 요청 본문(`BattleProposalRequest`) 주요 필드:
    - `category` (필수) — 카테고리 (철학, 문학, 예술, 과학, 사회, 역사)
    - `topic` (필수) — 논쟁 주제 (최대 100자)
    - `positionA` (필수) — A 입장
    - `positionB` (필수) — B 입장
    - `description` (선택) — 부가 설명 (최대 200자)

#### 성공 응답 `201 Created`
```json
{
  "statusCode": 201,
  "data": {
    "id": 1,
    "userId": 100,
    "nickname": "유저닉네임",
    "category": "철학",
    "topic": "논쟁이 될만한 주제",
    "positionA": "첫 번째 입장",
    "positionB": "두 번째 입장",
    "description": "부가 설명",
    "status": "PENDING",
    "createdAt": "2026-04-11T10:00:00"
  },
  "error": null
}
```

---

## 2. 관리자 API

기준 컨트롤러: `AdminBattleProposalController`

### 2.1 배틀 주제 제안 목록 조회
- `GET /api/v1/admin/battles/proposals`
- 쿼리 파라미터:
    - `page` (기본값: `1`)
    - `size` (기본값: `10`)
    - `status` (선택, 허용: `PENDING`, `ACCEPTED`, `REJECTED`)

#### 성공 응답 `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 100,
        "nickname": "유저닉네임",
        "category": "철학",
        "topic": "논쟁이 될만한 주제",
        "positionA": "첫 번째 입장",
        "positionB": "두 번째 입장",
        "description": "부가 설명",
        "status": "PENDING",
        "createdAt": "2026-04-11T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 1,
    "size": 10
  },
  "error": null
}
```

### 2.2 배틀 주제 채택/미채택 처리
- `PATCH /api/v1/admin/battles/proposals/{proposalId}`
- 설명: 제안된 주제를 채택하거나 거절합니다. 채택 시 제안자에게 100크레딧이 지급됩니다.
- 요청 본문(`BattleProposalReviewRequest`) 주요 필드:
    - `action` (필수) — `ACCEPT` 또는 `REJECT`

#### 성공 응답 `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "id": 1,
    "userId": 100,
    "nickname": "유저닉네임",
    "category": "철학",
    "topic": "논쟁이 될만한 주제",
    "positionA": "첫 번째 입장",
    "positionB": "두 번째 입장",
    "description": "부가 설명",
    "status": "ACCEPTED",
    "createdAt": "2026-04-11T10:00:00"
  },
  "error": null
}
```

---

## 3. 상태/정책 메모

- 제안 상태(`BattleProposalStatus`):

  | status | 설명 |
    |--------|------|
  | `PENDING` | 검토 대기 중 (기본값) |
  | `ACCEPTED` | 채택 완료 → 제안자에게 100크레딧 지급 |
  | `REJECTED` | 미채택 |

- 크레딧 정책:
    - 제안 등록 시: **-30크레딧** 차감 (`TOPIC_SUGGEST`)
    - 채택 시: **+100크레딧** 지급 (`TOPIC_ADOPTED`)
    - 크레딧 부족 시 제안 불가 (`CREDIT_NOT_ENOUGH`)
- `PENDING` 상태인 제안만 채택/미채택 처리 가능

---

## 4. 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `COMMON_INVALID_PARAMETER` | `400` | 요청 파라미터 오류 |
| `AUTH_UNAUTHORIZED` | `401` | 인증 실패 |
| `FORBIDDEN_ACCESS` | `403` | 접근 권한 없음 |
| `BATTLE_NOT_FOUND` | `404` | 존재하지 않는 제안 |
| `BATTLE_ALREADY_PUBLISHED` | `409` | 이미 처리된 제안 |
| `CREDIT_NOT_ENOUGH` | `400` | 크레딧 부족 |
| `INTERNAL_SERVER_ERROR` | `500` | 서버 오류 |