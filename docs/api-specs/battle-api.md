# 배틀 API 명세서

---

## 설계 메모

- **오늘의 배틀 :**
  - 스와이프 UI를 위해 약 5개의 배틀 리스트를 반환합니다. '오늘의 배틀(검정 창)'과 '일반 배틀 카드(하얀 창)'의 진입점(API)을 분리하여 각기 필요한 데이터를 제공합니다.
- **태그 :**
  - 배틀 응답의 `tags` 필드는 `{ tag_id, name }` 객체 배열로 반환됩니다. 태그 전체 목록 조회 및 태그 기반 배틀 필터링은 Tag API를 참조하세요.
- **도메인 분리 :**
  - 사용자 서비스 API와 관리자(Admin) 전용 API 도메인을 분리했습니다. 기본 콘텐츠 발행은 관리자 도메인에서 이루어집니다.
- **AI 자동 생성 :**
  - 스케줄러가 매일 자동으로 트렌딩 이슈를 검색·수집하여 AI API를 호출하고 배틀 초안을 `PENDING` 상태로 저장합니다. 관리자는 `/api/v1/admin/ai/battles`를 통해 검수·승인·반려합니다.
- **배틀 `status` 흐름 :**

  | status | 적용 대상        | 설명 |
    |--------|--------------|------|
  | `DRAFT` | 관리자          | 관리자가 작성 중인 초안 |
  | `PENDING` | AI, 유저 [후순위] | 검수 대기 중 |
  | `PUBLISHED` | 전체           | 검수 완료, 실제 노출 |
  | `REJECTED` | AI, 유저 [후순위] | 검수 반려 |
  | `ARCHIVED` | 전체           | 배틀 종료 후 이력 보존 |

- **[후순위] 크리에이터 정책 :**
  - 매너 온도 45도 이상의 사용자가 직접 배틀을 제안하는 기능은 런칭 스펙에서 제외됩니다.

---

## 사용자 API

### `GET /api/v1/battles/today`

- 스와이프 UI용으로 오늘 진행 중인 배틀 목록을 반환합니다.
- 피그마 디자인 상 5개로 임의 판단 -> 추후 수정 가능

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battle_id": "battle_001",
        "title": "드라마 <레이디 두아>, 원가 18만원 명품은 사기인가?",
        "summary": "18만 원짜리 가방을 1억에 판 주인공, 사기꾼일까 예술가일까?",
        "thumbnail_url": "https://cdn.pique.app/battle/hot-001.png",
        "tags": [
          { "tag_id": "tag_001", "name": "사회" },
          { "tag_id": "tag_002", "name": "철학" },
          { "tag_id": "tag_003", "name": "롤스" },
          { "tag_id": "tag_004", "name": "니체" }
        ],
        "participants_count": 2148,
        "audio_duration": 420,
        "share_url": "https://pique.app/battles/battle_001",
        "options": [
          { "option_id": "option_A", "label": "A", "title": "사기다 (롤스)" },
          { "option_id": "option_B", "label": "B", "title": "사기가 아니다 (니체)" }
        ],
        "user_vote_status": "NONE"
      }
    ],
    "total_count": 5
  },
  "error": null
}
```

---

### `GET /api/v1/battles/{battle_id}`

- 배틀 카드(하얀 창) 선택 시 노출되는 상세 정보(철학자, 성향, 인용구 등)를 조회합니다.

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "battle_id": "battle_001",
    "title": "드라마 <레이디 두아>, 원가 18만원 명품은 사기인가?",
    "tags": [
      { "tag_id": "tag_001", "name": "사회" },
      { "tag_id": "tag_002", "name": "철학" }
    ],
    "options": [
      {
        "option_id": "option_A",
        "label": "A",
        "stance": "정보의 대칭 (공정성)",
        "representative": "존 롤스",
        "title": "사기다",
        "quote": "베일 뒤에서 누구나 동의할 수 있는 공정한 규칙이 깨진 것입니다.",
        "keywords": ["합리적", "원칙주의", "절대적"],
        "image_url": "https://cdn.pique.app/images/rawls.png"
      },
      {
        "option_id": "option_B",
        "label": "B",
        "stance": "가치 창조 (욕망의 질서)",
        "representative": "프리드리히 니체",
        "title": "사기가 아니다",
        "quote": "주인공은 가려운 욕망을 정확히 읽어내고, 새로운 가치를 창조해낸 예술가입니다.",
        "keywords": ["본능적", "실용주의", "주관적"],
        "image_url": "https://cdn.pique.app/images/nietzsche.png"
      }
    ]
  },
  "error": null
}
```

---

## 관리자 API

### `POST /api/v1/admin/battles`

- 공식 배틀을 직접 생성합니다.

#### Request Body

