# 시나리오 API 명세서

---

## 설계 메모

- **시나리오 구조 (인터랙티브 O/X 모두 지원) :**
  - 배틀의 성격에 따라 인터랙티브(분기 선택)가 없는 '단일 오디오 재생'과 인터랙티브가 있는 '트리형 오디오 재생'을 모두 지원합니다. `is_interactive` 상태값으로 구분하여 클라이언트가 적절한 UI를 렌더링합니다.
- **트리(Node) 구조 :**
  - 시나리오(오디오/대본)는 오프닝/1라운드 → 유저 선택 분기(2라운드) → 최종 결론(3라운드/클로징)으로 이어지는 트리(Node) 구조를 가집니다.
- **TTS 사전 생성 :**
  - 관리자가 시나리오를 발행할 때 단 1번만 TTS API를 호출하여 `.mp3` 파일과 타임스탬프(`start_time`)를 생성하고 CDN에 저장합니다. 유저 플레이 시에는 실시간 호출 없이 저장된 파일을 스트리밍합니다.
- **AI 자동 생성 :**
  - 스케줄러가 매일 자동으로 트렌딩 이슈를 검색·수집하여 AI API를 호출하고 시나리오 초안을 `PENDING` 상태로 저장합니다. 관리자는 `/api/v1/admin/ai/scenarios`를 통해 검수·승인·반려합니다.
- **프론트엔드 자체 처리 :**
  - 글씨 크기(A-/A+) 및 오디오 플레이어 컨트롤(15초 전/후, 배속, 스와이프)은 프론트엔드에서 네이티브/UI 상태로 처리합니다.
- **시나리오 `status` 흐름 :**

  | status | 적용 대상        | 설명 |
    |--------|--------------|------|
  | `DRAFT` | 관리자          | 관리자가 작성 중인 초안. TTS 미생성 상태 |
  | `PENDING` | AI, 유저 [후순위] | 관리자 검수 대기 중 |
  | `PUBLISHED` | 전체           | TTS 생성 완료, CDN 업로드 완료, 실제 노출 |
  | `REJECTED` | AI, 유저 [후순위] | 검수 반려 |
  | `ARCHIVED` | 전체           | 배틀 종료 후 이력 보존, 더 이상 노출 안 함 |

- **[후순위] 크리에이터 정책 :**
  - 매너 온도 45도 이상의 사용자가 직접 시나리오를 제안하는 기능은 런칭 스펙에서 제외됩니다.

---

## 사용자 API

### `GET /api/v1/battles/{battle_id}/scenario`

- 사전 투표 완료 후 시나리오 창 진입 시 호출합니다.
- `is_interactive` 값에 따라 클라이언트 렌더링 방식이 분기됩니다.

---

#### CASE 1 - 단일 재생 (`is_interactive: false`)

- 전체 시나리오가 1개의 노드에 담기며, `interactive_options`는 빈 배열로 반환됩니다.

