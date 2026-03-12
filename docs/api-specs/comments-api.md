# 댓글 API 명세서

---

## 설계 메모

- 관점에서의 댓글 관련한 API 입니다.
- 대댓글은 존재하지 않고 같은 최상위 뎁스의 댓글만 존재합니다.

---

## 댓글 목록 조회 API

### `GET /api/v1/perspectives/{perspective_id}/comments`

- 댓글 목록 조회 (UI 상에서 아직 없어 임의로 기입함)

#### 쿼리 파라미터

- 파라미터        |    타입    | 필수 | 설명
- cursor        |   string  |  X  | 커서 페이지네이션
-  size         |   number  |  X  | 기본값 20 (임의 설정했음)

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "comment_id": "comment_001",
        "user": {
          "user_tag": "user@12312asb",
          "nickname": "철학하는고양이",
          "character_url": "https://cdn.pique.app/characters/cat.png"
        },
        "content": "저도 같은 생각이에요.",
        "is_mine": true,
        "created_at": "2026-03-11T12:00:00Z"
      }
    ],
    "next_cursor": "cursor_002",
    "has_next": true
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

---
## 특정 댓글 삭제 API
### `DELETE /api/v1/perspectives/{perspective_id}/comments/{comment_id}`

- 특정 댓글을 삭제

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "success": true
  },
  "error": null
}
```

#### 예외 응답 `404 - 댓글 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "COMMENT_NOT_FOUND",
    "message": "존재하지 않는 댓글입니다.",
    "errors": []
  }
}
```

#### 예외 응답 `403 - 본인 댓글 아님`

```json
{
  "statusCode": 403,
  "data": null,
  "error": {
    "code": "FORBIDDEN_ACCESS",
    "message": "본인 댓글만 삭제할 수 있습니다.",
    "errors": []
  }
}
```

---

## 특정 댓글 수정 API
### `PATCH  /api/v1/perspectives/{perspective_id}/comments/{comment_id}`

- 특정 댓글을 삭제

#### Request Body

```json
{
  "content": "수정된 댓글 내용이에요."
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "comment_id": "comment_001",
    "content": "수정된 댓글 내용이에요.",
    "updated_at": "2026-03-11T13:00:00Z"
  },
  "error": null
}
```

#### 예외 응답 `404 - 댓글 없음`

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "COMMENT_NOT_FOUND",
    "message": "존재하지 않는 댓글입니다.",
    "errors": []
  }
}
```

#### 예외 응답 `403 - 본인 댓글 아님`

```json
{
  "statusCode": 403,
  "data": null,
  "error": {
    "code": "COMMENT_FORBIDDEN",
    "message": "본인 댓글만 삭제할 수 있습니다.",
    "errors": []
  }
}
```

#### 예외 응답 `400 - 내용 없음`

```json
{
  "statusCode": 400,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_PARAMETER",
    "message": "댓글 내용을 입력해주세요.",
    "errors": []
  }
}
```

---


## 특정 댓글 생성 API
### `DELETE  /api/v1/perspectives/{perspective_id}/comments`

- 특정 댓글을 삭제

#### Request Body

```json
{
  "content": "저도 같은 생각이에요."
}
```

#### 성공 응답 `201 Created`

```json
{
  "statusCode": 201,
  "data": {
    "comment_id": "comment_001",
    "user": {
      "user_tag": "user@12312asb",
      "nickname": "철학하는고양이",
      "character_url": "https://cdn.pique.app/characters/cat.png"
    },
    "content": "저도 같은 생각이에요.",
    "created_at": "2026-03-11T12:00:00Z"
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

#### 예외 응답 `400 - 내용 없음`

```json
{
  "statusCode": 400,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_PARAMETER",
    "message": "댓글 내용을 입력해주세요.",
    "errors": []
  }
}
```


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

## 댓글 에러 코드

| Error Code | HTTP Status | 설명        |
|------------|:-----------:|-----------|
| `COMMENT_NOT_FOUND` | `404` | 존재하지 않는 댓글 |
| `COMMENT_FORBIDDEN` | `403` | 본인 댓글 아님  |
---