```json
{
  "title": "드라마 <레이디 두아>, 원가 18만원 명품은 사기인가?",
  "summary": "18만 원짜리 가방을 1억에 판 주인공, 사기꾼일까 예술가일까?",
  "description": "예술과 사기의 경계에 대한 철학적 딜레마",
  "thumbnail_url": "https://cdn.pique.app/battle/hot-001.png",
  "target_date": "2026-03-10",
  "tag_ids": ["tag_001", "tag_002", "tag_003", "tag_004"],
  "options": [
    {
      "label": "A",
      "title": "사기다",
      "stance": "정보의 대칭 (공정성)",
      "representative": "존 롤스",
      "quote": "베일 뒤에서 누구나 동의할 수 있는 공정한 규칙이 깨진 것입니다.",
      "keywords": ["합리적", "원칙주의", "절대적"],
      "image_url": "https://cdn.pique.app/images/rawls.png"
    },
    {
      "label": "B",
      "title": "사기가 아니다",
      "stance": "가치 창조 (욕망의 질서)",
      "representative": "프리드리히 니체",
      "quote": "주인공은 가려운 욕망을 정확히 읽어내고, 새로운 가치를 창조해낸 예술가입니다.",
      "keywords": ["본능적", "실용주의", "주관적"],
      "image_url": "https://cdn.pique.app/images/nietzsche.png"
    }
  ]
}
```

#### 성공 응답 `201 Created`

```json
{
  "statusCode": 201,
  "data": {
    "battle_id": "battle_001",
    "title": "드라마 <레이디 두아>, 원가 18만원 명품은 사기인가?",
    "summary": "18만 원짜리 가방을 1억에 판 주인공, 사기꾼일까 예술가일까?",
    "description": "예술과 사기의 경계에 대한 철학적 딜레마",
    "thumbnail_url": "https://cdn.pique.app/battle/hot-001.png",
    "target_date": "2026-03-10",
    "status": "DRAFT",
    "creator_type": "ADMIN",
    "tags": [
      { "tag_id": "tag_001", "name": "사회" },
      { "tag_id": "tag_002", "name": "철학" },
      { "tag_id": "tag_003", "name": "롤스" },
      { "tag_id": "tag_004", "name": "니체" }
    ],
    "options": [
      {
        "option_id": "option_A",
        "label": "A",
        "title": "사기다",
        "stance": "정보의 대칭 (공정성)",
        "representative": "존 롤스",
        "quote": "베일 뒤에서 누구나 동의할 수 있는 공정한 규칙이 깨진 것입니다.",
        "keywords": ["합리적", "원칙주의", "절대적"],
        "image_url": "https://cdn.pique.app/images/rawls.png"
      },
      {
        "option_id": "option_B",
        "label": "B",
        "title": "사기가 아니다",
        "stance": "가치 창조 (욕망의 질서)",
        "representative": "프리드리히 니체",
        "quote": "주인공은 가려운 욕망을 정확히 읽어내고, 새로운 가치를 창조해낸 예술가입니다.",
        "keywords": ["본능적", "실용주의", "주관적"],
        "image_url": "https://cdn.pique.app/images/nietzsche.png"
      }
    ],
    "created_at": "2026-03-10T09:00:00Z"
  },
  "error": null
}
```

---

### `PATCH /api/v1/admin/battles/{battle_id}`

- 배틀 정보를 수정합니다. 변경할 필드만 포함합니다.

#### Request Body

```json
{
  "title": "드라마 <레이디 두아>, 원가 18만원 명품은 사기인가? (수정)",
  "status": "PUBLISHED",
  "tag_ids": ["tag_001", "tag_002"]
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "battle_id": "battle_001",
    "title": "드라마 <레이디 두아>, 원가 18만원 명품은 사기인가? (수정)",
    "summary": "18만 원짜리 가방을 1억에 판 주인공, 사기꾼일까 예술가일까?",
    "description": "예술과 사기의 경계에 대한 철학적 딜레마",
    "thumbnail_url": "https://cdn.pique.app/battle/hot-001.png",
    "target_date": "2026-03-10",
    "status": "PUBLISHED",
    "creator_type": "ADMIN",
    "tags": [
      { "tag_id": "tag_001", "name": "사회" },
      { "tag_id": "tag_002", "name": "철학" }
    ],
    "updated_at": "2026-03-10T10:00:00Z"
  },
  "error": null
}
```

---

### `DELETE /api/v1/admin/battles/{battle_id}`

- 배틀을 삭제합니다.

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

## `[후순위]` 관리자 AI 검수 API 

- 스케줄러가 자동 생성한 AI 배틀 초안(`PENDING`)을 관리자가 검수 · 승인 · 반려합니다.

### `GET /api/v1/admin/ai/battles`

