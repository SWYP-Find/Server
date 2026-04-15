# 내 정보 / 사용자 API 명세서

## 1. 설계 메모

- 이 문서는 사용자 프로필 수정과 `/api/v1/me/**` 계열 API를 함께 다룹니다.
- 문서 전반은 `snake_case` 필드명을 기준으로 합니다.
- 외부 응답에서는 내부 PK인 `user_id`를 노출하지 않고 `user_tag`를 사용합니다.
- `nickname`은 중복 허용 프로필명입니다.
- `user_tag`는 고유한 공개 식별자이며 저장 시 `@` 없이 관리합니다.
- `user_tag`는 prefix 없이 생성되는 8자리 이하의 랜덤 문자열입니다.
- 프로필 아바타는 자유 입력 이모지가 아니라 `character_type` 선택 방식으로 관리합니다.
- `GET /api/v1/me/mypage`는 상단 요약 조회, `GET /api/v1/me/recap`은 상세 리캡 조회에 사용합니다.
- `GET /api/v1/me/credits/history`는 로그인한 사용자의 크레딧 적립/소비 내역을 `offset/size` 기반으로 조회합니다.
- 프론트는 `philosopher_type` 값에 따라 사전 정의된 철학자 카드를 통째로 교체 렌더링합니다.
- 그래서 백엔드는 철학자 카드용 `title`, `description`, 해시태그 문구를 내려주지 않습니다.
- 현재 크레딧(`current_point`)은 `users.credit` 캐시 컬럼 기준으로 조회합니다.
- 현재 반영 크레딧 타입은 `BATTLE_VOTE(5)`, `MAJORITY_WIN(10)`, `BEST_COMMENT(50)`, `WEEKLY_CHARGE(40)`, `FREE_CHARGE(가변)` 입니다.
- 다수결/베댓 보상은 매주 월요일 00:00(KST) 배치로 정산하며 대상 배틀 윈도우는 `runDate - 20일`부터 `runDate - 14일`까지입니다.
- 베댓 보상은 배틀당 좋아요 상위 3개 관점만 대상이며 각 관점은 좋아요 10개 이상이어야 합니다.
- 철학자 산출 로직은 추후 확정 예정이며, 현재는 프론트 연동을 위해 임시로 `SOCRATES`를 반환합니다.

### 1.1 공통 프로필 응답 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `user_tag` | string | 외부 공개용 사용자 식별자 |
| `nickname` | string | 중복 허용 프로필명 |
| `character_type` | string | 캐릭터 enum 값 |
| `manner_temperature` | number | 사용자 매너 온도 |

### 1.2 공통 enum 값

| 필드 | 가능한 값 |
|------|-----------|
| `philosopher_type` | `SOCRATES \| PLATO \| ARISTOTLE \| KANT \| NIETZSCHE \| MARX \| SARTRE \| CONFUCIUS \| LAOZI \| BUDDHA` |
| `character_type` | `OWL \| FOX \| WOLF \| LION \| PENGUIN \| BEAR \| RABBIT \| CAT` |
| `activity_type` | `COMMENT \| LIKE` |
| `vote_side` | `PRO \| CON` |
| `credit_type` | `BATTLE_VOTE \| MAJORITY_WIN \| BEST_COMMENT \| WEEKLY_CHARGE \| FREE_CHARGE` |

---

## 2. 프로필 API

### 2.1 `PATCH /api/v1/me/profile`

닉네임 및 캐릭터 수정.

요청:

```json
{
  "nickname": "생각하는펭귄",
  "character_type": "PENGUIN"
}
```

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "a7k2m9q1",
    "nickname": "생각하는펭귄",
    "character_type": "PENGUIN",
    "updated_at": "2026-03-08T12:00:00Z"
  },
  "error": null
}
```

---

## 3. 마이페이지 조회 API

### 3.1 `GET /api/v1/me/mypage`

마이페이지 상단에 필요한 집계 데이터 조회.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "profile": {
      "user_tag": "a7k2m9q1",
      "nickname": "생각하는올빼미",
      "character_type": "OWL",
      "manner_temperature": 36.5
    },
    "philosopher": {
      "philosopher_type": "SOCRATES"
    },
    "tier": {
      "tier_code": "WANDERER",
      "tier_label": "방랑자",
      "current_point": 40
    }
  },
  "error": null
}
```

