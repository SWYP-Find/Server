# 홈 API 명세서

## 1. 설계 메모

- `Home`은 원천 도메인이 아니라 여러 도메인을 조합하는 집계 API입니다.
- 메인 화면에서 바로 응답하는 즉답형 기능은 `quiz` 도메인으로 분리합니다.
- 홈 화면은 아래 데이터를 한 번에 조합해서 반환합니다.
  - HOT 배틀
  - PICK 배틀
  - 퀴즈
  - 최신 배틀
- 공지는 홈 상단 노출 대상만 조회합니다.

---

## 2. 홈 API

### 2.1 `GET /api/v1/home`

홈 화면 집계 조회 API.

반환 항목:

- HOT 배틀
- PICK 배틀
- 퀴즈 2지선다
- 퀴즈 4지선다
- 최신 배틀 목록

```json
{
  "hot_battle": {
    "battle_id": "battle_001",
    "title": "안락사 도입, 찬성 vs 반대",
    "summary": "인간에게 품위 있는 죽음을 허용해야 할까?",
    "thumbnail_url": "https://cdn.example.com/battle/hot-001.png"
  },
  "pick_battle": {
    "battle_id": "battle_002",
    "title": "공리주의 vs 의무론",
    "summary": "도덕 판단의 기준은 결과일까 원칙일까?",
    "thumbnail_url": "https://cdn.example.com/battle/pick-002.png"
  },
  "quizzes": [
    {
      "quiz_id": "quiz_001",
      "type": "BINARY",
      "title": "AI가 만든 그림도 예술일까?",
      "options": [
        { "code": "A", "label": "그렇다" },
        { "code": "B", "label": "아니다" }
      ]
    },
    {
      "quiz_id": "quiz_002",
      "type": "MULTIPLE_CHOICE",
      "title": "도덕 판단의 기준은?",
      "options": [
        { "code": "A", "label": "결과" },
        { "code": "B", "label": "의도" },
        { "code": "C", "label": "규칙" },
        { "code": "D", "label": "상황" }
      ]
    }
  ],
  "latest_battles": [
    {
      "battle_id": "battle_101",
      "title": "정의란 무엇인가",
      "summary": "정의의 기준은 모두에게 같아야 할까?",
      "thumbnail_url": "https://cdn.example.com/battle/latest-101.png"
    }
  ]
}
```

### 2.2 `POST /api/v1/quiz/{quizId}/responses`

홈 화면에서 퀴즈 응답 저장.

요청:

```json
{
  "selected_option_code": "A"
}
```

응답:

```json
{
  "quiz_id": "quiz_001",
  "selected_option_code": "A",
  "submitted_at": "2026-03-08T12:00:00Z"
}
```

---

## 3. 공지 API

### 3.1 `GET /api/v1/notices`

현재 노출 가능한 전체 공지 목록 조회.

쿼리 파라미터:

- `placement`: 선택, 예시 `HOME_TOP`
- `limit`: 선택

응답:

```json
{
  "items": [
    {
      "notice_id": "notice_001",
      "title": "3월 신규 딜레마 업데이트",
      "body": "매일 새로운 딜레마가 추가돼요.",
      "notice_type": "ANNOUNCEMENT",
      "is_pinned": true,
      "starts_at": "2026-03-01T00:00:00Z",
      "ends_at": "2026-03-31T23:59:59Z"
    }
  ]
}
```
