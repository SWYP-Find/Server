# 관점 API 명세서

---

## 설계 메모

- 관점 API 입니다.
- 현재 Creator 뱃지 부분이 ERD 상에선 보이지 않는데 확인 필요
---

## 관점 생성 API

### `POST /api/v1/battles/{battle_id}/perspectives`

- 특정 배틀에 대한 관점 생성

#### Request Body

```json
{
  "content": "자기결정권은 가장 기본적인 인권이라고 생각해요."
}
```

#### 성공 응답 `201 Created`

```json
{
  "statusCode": 201,
  "data": {
    "perspective_id": "perspective_001",
    "status": "PENDING",
    "created_at": "2026-03-11T12:00:00Z"
  },
  "error": null
}
```

#### 예외 응답 `404 - 존재하지 않는 배틀`

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

#### 예외 응답 `409 - 이미 관점 작성함`

```json
{
  "statusCode": 409,
  "data": null,
  "error": {
    "code": "PERSPECTIVE_ALREADY_EXISTS",
    "message": "이미 관점을 작성한 배틀입니다.",
    "errors": []
  }
}
```



---
## 관점 리스트 조회 API
### `GET /api/v1/battles/{battle_id}/perspectives`

- 특정 배틀에 대한 관점 리스트 조회

#### 쿼리 파라미터

- 파라미터        |    타입    | 필수 | 설명
- cursor        |   string  |  X  | 커서 페이지네이션
-  size         |   number  |  X  | 기본값 20 (임의 설정했음)
-  option_label |   string  |  X  | A or B 투표 옵션 필터


#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "perspective_id": "perspective_001",
        "user": {
          "user_id": 105,
          "nickname": "철학하는고양이",
          "character_url": "https://cdn.pique.app/characters/cat.png"
        },
        "option": {
          "option_id": "option_A",
          "label": "A",
          "title": "찬성"
        },
        "content": "자기결정권은 가장 기본적인 인권이라고 생각해요.",
        "like_count": 12,
        "comment_count": 3,
        "is_liked": false,
        "created_at": "2026-03-11T12:00:00Z"
      }
    ],
    "next_cursor": "cursor_002",
    "has_next": true
  },
  "error": null
}
```

#### 예외 응답 `404 - 존재하지 않는 배틀`

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
## 관점 삭제 API
### `DELETE /api/v1/perspectives/{perspective_id}`

- 특정 배틀에 대한 내가 쓴 관점 삭제

#### 쿼리 파라미터

- 파라미터        |    타입    | 필수 | 설명
- cursor        |   string  |  X  | 커서 페이지네이션
-  size         |   number  |  X  | 기본값 20 (임의 설정했음)
-  option_label |   string  |  X  | A or B 투표 옵션 필터


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

#### 예외 응답 `403 - 본인 관점 아님`

```json
{
  "statusCode": 403,
  "data": null,
  "error": {
    "code": "FORBIDDEN_ACCESS",
    "message": "본인 관점만 삭제할 수 있습니다.",
    "errors": []
  }
}
```


---
## 관점 수정 API
### `PATCH /api/v1/perspectives/{perspective_id}`

- 특정 배틀에 대한 내가 쓴 관점 수정

#### Request Body

```json
{
  "content": "수정된 관점 내용입니다."
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "perspective_id": "perspective_001",
    "content": "수정된 관점 내용입니다.",
    "updated_at": "2026-03-11T13:00:00Z"
  },
  "error": null
}
```

#### 예외 응답 `404 - 존재하지 않는 관점`

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

#### 예외 응답 `403 - 본인 관점 아님`
```json
{
  "statusCode": 403,
  "data": null,
  "error": {
    "code": "FORBIDDEN_ACCESS",
    "message": "본인 관점만 수정할 수 있습니다.",
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

## 관점 에러 코드

| Error Code | HTTP Status | 설명          |
|------------|:-----------:|-------------|
| `PERSPECTIVE_NOT_FOUND` | `404` | 존재하지 않는 관점  |
| `PERSPECTIVE_ALREADY_EXISTS` | `409` | 해당 배틀에 이미 관점 작성함      |
| `PERSPECTIVE_FORBIDDEN` | `403` | 본인 관점 아님  |
| `PERSPECTIVE_POST_VOTE_REQUIRED` | `409` | 사후 투표 미완료 |

---