### 3.2 `GET /api/v1/me/recap`

상세 리캡 정보 조회.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "my_card": {
      "philosopher_type": "SOCRATES"
    },
    "best_match_card": {
      "philosopher_type": "PLATO"
    },
    "worst_match_card": {
      "philosopher_type": "MARX"
    },
    "scores": {
      "principle": 88,
      "reason": 74,
      "individual": 62,
      "change": 45,
      "inner": 30,
      "ideal": 15
    },
    "preference_report": {
      "total_participation": 47,
      "opinion_changes": 12,
      "battle_win_rate": 68,
      "favorite_topics": [
        {
          "rank": 1,
          "tag_name": "철학",
          "participation_count": 20
        },
        {
          "rank": 2,
          "tag_name": "문학",
          "participation_count": 13
        },
        {
          "rank": 3,
          "tag_name": "예술",
          "participation_count": 8
        },
        {
          "rank": 4,
          "tag_name": "사회",
          "participation_count": 5
        }
      ]
    }
  },
  "error": null
}
```

### 3.3 `GET /api/v1/me/battle-records`

내 배틀 기록 조회.
찬성/반대 탭을 따로 나누지 않고 하나의 목록으로 반환합니다.
각 item의 `vote_side`가 실제 구분자입니다.

쿼리 파라미터:

- `offset`: 선택, 0-based 시작 위치
- `size`: 선택
- `vote_side`: 각 item의 구분자이며 가능한 값은 `PRO | CON`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battle_id": "battle_001",
        "record_id": "vote_001",
        "vote_side": "PRO",
        "title": "안락사 도입, 찬성 vs 반대",
        "summary": "인간에게 품위 있는 죽음을 허용해야 할까?",
        "created_at": "2026-03-07T18:30:00"
      }
    ],
    "next_offset": 20,
    "has_next": true
  },
  "error": null
}
```

### 3.4 `GET /api/v1/me/content-activities`

내 댓글/좋아요 기반 콘텐츠 활동 조회.
댓글/좋아요 탭을 따로 나누지 않고 하나의 목록으로 반환합니다.
각 item의 `activity_type`이 실제 구분자입니다.

쿼리 파라미터:

- `offset`: 선택, 0-based 시작 위치
- `size`: 선택
- `activity_type`: 각 item의 구분자이며 가능한 값은 `COMMENT | LIKE`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "activity_id": "comment_001",
        "activity_type": "COMMENT",
        "perspective_id": "perspective_001",
        "battle_id": "battle_001",
        "battle_title": "안락사 도입, 찬성 vs 반대",
        "author": {
          "user_tag": "a7k2m9q1",
          "nickname": "사색하는고양이",
          "character_type": "CAT"
        },
        "stance": "반대",
        "content": "제도가 무서운 건, 사회적 압력이 선택을 의무로 바꿀 수 있다는 거예요.",
        "like_count": 1340,
        "created_at": "2026-03-08T12:00:00"
      }
    ],
    "next_offset": 20,
    "has_next": true
  },
  "error": null
}
```

### 3.5 `GET /api/v1/me/credits/history`

로그인한 사용자의 크레딧 적립/소비 내역 조회.

쿼리 파라미터:

- `offset`: 선택, 0-based 시작 위치
- `size`: 선택

응답:

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "id": 301,
        "credit_type": "BEST_COMMENT",
        "amount": 50,
        "reference_id": 200,
        "created_at": "2026-04-13T00:00:00"
      },
      {
        "id": 300,
        "credit_type": "BATTLE_VOTE",
        "amount": 5,
        "reference_id": 12345,
        "created_at": "2026-04-12T14:30:00"
      }
    ],
    "next_offset": 20,
    "has_next": true
  },
  "error": null
}
```

