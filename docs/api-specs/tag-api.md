# 태그 API 명세서

---

## 설계 메모

- **태그 구조 :**
  - 태그는 별도 `TAGS` 테이블로 관리하며, `BATTLE_TAGS` 중간 테이블을 통해 배틀과 N:M 관계를 가집니다.
- **태그 목록 조회 :**
  - 관리자가 배틀에 태그를 붙일 때 선택 목록 제공 및 클라이언트 필터 UI 구성에 활용됩니다.
- **태그 기반 배틀 필터링 :**
  - `tag_id` 쿼리 파라미터로 특정 태그가 붙은 배틀 목록을 조회합니다.

---

## 사용자 API

### `GET /api/v1/tags`

- 전체 태그 목록을 조회합니다. 클라이언트 필터 UI 구성 및 관리자 태그 선택에 활용됩니다.

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      { "tag_id": "tag_001", "name": "사회" },
      { "tag_id": "tag_002", "name": "철학" },
      { "tag_id": "tag_003", "name": "롤스" },
      { "tag_id": "tag_004", "name": "니체" },
      { "tag_id": "tag_005", "name": "경제" },
      { "tag_id": "tag_006", "name": "윤리" }
    ],
    "total_count": 6
  },
  "error": null
}
```

---

### `GET /api/v1/battles?tag_id={tag_id}`

- 특정 태그가 붙은 배틀 목록을 조회합니다.

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:----:|------|
| `tag_id` | string | ✅ | 필터링할 태그 ID |

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "tag": { "tag_id": "tag_002", "name": "철학" },
    "items": [
      {
        "battle_id": "battle_001",
        "title": "드라마 <레이디 두아>, 원가 18만원 명품은 사기인가?",
        "summary": "18만 원짜리 가방을 1억에 판 주인공, 사기꾼일까 예술가일까?",
        "thumbnail_url": "https://cdn.pique.app/battle/hot-001.png",
        "tags": [
          { "tag_id": "tag_001", "name": "사회" },
          { "tag_id": "tag_002", "name": "철학" }
        ],
        "participants_count": 2148,
        "audio_duration": 420,
        "options": [
          { "option_id": "option_A", "label": "A", "title": "사기다 (롤스)" },
          { "option_id": "option_B", "label": "B", "title": "사기가 아니다 (니체)" }
        ],
        "user_vote_status": "NONE"
      }
    ],
    "total_count": 1
  },
  "error": null
}
```

---

## 관리자 API

### `POST /api/v1/admin/tags`

- 새 태그를 생성합니다.

#### Request Body

```json
{
  "name": "정치"
}
```

#### 성공 응답 `201 Created`

```json
{
  "statusCode": 201,
  "data": {
    "tag_id": "tag_007",
    "name": "정치",
    "created_at": "2026-03-10T09:00:00Z"
  },
  "error": null
}
```

---

### `PATCH /api/v1/admin/tags/{tag_id}`

- 태그명을 수정합니다.

#### Request Body

```json
{
  "name": "사회"
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "tag_id": "tag_007",
    "name": "사회",
    "updated_at": "2026-03-10T10:00:00Z"
  },
  "error": null
}
```

---

### `DELETE /api/v1/admin/tags/{tag_id}`

- 태그를 삭제합니다. 연결된 `BATTLE_TAGS` 레코드도 함께 삭제됩니다.

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "success": true,
    "deleted_at": "2026-03-10T11:00:00Z"
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

## 태그 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `TAG_NOT_FOUND` | `404` | 존재하지 않는 태그 |
| `TAG_ALREADY_EXISTS` | `409` | 이미 존재하는 태그명 |
| `TAG_IN_USE` | `409` | 배틀에 사용 중인 태그 (삭제 불가) |
| `TAG_LIMIT_EXCEEDED` | `400` | 배틀당 태그 최대 개수 초과 |

---