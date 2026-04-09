# 홈 API 명세서

## 1. 설계 메모

- 홈은 여러 조회 결과를 한 번에 내려주는 집계 API입니다.
- 이번 문서는 `GET /api/v1/home` 하나만 정의합니다.
- 공지 목록/상세는 홈에서 직접 내려주지 않고, 마이페이지 공지 탭에서 처리합니다.
- 홈에서는 공지 내용 대신 `newNotice` boolean만 내려서 새 공지 유입 여부만 표시합니다.
- `todayPicks` 안에는 찬반형과 4지선다형이 함께 포함됩니다.

---

## 2. 홈 API

### 2.1 `GET /api/v1/home`

홈 화면 진입 시 필요한 데이터를 한 번에 조회합니다.

반환 섹션:

- `newNotice`: 새 공지가 있는지 여부
- `editorPicks`: Editor Pick
- `trendingBattles`: 지금 뜨는 배틀
- `bestBattles`: Best 배틀
- `todayPicks`: 오늘의 Pické
- `newBattles`: 새로운 배틀

```json
{
  "newNotice": true,
  "editorPicks": [
    {
      "battleId": "7b6c8d81-40f4-4f1e-9f13-4cc2fa0a3a10",
      "title": "연애 상대의 전 애인 사진, 지워달라고 말한다 vs 그냥 둔다",
      "summary": "에디터가 직접 골라본 오늘의 주제",
      "thumbnailUrl": "https://cdn.example.com/battle/editor-pick-001.png",
      "type": "BATTLE",
      "viewCount": 182,
      "participantsCount": 562,
      "audioDuration": 153,
      "tags": [],
      "options": []
    }
  ],
  "trendingBattles": [
    {
      "battleId": "40f4c311-0bd8-4baf-85df-58f8eaf1bf1f",
      "title": "안락사 도입, 찬성 vs 반대",
      "summary": "최근 24시간 참여가 급증한 배틀",
      "thumbnailUrl": "https://cdn.example.com/battle/hot-001.png",
      "type": "BATTLE",
      "viewCount": 120,
      "participantsCount": 420,
      "audioDuration": 180,
      "tags": [],
      "options": []
    }
  ],
  "bestBattles": [
    {
      "battleId": "11c22d33-44e5-6789-9abc-123456789def",
      "title": "반려동물 출입 가능 식당, 확대해야 한다 vs 제한해야 한다",
      "summary": "누적 참여와 댓글 반응이 높은 배틀",
      "thumbnailUrl": "https://cdn.example.com/battle/best-001.png",
      "type": "BATTLE",
      "viewCount": 348,
      "participantsCount": 1103,
      "audioDuration": 201,
      "tags": [],
      "options": []
    }
  ],
  "todayPicks": [
    {
      "battleId": "4e5291d2-b514-4d2a-a8fb-1258ae21a001",
      "title": "배달 일회용 수저 기본 제공, 찬성 vs 반대",
      "summary": "오늘의 Pické 찬반형 예시",
      "thumbnailUrl": "https://cdn.example.com/battle/today-vote-001.png",
      "type": "VOTE",
      "viewCount": 97,
      "participantsCount": 238,
      "audioDuration": 96,
      "tags": [],
      "options": [
        {
          "label": "A",
          "text": "찬성"
        },
        {
          "label": "B",
          "text": "반대"
        }
      ]
    },
    {
      "battleId": "9f8e7d6c-5b4a-3210-9abc-7f6e5d4c3b2a",
      "title": "다음 중 세계에서 가장 큰 사막은?",
      "summary": "오늘의 Pické 4지선다형 예시",
      "thumbnailUrl": "https://cdn.example.com/battle/today-quiz-001.png",
      "type": "QUIZ",
      "viewCount": 76,
      "participantsCount": 191,
      "audioDuration": 88,
      "tags": [],
      "options": [
        {
          "label": "A",
          "text": "사하라 사막"
        },
        {
          "label": "B",
          "text": "고비 사막"
        },
        {
          "label": "C",
          "text": "남극 대륙"
        },
        {
          "label": "D",
          "text": "아라비아 사막"
        }
      ]
    }
  ],
  "newBattles": [
    {
      "battleId": "aa11bb22-cc33-44dd-88ee-ff0011223344",
      "title": "회사 회식은 근무의 연장이다 vs 사적인 친목이다",
      "summary": "홈의 다른 섹션과 중복되지 않는 최신 배틀",
      "thumbnailUrl": "https://cdn.example.com/battle/new-001.png",
      "type": "BATTLE",
      "viewCount": 24,
      "participantsCount": 71,
      "audioDuration": 142,
      "tags": [],
      "options": []
    }
  ]
}
```

비고:

- `newNotice`는 홈에서 공지 내용을 직접 노출하지 않고, 마이페이지 공지 탭으로 이동시키기 위한 신규 공지 존재 여부입니다.
- `editorPicks`, `trendingBattles`, `bestBattles`, `newBattles`는 동일한 배틀 요약 카드 구조를 사용합니다.
- `todayPicks`는 `type`으로 찬반형과 4지선다형을 구분합니다.
- `todayPicks`의 4지선다형은 별도 `quizzes` 필드로 분리하지 않고 이 배열 안에 포함합니다.
- 데이터가 없으면 리스트 섹션은 빈 배열을, `newNotice`는 `false`를 반환합니다.
