# 마이페이지 API 명세서

## 1. 설계 메모

- 마이페이지는 원천 도메인이 아니라 사용자, 리캡, 활동 이력을 묶는 조회 API 성격이 강합니다.
- 상단 요약과 상세 목록은 분리해서 조회합니다.

---

## 2. 마이페이지 API

### 2.1 `GET /api/v1/me/mypage`

마이페이지 상단에 필요한 집계 데이터 조회.

응답:

```json
{
  "profile": {
    "user_id": "user_001",
    "nickname": "생각하는올빼미",
    "avatar_emoji": "🦉",
    "manner_temperature": 36.5
  },
  "recap_summary": {
    "personality_title": "원칙 중심형",
    "summary": "감정보다 이성과 규칙을 더 중시하는 편이에요."
  },
  "activity_counts": {
    "comments": 12,
    "posts": 3,
    "liked_contents": 8,
    "changed_mind_contents": 2
  }
}
```

### 2.2 `GET /api/v1/me/recap`

상세 리캡 정보 조회.

응답:

```json
{
  "personality_title": "원칙 중심형",
  "summary": "감정보다 이성과 규칙을 더 중시하는 편이에요.",
  "scores": {
    "score_1": 88,
    "score_2": 74,
    "score_3": 62,
    "score_4": 45,
    "score_5": 30,
    "score_6": 15
  }
}
```

### 2.3 `GET /api/v1/me/activities`

사용자 행동 이력 조회.

쿼리 파라미터:

- `type`: `COMMENT | POST | LIKED_CONTENT | CHANGED_MIND`
- `cursor`: 선택
- `size`: 선택

응답:

```json
{
  "items": [
    {
      "activity_id": "act_001",
      "type": "COMMENT",
      "title": "안락사 도입, 찬성 vs 반대",
      "description": "자기결정권은 가장 기본적인 인권이라고 생각해요.",
      "created_at": "2026-03-08T12:00:00Z"
    }
  ],
  "next_cursor": "cursor_002"
}
```