```json
{
  "statusCode": 200,
  "data": {
    "battle_id": "battle_001",
    "is_interactive": false,
    "my_pre_vote": {
      "option_id": "option_A",
      "label": "A",
      "title": "사기다"
    },
    "start_node_id": "node_001_full",
    "nodes": [
      {
        "node_id": "node_001_full",
        "audio_url": "https://cdn.pique.app/audio/battle_001_full.mp3",
        "audio_duration": 420,
        "scripts": [
          { "start_time": 0,      "speaker_name": "나레이션",        "speaker_side": "NONE", "message": "여기 한 여자가 있습니다. 동대문에서 18만 원에 떼온 가방을 1억 원에 팔았습니다..." },
          { "start_time": 60000,  "speaker_name": "존 롤스",         "speaker_side": "A",    "message": "재판장님, 시장 경제의 핵심은 '정보의 대칭'입니다. 판매자가 원가를 은폐한 것은 기만입니다." },
          { "start_time": 90000,  "speaker_name": "프리드리히 니체", "speaker_side": "B",    "message": "명품을 사는 사람이 원가를 몰라서 삽니까? 그들은 남들보다 우월해지기 위해 기꺼이 1억을 지불한 겁니다." },
          { "start_time": 150000, "speaker_name": "존 롤스",         "speaker_side": "A",    "message": "현명하십니다. 상품의 가치가 전적으로 기만에 의해 결정된다면 사회적 계약의 약탈입니다." },
          { "start_time": 210000, "speaker_name": "프리드리히 니체", "speaker_side": "B",    "message": "역시 가치를 아시는군요! 거래는 예술입니다. 주인공은 가방에 독점적 서사를 입혔고 구매자는 만족했습니다." },
          { "start_time": 300000, "speaker_name": "존 롤스",         "speaker_side": "A",    "message": "한 가지 묻겠습니다. 당신이 만약 그 가방의 구매자였다면, 원가를 알고도 웃으며 1억을 내놓겠습니까?" },
          { "start_time": 330000, "speaker_name": "프리드리히 니체", "speaker_side": "B",    "message": "질문이 틀렸소. 명품을 사는 자들은 이미 그 게임의 규칙을 압니다. 불쾌함이 곧 사기는 아닙니다." },
          { "start_time": 390000, "speaker_name": "나레이션",        "speaker_side": "NONE", "message": "거래는 끝났고, 가방은 누군가의 손에 들려 있습니다. 이제 당신의 최종 선택을 들려주세요." }
        ],
        "interactive_options": []
      }
    ]
  },
  "error": null
}
```

---

#### CASE 2 - 분기형 인터랙티브 재생 (`is_interactive: true`)

- `interactive_options` 배열의 `next_node_id`를 따라 노드를 순회합니다.

```json
{
  "statusCode": 200,
  "data": {
    "battle_id": "battle_001",
    "is_interactive": true,
    "my_pre_vote": {
      "option_id": "option_A",
      "label": "A",
      "title": "사기다"
    },
    "start_node_id": "node_001_opening",
    "nodes": [
      {
        "node_id": "node_001_opening",
        "audio_url": "https://cdn.pique.app/audio/battle_001_round1.mp3",
        "audio_duration": 150,
        "scripts": [
          { "start_time": 0,     "speaker_name": "나레이션",        "speaker_side": "NONE", "message": "여기 한 여자가 있습니다. 동대문에서 18만 원에 떼온 가방을 1억 원에 팔았습니다..." },
          { "start_time": 60000, "speaker_name": "존 롤스",         "speaker_side": "A",    "message": "재판장님, 시장 경제의 핵심은 '정보의 대칭'입니다..." },
          { "start_time": 90000, "speaker_name": "프리드리히 니체", "speaker_side": "B",    "message": "명품을 사는 사람이 원가를 몰라서 삽니까? 그들은 차별화를 위해..." }
        ],
        "interactive_options": [
          { "label": "사회적 신뢰를 위해 정보의 투명성이 우선이다.", "next_node_id": "node_002_branch_a" },
          { "label": "시장은 개인의 욕망이 만나는 곳이다.",           "next_node_id": "node_002_branch_b" }
        ]
      },
      {
        "node_id": "node_002_branch_a",
        "audio_url": "https://cdn.pique.app/audio/battle_001_round2_a.mp3",
        "audio_duration": 110,
        "scripts": [
          { "start_time": 0,     "speaker_name": "유저",    "speaker_side": "A", "message": "사회의 기본 신뢰를 위해 투명한 정보 공개가 우선되어야 합니다." },
          { "start_time": 10000, "speaker_name": "존 롤스", "speaker_side": "A", "message": "현명하십니다. 상품의 가치가 전적으로 기만에 의해 결정된다면..." }
        ],
        "interactive_options": [
          { "label": "최종 충돌 및 정리 듣기", "next_node_id": "node_003_closing" }
        ]
      },
      {
        "node_id": "node_002_branch_b",
        "audio_url": "https://cdn.pique.app/audio/battle_001_round2_b.mp3",
        "audio_duration": 120,
        "scripts": [
          { "start_time": 0,     "speaker_name": "유저",            "speaker_side": "B", "message": "강요 없는 자발적 거래라면, 욕망에 따른 가격 결정은 시장의 자유입니다." },
          { "start_time": 10000, "speaker_name": "프리드리히 니체", "speaker_side": "B", "message": "역시 가치를 아시는군요! 거래는 예술입니다..." }
        ],
        "interactive_options": [
          { "label": "최종 충돌 및 정리 듣기", "next_node_id": "node_003_closing" }
        ]
      },
      {
        "node_id": "node_003_closing",
        "audio_url": "https://cdn.pique.app/audio/battle_001_round3_closing.mp3",
        "audio_duration": 90,
        "scripts": [
          { "start_time": 0,     "speaker_name": "존 롤스",         "speaker_side": "A",    "message": "한 가지 묻겠습니다. 당신이 만약 그 가방의 구매자였다면..." },
          { "start_time": 30000, "speaker_name": "프리드리히 니체", "speaker_side": "B",    "message": "질문이 틀렸소. 명품을 사는 자들은 이미 그 게임의 규칙을 압니다..." },
          { "start_time": 60000, "speaker_name": "나레이션",        "speaker_side": "NONE", "message": "이제 당신의 최종 선택을 들려주세요." }
        ],
        "interactive_options": []
      }
    ]
  },
  "error": null
}
```