- AI가 생성한 `PENDING` 상태의 배틀 목록을 조회합니다.

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battle_id": "battle_ai_001",
        "title": "AI가 제안한 배틀 제목",
        "summary": "AI가 생성한 요약",
        "thumbnail_url": "https://cdn.pique.app/battle/ai-001.png",
        "target_date": "2026-03-11",
        "status": "PENDING",
        "creator_type": "AI",
        "tags": [
          { "tag_id": "tag_001", "name": "사회" }
        ],
        "options": [
          { "option_id": "option_A", "label": "A", "title": "찬성", "keywords": ["합리적", "효율중심", "미래지향"] },
          { "option_id": "option_B", "label": "B", "title": "반대", "keywords": ["인본주의", "도덕중심", "전통적"] }
        ],
        "created_at": "2026-03-11T06:00:00Z"
      }
    ],
    "total_count": 3
  },
  "error": null
}
```

---

### `PATCH /api/v1/admin/ai/battles/{battle_id}`

- AI가 생성한 배틀을 승인하거나 반려합니다. 승인 시 내용을 수정한 뒤 승인할 수 있습니다.

#### Request Body — 승인

```json
{
  "action": "APPROVE",
  "title": "AI 초안 제목 (수정 가능)",
  "summary": "AI 초안 요약 (수정 가능)",
  "tag_ids": ["tag_001", "tag_002"]
}
```

#### Request Body — 반려

```json
{
  "action": "REJECT",
  "reject_reason": "주제가 서비스 방향과 맞지 않음"
}
```

#### 성공 응답 `200 OK` — 승인

```json
{
  "statusCode": 200,
  "data": {
    "battle_id": "battle_ai_001",
    "status": "PUBLISHED",
    "creator_type": "AI",
    "updated_at": "2026-03-11T09:00:00Z"
  },
  "error": null
}
```

#### 성공 응답 `200 OK` — 반려

```json
{
  "statusCode": 200,
  "data": {
    "battle_id": "battle_ai_001",
    "status": "REJECTED",
    "reject_reason": "주제가 서비스 방향과 맞지 않음",
    "updated_at": "2026-03-11T09:00:00Z"
  },
  "error": null
}
```

---

## `[후순위]` 크리에이터 API 

### `POST /api/v1/battles`

- 배틀을 제안합니다. (매너 온도 45도 이상 유저)

#### Request Body

```json
{
  "title": "AI가 만든 예술 작품, 저작권은 누구에게?",
  "summary": "AI 창작물의 저작권 귀속 주체에 대한 철학적 딜레마",
  "description": "창작의 주체성과 소유권에 대한 철학적 논쟁",
  "thumbnail_url": "https://cdn.pique.app/battle/ai-art.png",
  "target_date": "2026-03-15",
  "tag_ids": ["tag_002", "tag_005"],
  "options": [
    {
      "label": "A",
      "title": "AI 개발사에게 귀속된다",
      "stance": "도구 이론",
      "representative": "존 로크",
      "quote": "노동을 투입한 자에게 소유권이 있다.",
      "keywords": ["합리적", "효율중심", "미래지향"],
      "image_url": "https://cdn.pique.app/images/locke.png"
    },
    {
      "label": "B",
      "title": "퍼블릭 도메인이어야 한다",
      "stance": "공유재 이론",
      "representative": "장 자크 루소",
      "quote": "창작물은 사회의 산물이므로 모두의 것이다.",
      "keywords": ["합리적", "효율중심", "미래지향"],
      "image_url": "https://cdn.pique.app/images/rousseau.png"
    }
  ]
}
```

#### 성공 응답 `201 Created`

```json
{
  "statusCode": 201,
  "data": {
    "battle_id": "battle_002",
    "title": "AI가 만든 예술 작품, 저작권은 누구에게?",
    "status": "PENDING",
    "creator_type": "USER",
    "created_at": "2026-03-10T12:00:00Z"
  },
  "error": null
}
```

---

### `PATCH /api/v1/battles/{battle_id}`

- 제안한 배틀 정보를 수정합니다. 변경할 필드만 포함합니다.

#### Request Body

```json
{
  "title": "AI가 만든 예술 작품, 저작권은 누구에게? (수정)",
  "summary": "AI 창작물의 저작권 귀속 주체에 대한 철학적 딜레마"
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "battle_id": "battle_002",
    "title": "AI가 만든 예술 작품, 저작권은 누구에게? (수정)",
    "summary": "AI 창작물의 저작권 귀속 주체에 대한 철학적 딜레마",
    "status": "PENDING",
    "creator_type": "USER",
    "updated_at": "2026-03-10T13:00:00Z"
  },
  "error": null
}
```

---

### `DELETE /api/v1/battles/{battle_id}`

- 제안한 배틀을 삭제합니다.

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "success": true,
    "deleted_at": "2026-03-10T14:00:00Z"
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

## 배틀 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `BATTLE_NOT_FOUND` | `404` | 존재하지 않는 배틀 |
| `BATTLE_CLOSED` | `409` | 종료된 배틀 |
| `BATTLE_ALREADY_PUBLISHED` | `409` | 이미 발행된 배틀 |
| `BATTLE_OPTION_NOT_FOUND` | `404` | 존재하지 않는 선택지 |

---