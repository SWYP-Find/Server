# 투표 API 명세서

---

## 설계 메모

- **사전/사후 투표 단일 레코드 :**
  - 사전 투표와 사후 투표는 `VOTES` 테이블의 단일 레코드로 관리됩니다. `status` 필드(`NONE` → `PRE_VOTED` → `POST_VOTED`)로 진행 단계를 추적합니다.
- **투표 수정 :**
  - 투표 입장 변경은 `PATCH` 메서드를 사용합니다. `vote_type` 필드로 사전/사후 구분합니다.
- **사후 투표 응답 :**
  - 사후 투표 완료 시 `mind_changed` 여부와 전체 통계, 리워드 정보를 함께 반환합니다.

---

## 사용자 API

### `POST /api/v1/battles/{battle_id}/votes/pre`

- 시나리오 청취 전 사전 투표를 진행합니다.

#### Request Body

```json
{
  "option_id": "option_A"
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "vote_id": "vote_001",
    "status": "PRE_VOTED",
    "next_step_url": "/battles/battle_001/scenario"
  },
  "error": null
}
```

---

### `POST /api/v1/battles/{battle_id}/votes/post`

- 시나리오 청취 후 최종 사후 투표를 진행합니다. 완료 시 결과 통계와 리워드를 함께 반환합니다.

#### Request Body

```json
{
  "option_id": "option_A"
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "vote_id": "vote_001",
    "mind_changed": false,
    "status": "POST_VOTED",
    "statistics": {
      "option_A_ratio": 65,
      "option_B_ratio": 35
    },
    "reward": {
      "is_majority": true,
      "credits_earned": 10
    },
    "updated_at": "2026-03-10T16:35:00Z"
  },
  "error": null
}
```

---

### `PATCH /api/v1/battles/{battle_id}/votes`

- 기존 투표 입장을 변경합니다. `vote_type`으로 사전/사후 투표를 구분합니다.

#### Request Body

```json
{
  "vote_type": "PRE",
  "option_id": "option_B"
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "vote_id": "vote_001",
    "updated_at": "2026-03-10T16:40:00Z"
  },
  "error": null
}
```

---

### `DELETE /api/v1/battles/{battle_id}/votes`

- 투표 이력을 취소 및 삭제합니다.

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "success": true,
    "deleted_at": "2026-03-10T16:45:00Z"
  },
  "error": null
}
```

---

## 공통 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `COMMON_INVALID_PARAMETER` | `400` | 요청 파라미터 오류 |
| `COMMON_BAD_REQUEST` | `400` | 잘못된 요청 |
| `AUTH_UNAUTHORIZED` | `401` | 인증 실패 |
| `AUTH_TOKEN_EXPIRED` | `401` | 토큰 만료 |
| `FORBIDDEN_ACCESS` | `403` | 접근 권한 없음 |
| `USER_BANNED` | `403` | 제재된 사용자 |
| `INTERNAL_SERVER_ERROR` | `500` | 서버 오류 |

---

## 투표 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `VOTE_NOT_FOUND` | `404` | 존재하지 않는 투표 |
| `VOTE_ALREADY_SUBMITTED` | `409` | 이미 투표 완료 |
| `PRE_VOTE_REQUIRED` | `409` | 사전 투표 필요 |
| `POST_VOTE_REQUIRED` | `409` | 사후 투표 필요 |

---