### 3.6 `GET /api/v1/share/recap`

현재 로그인한 사용자의 리캡 공유 키 발급.
이미 발급된 키가 있으면 동일 키를 재사용합니다.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "shareKey": "550e8400-e29b-41d4-a716-446655440000"
  },
  "error": null
}
```

### 3.7 `GET /api/v1/share/recap/{shareKey}`

공유 키로 다른 사용자의 리캡 조회.
인증 없이 호출 가능합니다.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "my_card": {
      "philosopher_type": "SOCRATES"
    },
    "best_match_card": {
      "philosopher_type": "PLATO"
    },
    "worst_match_card": {
      "philosopher_type": "MARX"
    },
    "scores": {
      "principle": 88,
      "reason": 74,
      "individual": 62,
      "change": 45,
      "inner": 30,
      "ideal": 15
    },
    "preference_report": {
      "total_participation": 47,
      "opinion_changes": 12,
      "battle_win_rate": 68,
      "favorite_topics": []
    }
  },
  "error": null
}
```

### 3.8 `GET /api/v1/me/notification-settings`

마이페이지 알림 설정 조회.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "new_battle_enabled": false,
    "battle_result_enabled": true,
    "comment_reply_enabled": true,
    "new_comment_enabled": false,
    "content_like_enabled": false,
    "marketing_event_enabled": true
  },
  "error": null
}
```

### 3.9 `PATCH /api/v1/me/notification-settings`

마이페이지 알림 설정 부분 수정.

요청:

```json
{
  "battle_result_enabled": true,
  "marketing_event_enabled": false
}
```

응답:

```json
{
  "statusCode": 200,
  "data": {
    "new_battle_enabled": false,
    "battle_result_enabled": true,
    "comment_reply_enabled": true,
    "new_comment_enabled": false,
    "content_like_enabled": false,
    "marketing_event_enabled": false
  },
  "error": null
}
```

### 3.10 `GET /api/v1/me/notices`

공지/이벤트 목록 조회.

쿼리 파라미터:

- `type`: `NOTICE | EVENT`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "notice_id": "notice_001",
        "type": "NOTICE",
        "title": "3월 신규 딜레마 업데이트",
        "body_preview": "매일 새로운 딜레마가 추가돼요.",
        "is_pinned": true,
        "published_at": "2026-03-01T00:00:00"
      }
    ]
  },
  "error": null
}
```

### 3.11 `GET /api/v1/me/notices/{noticeId}`

공지/이벤트 상세 조회.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "notice_id": "notice_001",
    "type": "NOTICE",
    "title": "3월 신규 딜레마 업데이트",
    "body": "매일 새로운 딜레마가 추가돼요.",
    "is_pinned": true,
    "published_at": "2026-03-01T00:00:00"
  },
  "error": null
}
```

---

## 4. 에러 코드

### 4.1 공통 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `COMMON_INVALID_PARAMETER` | `400` | 요청 파라미터 오류 |
| `AUTH_UNAUTHORIZED` | `401` | 인증 실패 |
| `AUTH_ACCESS_TOKEN_EXPIRED` | `401` | Access Token 만료 |
| `AUTH_REFRESH_TOKEN_EXPIRED` | `401` | Refresh Token 만료 - 재로그인 필요 |
| `USER_BANNED` | `403` | 영구 제재된 사용자 |
| `USER_SUSPENDED` | `403` | 일정 기간 이용 정지된 사용자 |
| `INTERNAL_SERVER_ERROR` | `500` | 서버 오류 |

### 4.2 사용자 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `USER_NOT_FOUND` | `404` | 존재하지 않는 사용자 |
| `ONBOARDING_ALREADY_COMPLETED` | `409` | 이미 온보딩이 완료된 사용자 |
