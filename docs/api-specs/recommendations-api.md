# 성향기반 배틀 추천 API 명세서

---

## 설계 메모

- 연관 , 비슷한 , 반대 성향에 대한 추천 / 내부 정책 로직 API 입니다.

---

## 성향 기반 연관 배틀 조회 API

### `GET /api/v1/battles/{battle_id}/related`

- 연관 배틀 조회

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battle_id": "battle_002",
        "title": "유전자 편집 아기, 허용해야 할까?",
        "tags": [
          { "tag_id": "tag_001", "name": "과학" },
          { "tag_id": "tag_002", "name": "윤리" }
        ],
        "options": [
          { "option_id": "option_A", "label": "A", "title": "허용" },
          { "option_id": "option_B", "label": "B", "title": "금지" }
        ],
        "participants_count": 890
      }
    ]
  },
  "error": null
}
```

#### 예외 응답 `404 - 배틀 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "BATTLE_NOT_FOUND",
    "message": "존재하지 않는 배틀입니다.",
    "errors": []
  }
}
```

---
## 성향 기반 비슷한 유저가 들은 배틀 조회 API
### `GET /api/v1/battles/{battle_id}/recommendations/similar`

- 비슷한 유저가 들은 배틀

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battle_id": "battle_002",
        "title": "사형제도, 유지 vs 폐지",
        "thumbnail_url": "https://cdn.pique.app/battle/002.png",
        "tags": [
          { "tag_id": "tag_001", "name": "사회" }
        ],
        "participants_count": 1500,
        "options": [
          { "option_id": "option_A", "label": "A", "title": "유지" },
          { "option_id": "option_B", "label": "B", "title": "폐지" }
        ],
        "match_ratio": 87
      }
    ]
  },
  "error": null
}
```

### 예외 응답 `404 - 배틀 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "BATTLE_NOT_FOUND",
    "message": "존재하지 않는 배틀입니다.",
    "errors": []
  }
}
```

---
## 성향 기반 반대 성향 유저에게 인기 배틀 조회 API
### `GET /api/v1/battles/{battle_id}/recommendations/opposite`

- 반대 성향 유저에게 인기 중인 배틀

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battle_id": "battle_003",
        "title": "AI 판사, 도입해야 할까?",
        "thumbnail_url": "https://cdn.pique.app/battle/003.png",
        "tags": [
          { "tag_id": "tag_002", "name": "기술" }
        ],
        "participants_count": 780,
        "options": [
          { "option_id": "option_A", "label": "A", "title": "도입" },
          { "option_id": "option_B", "label": "B", "title": "반대" }
        ]
      }
    ]
  },
  "error": null
}
```

#### 예외 응답 `404 - 배틀 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "BATTLE_NOT_FOUND",
    "message": "존재하지 않는 배틀입니다.",
    "errors": []
  }
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