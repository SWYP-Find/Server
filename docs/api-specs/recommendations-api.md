# 성향기반 배틀 추천 API 명세서

---

## 설계 메모

- 연관 , 비슷한 , 반대 성향에 대한 추천 / 내부 정책 로직 API 입니다.

---

## 성향 기반 비슷한 유저가 들은 배틀 조회 API
### `GET /api/v1/battles/{battle_id}/recommendations/similar`

- 비슷한 유저가 들은 배틀 , PM의 전략 미확정 (26.03.15) 

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battle_id": "battle_002",
        "title": "사후세계는 존재하는가, 인간이 만든 위안인가?",
        "tags": [
          { "tag_id": "tag_001", "name": "철학" }
        ],
        "participants_count": 1340,
        "options": [
          {
            "option_id": "option_A",
            "label": "A",
            "title": "존재한다",
            "representative": "플라톤",
            "image_url": "https://cdn.pique.app/characters/platon.png"
          },
          {
            "option_id": "option_B",
            "label": "B",
            "title": "인간이 만든 위안이다",
            "representative": "에피쿠로스",
            "image_url": "https://cdn.pique.app/characters/epicurus.png"
          }
        ]
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