---

## 관리자 API

### `POST /api/v1/admin/scenarios`

- 공식 시나리오를 직접 생성합니다. 생성 시 TTS API가 자동 호출되어 `.mp3` 파일이 CDN에 업로드됩니다.

#### Request Body

```json
{
  "battle_id": "battle_001",
  "is_interactive": true,
  "nodes": [
    {
      "node_name": "node_001_opening",
      "is_start_node": true,
      "scripts": [
        { "speaker_name": "나레이션",        "speaker_side": "NONE", "message": "여기 한 여자가 있습니다. 동대문에서 18만 원에 떼온 가방을 1억 원에 팔았습니다..." },
        { "speaker_name": "존 롤스",         "speaker_side": "A",    "message": "재판장님, 시장 경제의 핵심은 '정보의 대칭'입니다..." },
        { "speaker_name": "프리드리히 니체", "speaker_side": "B",    "message": "명품을 사는 사람이 원가를 몰라서 삽니까?..." }
      ],
      "interactive_options": [
        { "label": "사회적 신뢰를 위해 정보의 투명성이 우선이다.", "next_node_name": "node_002_branch_a" },
        { "label": "시장은 개인의 욕망이 만나는 곳이다.",           "next_node_name": "node_002_branch_b" }
      ]
    },
    {
      "node_name": "node_002_branch_a",
      "is_start_node": false,
      "scripts": [
        { "speaker_name": "유저",    "speaker_side": "A", "message": "사회의 기본 신뢰를 위해 투명한 정보 공개가 우선되어야 합니다." },
        { "speaker_name": "존 롤스", "speaker_side": "A", "message": "현명하십니다. 상품의 가치가 전적으로 기만에 의해 결정된다면..." }
      ],
      "interactive_options": [
        { "label": "최종 충돌 및 정리 듣기", "next_node_name": "node_003_closing" }
      ]
    },
    {
      "node_name": "node_002_branch_b",
      "is_start_node": false,
      "scripts": [
        { "speaker_name": "유저",            "speaker_side": "B", "message": "강요 없는 자발적 거래라면, 욕망에 따른 가격 결정은 시장의 자유입니다." },
        { "speaker_name": "프리드리히 니체", "speaker_side": "B", "message": "역시 가치를 아시는군요! 거래는 예술입니다..." }
      ],
      "interactive_options": [
        { "label": "최종 충돌 및 정리 듣기", "next_node_name": "node_003_closing" }
      ]
    },
    {
      "node_name": "node_003_closing",
      "is_start_node": false,
      "scripts": [
        { "speaker_name": "존 롤스",         "speaker_side": "A",    "message": "한 가지 묻겠습니다. 당신이 만약 그 가방의 구매자였다면..." },
        { "speaker_name": "프리드리히 니체", "speaker_side": "B",    "message": "질문이 틀렸소. 명품을 사는 자들은 이미 그 게임의 규칙을 압니다..." },
        { "speaker_name": "나레이션",        "speaker_side": "NONE", "message": "이제 당신의 최종 선택을 들려주세요." }
      ],
      "interactive_options": []
    }
  ]
}
```

