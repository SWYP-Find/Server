# 좋아요 API 명세서

---

## 설계 메모

- 관점에 들어갈 좋아요 API 입니다.

---

## 관점 좋아요 조회 API

### `GET /api/v1/perspectives/{perspective_id}/likes`

- 관점 좋아요

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "perspective_id": "perspective_001",
    "like_count": 13
  },
  "error": null
}
```

#### 예외 응답 `404 - 관점 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "PERSPECTIVE_NOT_FOUND",
    "message": "존재하지 않는 관점입니다.",
    "errors": []
  }
}
```

```json
{
  "statusCode": 409,
  "data": null,
  "error": {
    "code": "LIKE_ALREADY_EXISTS",
    "message": "이미 좋아요를 누른 관점입니다.",
    "errors": []
  }
}
```

---
## 관점 좋아요 등록 API
### `POST /api/v1/perspectives/{perspective_id}/likes`

- 좋아요 등록

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "perspective_id": "perspective_001",
    "like_count": 13,
    "is_liked": true
  },
  "error": null
}
```

#### 실패 응답 `404 - 관점 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "PERSPECTIVE_NOT_FOUND",
    "message": "존재하지 않는 관점입니다.",
    "errors": []
  }
}
```

#### 실패 응답 `409 - 이미 좋아요 누름`

```json
{
  "statusCode": 409,
  "data": null,
  "error": {
    "code": "LIKE_ALREADY_EXISTS",
    "message": "이미 좋아요를 누른 관점입니다.",
    "errors": []
  }
}
```


---
##  관점에 등록됐던 좋아요 삭제 API
### `DELETE /api/v1/perspectives/{perspective_id}/likes`

- 관점에 등록됐던 좋아요 취소 API

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "perspective_id": "perspective_001",
    "like_count": 12,
    "is_liked": false
  },
  "error": null
}
```

#### 실패 응답 `404 - 관점 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "PERSPECTIVE_NOT_FOUND",
    "message": "존재하지 않는 관점입니다.",
    "errors": []
  }
}
```

#### 실패 응답 `409 - 좋아요 누른 적 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "LIKE_NOT_FOUND",
    "message": "좋아요를 누른 적 없는 관점입니다.",
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

## 좋아요 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `LIKE_ALREADY_EXISTS` | `409` | 이미 좋아요 누른 관점 |
| `LIKE_NOT_FOUND` | `404` | 좋아요 누른 적 없는 관점 |

---