#### 성공 응답 `201 Created`

```json
{
  "statusCode": 201,
  "data": {
    "scenario_id": "scenario_001",
    "battle_id": "battle_001",
    "is_interactive": true,
    "status": "DRAFT",
    "creator_type": "ADMIN",
    "nodes": [
      {
        "node_id": "node_001_opening",
        "node_name": "node_001_opening",
        "is_start_node": true,
        "audio_url": "https://cdn.pique.app/audio/battle_001_round1.mp3",
        "audio_duration": 150,
        "interactive_options": [
          { "label": "사회적 신뢰를 위해 정보의 투명성이 우선이다.", "next_node_id": "node_002_branch_a" },
          { "label": "시장은 개인의 욕망이 만나는 곳이다.",           "next_node_id": "node_002_branch_b" }
        ]
      },
      {
        "node_id": "node_002_branch_a",
        "node_name": "node_002_branch_a",
        "is_start_node": false,
        "audio_url": "https://cdn.pique.app/audio/battle_001_round2_a.mp3",
        "audio_duration": 110,
        "interactive_options": [
          { "label": "최종 충돌 및 정리 듣기", "next_node_id": "node_003_closing" }
        ]
      },
      {
        "node_id": "node_002_branch_b",
        "node_name": "node_002_branch_b",
        "is_start_node": false,
        "audio_url": "https://cdn.pique.app/audio/battle_001_round2_b.mp3",
        "audio_duration": 120,
        "interactive_options": [
          { "label": "최종 충돌 및 정리 듣기", "next_node_id": "node_003_closing" }
        ]
      },
      {
        "node_id": "node_003_closing",
        "node_name": "node_003_closing",
        "is_start_node": false,
        "audio_url": "https://cdn.pique.app/audio/battle_001_round3_closing.mp3",
        "audio_duration": 90,
        "interactive_options": []
      }
    ],
    "created_at": "2026-03-10T09:00:00Z"
  },
  "error": null
}
```

---

### `PATCH /api/v1/admin/scenarios/{scenario_id}`

- 시나리오 정보를 수정합니다. 변경할 필드만 포함합니다.

#### Request Body

```json
{
  "status": "PUBLISHED"
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "scenario_id": "scenario_001",
    "battle_id": "battle_001",
    "is_interactive": true,
    "status": "PUBLISHED",
    "creator_type": "ADMIN",
    "updated_at": "2026-03-10T10:00:00Z"
  },
  "error": null
}
```

---

### `DELETE /api/v1/admin/scenarios/{scenario_id}`

- 시나리오를 삭제합니다.

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

- 스케줄러가 자동 생성한 AI 시나리오 초안(`PENDING`)을 관리자가 검수 · 승인 · 반려합니다.

### `GET /api/v1/admin/ai/scenarios`

- AI가 생성한 `PENDING` 상태의 시나리오 목록을 조회합니다.

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "scenario_id": "scenario_ai_001",
        "battle_id": "battle_ai_001",
        "is_interactive": true,
        "status": "PENDING",
        "creator_type": "AI",
        "nodes": [
          {
            "node_id": "node_ai_001_opening",
            "node_name": "node_ai_001_opening",
            "is_start_node": true,
            "audio_url": "https://cdn.pique.app/audio/battle_ai_001_round1.mp3",
            "audio_duration": 140,
            "interactive_options": [
              { "label": "AI 생성 선택지 A", "next_node_id": "node_ai_002_branch_a" },
              { "label": "AI 생성 선택지 B", "next_node_id": "node_ai_002_branch_b" }
            ]
          }
        ],
        "created_at": "2026-03-11T06:00:00Z"
      }
    ],
    "total_count": 2
  },
  "error": null
}
```

---

### `PATCH /api/v1/admin/ai/scenarios/{scenario_id}`

- AI가 생성한 시나리오를 승인하거나 반려합니다. 승인 시 내용을 수정한 뒤 승인할 수 있습니다.

#### Request Body — 승인

```json
{
  "action": "APPROVE",
  "nodes": [
    {
      "node_id": "node_ai_001_opening",
      "scripts": [
        { "speaker_name": "나레이션", "speaker_side": "NONE", "message": "수정된 나레이션 내용..." }
      ],
      "interactive_options": [
        { "label": "수정된 선택지 A", "next_node_id": "node_ai_002_branch_a" },
        { "label": "수정된 선택지 B", "next_node_id": "node_ai_002_branch_b" }
      ]
    }
  ]
}
```

#### Request Body — 반려

```json
{
  "action": "REJECT",
  "reject_reason": "시나리오 흐름이 부자연스러움"
}
```

#### 성공 응답 `200 OK` — 승인

```json
{
  "statusCode": 200,
  "data": {
    "scenario_id": "scenario_ai_001",
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
    "scenario_id": "scenario_ai_001",
    "status": "REJECTED",
    "reject_reason": "시나리오 흐름이 부자연스러움",
    "updated_at": "2026-03-11T09:00:00Z"
  },
  "error": null
}
```

---

## `[후순위]` 크리에이터 API 

### `POST /api/v1/scenarios`

- 시나리오를 제안합니다. (매너 온도 45도 이상 유저)

#### Request Body

```json
{
  "battle_id": "battle_002",
  "is_interactive": false,
  "nodes": [
    {
      "node_name": "node_001_full",
      "is_start_node": true,
      "scripts": [
        { "speaker_name": "나레이션", "speaker_side": "NONE", "message": "AI가 그린 그림 한 장이 경매에서 1억 원에 낙찰됐습니다..." },
        { "speaker_name": "존 로크",  "speaker_side": "A",    "message": "노동을 투입한 자에게 소유권이 있습니다. AI 개발사가 권리를 가져야 합니다." },
        { "speaker_name": "루소",     "speaker_side": "B",    "message": "AI는 인류의 지식을 학습했습니다. 그 결과물은 모두의 것이어야 합니다." }
      ],
      "interactive_options": []
    }
  ]
}
```

#### 성공 응답 `201 Created`

```json
{
  "statusCode": 201,
  "data": {
    "scenario_id": "scenario_002",
    "battle_id": "battle_002",
    "is_interactive": false,
    "status": "PENDING",
    "creator_type": "USER",
    "created_at": "2026-03-10T12:00:00Z"
  },
  "error": null
}
```

---

### `PATCH /api/v1/scenarios/{scenario_id}`

제안한 시나리오를 수정합니다. 변경할 필드만 포함합니다.

#### Request Body

```json
{
  "nodes": [
    {
      "node_name": "node_001_full",
      "is_start_node": true,
      "scripts": [
        { "speaker_name": "나레이션", "speaker_side": "NONE", "message": "AI가 그린 그림 한 장이 경매에서 1억 원에 낙찰됐습니다. (수정)" },
        { "speaker_name": "존 로크",  "speaker_side": "A",    "message": "노동을 투입한 자에게 소유권이 있습니다." },
        { "speaker_name": "루소",     "speaker_side": "B",    "message": "AI는 인류의 지식을 학습했습니다." }
      ],
      "interactive_options": []
    }
  ]
}
```

#### 성공 응답 `200 OK`

```json
{
  "statusCode": 200,
  "data": {
    "scenario_id": "scenario_002",
    "battle_id": "battle_002",
    "is_interactive": false,
    "status": "PENDING",
    "creator_type": "USER",
    "updated_at": "2026-03-10T13:00:00Z"
  },
  "error": null
}
```

---

### `DELETE /api/v1/scenarios/{scenario_id}`

- 제안한 시나리오를 삭제합니다.

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

## 시나리오 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `SCENARIO_NOT_FOUND` | `404` | 존재하지 않는 시나리오 |
| `SCENARIO_NODE_NOT_FOUND` | `404` | 존재하지 않는 노드 |
| `SCENARIO_ALREADY_PUBLISHED` | `409` | 이미 발행된 시나리오 |
| `SCENARIO_TTS_FAILED` | `500` | TTS 생성 실패 |

---