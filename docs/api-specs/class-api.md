# 클래스 API 명세 — 커스텀 배틀·추천·참여 코드 확인

기준 코드:  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassTopicController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassDebateController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassBattleRecommendationController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassMemberController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassroomController.java`  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminBattleController.java`

대상 이슈: #545 주제 제안·수정 검증, #547 커스텀 배틀 클래스 생성, #548 AI 철학자 토론, #551 Picke 배틀 추천·어드민 클래스 설정, #553 참여 코드 확인

---

## 0. 공통

- 모든 API는 로그인이 필요하다 (`Authorization: Bearer {accessToken}`).
- 응답 필드는 camelCase다.
- 응답은 아래 형식으로 감싸서 내려간다. 에러 판별은 `error.code`로 한다.

```json
{ "statusCode": 200, "data": { } }
```

```json
{ "statusCode": 409, "error": { "code": "CLASS_409_FULL", "message": "..." } }
```

- `difficulty`: `BEGINNER` / `INTERMEDIATE` / `ADVANCED`
- `category`: 한글 이름으로 주고받는다. 값은 `철학` / `사회` / `문학` / `과학` / `예술` / `역사`다. 추천 API 쿼리는 enum 이름(`PHILOSOPHY`)도 받는다.
- `philosopher`: 철학자 코드 (`KANT` 등, `PhilosopherType`)

### 화면 흐름

**새 클래스 만들기, Picke 배틀**
1. 추천: `GET /class-battle-recommendations`
2. 클래스 생성: `POST /classes`에 `battleId`를 보낸다.

**새 클래스 만들기, 커스텀 배틀**
1. 초안 생성: `POST /class-topic-drafts`
2. AI 제안: `POST /class-topic-drafts/{draftId}/suggestions`
3. 다음 둘 중 하나로 주제 버전을 만든다.
   - 후보 선택: `POST /class-topic-drafts/{draftId}/selection`
   - 직접 수정 검증: `POST /class-topic-drafts/{draftId}/validations`
4. 클래스 생성: `POST /classes`에 `topicVersionId`를 보낸다.

**클래스 참여**
1. 코드 확인: `GET /classes/join/preview`
2. 이름 입력 뒤 참여: `POST /classes/join`

**커스텀 배틀 진행**
1. 사전투표
2. 토론 시작: `POST /classes/{classId}/battle/debate`
3. 반박 제출 ×3: `POST /classes/{classId}/battle/debate/replies`
4. 사후투표

---

## 1. Picke 배틀 추천 (#551)

### 1.1 조건에 맞는 배틀 추천
- `GET /api/v1/class-battle-recommendations`
- 쿼리 파라미터:

| 이름 | 필수 | 설명 |
|---|---|---|
| `difficulty` | O | 대상 수준 |
| `categories` | O | 카테고리 1~3개. `categories=철학,사회` 또는 `categories=철학&categories=사회` |
| `keyword` | X | 찾고 싶은 주제. 50자 이하이고 쉼표·공백으로 여러 단어를 넣을 수 있다 (앞 5단어만 사용) |

- **후보:** 공개(PUBLISHED) 상태이고 삭제되지 않았으며, 어드민이 클래스 추천을 켠 배틀이다. 그중 수준이 같고 카테고리가 하나라도 겹치는 배틀만 남긴다.
- **keyword를 넣으면:** 제목·요약·설명에 단어가 하나 이상 들어간 배틀만 남긴다.
- **정렬:** 일치한 단어 수 → 에디터 픽 → 참여자 수 → 최신순
- **개수:** 최대 3개다. 조건에 맞는 배틀이 적으면 채우지 않고 0~2개를 그대로 돌려준다. 빈 결과일 때 보여줄 안내 화면이 필요하다.

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "battleId": 12,
        "title": "AI가 쓴 글도 창작일까?",
        "summary": "...",
        "thumbnailUrl": "https://...",
        "category": "철학",
        "difficulty": "BEGINNER",
        "audioDuration": 180,
        "options": [
          { "optionId": 31, "position": "A", "title": "창작이다", "stance": "...", "representative": "니체", "imageUrl": "https://..." },
          { "optionId": 32, "position": "B", "title": "창작이 아니다", "stance": "...", "representative": "칸트", "imageUrl": "https://..." }
        ]
      }
    ]
  }
}
```

- `audioDuration`은 초 단위다.
- 미리듣기 음성은 `GET /api/v1/battles/{battleId}/scenario`로 받는다.

| 상황 | 응답 |
|---|---|
| 수준 누락, 카테고리 0개·4개 이상·잘못된 값, 주제 50자 초과 | 400 `CLASS_400_RECOMMEND_CONDITION` |

---

## 2. 커스텀 배틀 주제 (#545)

### 2.1 주제 초안 생성
- `POST /api/v1/class-topic-drafts`
- AI를 호출하지 않는다. 초안마다 제안 3회와 수정 검증 3회가 새로 주어진다.

```json
{
  "topicSeed": "카뮈 『이방인』의 뫼르소처럼 어머니 장례식에서 울지 않은 사람을 비난할 수 있을까",
  "difficulty": "INTERMEDIATE",
  "categories": ["철학", "문학"]
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `topicSeed` | O | 주제·관심사. 앞뒤 공백 제거 후 2~200자 |
| `difficulty` | O | 대상 수준 |
| `categories` | O | 카테고리 1~3개 (중복은 제거) |

- 응답: `ClassTopicDraftResponse` (2.6 참고). 처음에는 `suggestStatus=NONE`이고 `batchNo=0`이다.

| 상황 | 응답 |
|---|---|
| 사용 중인 이용권 없음 | 403 `CLASS_403_TICKET` |
| 주제 길이·카테고리 개수 위반 | 400 `CLASS_400_TOPIC_CONDITION` |

### 2.2 주제 초안 조회
- `GET /api/v1/class-topic-drafts/{draftId}`
- 저장된 후보, 상태, 남은 횟수를 돌려준다. AI는 호출하지 않는다.
- 제안 중에 화면을 나갔다가 돌아오면 이 API로 결과를 이어서 본다.
- 본인 초안이 아니거나 없는 초안이면 404 `CLASS_404_TOPIC_DRAFT`다.

### 2.3 AI 주제 제안 (최초·재추천)
- `POST /api/v1/class-topic-drafts/{draftId}/suggestions`
- 본문은 생략할 수 있다. 조건을 바꿔 재추천할 때만 바꿀 필드를 채운다. null 필드는 초안의 기존 조건을 쓴다.

```json
{ "topicSeed": "...", "difficulty": "BEGINNER", "categories": ["사회"] }
```

- 응답은 `ClassTopicDraftResponse`다. 결과는 `suggestStatus`로 구분한다.

| suggestStatus | 의미 | 화면 처리 |
|---|---|---|
| `SUCCEEDED` | 후보 3개 생성. `batchNo`가 1 늘고 제안 횟수가 1 줄어든다 | 후보 표시 |
| `NEEDS_CLARIFICATION` | 맥락이 부족함 | `clarificationMessage` 표시 |
| `FAILED` | AI 생성 실패. 기존 후보와 횟수는 그대로다 | 재시도 안내 |
| `GENERATING` | 생성 중 | "생각을 정리하고 있어요" |

| 상황 | 응답 |
|---|---|
| 성공 배치 3회 소진 | 409 `CLASS_409_TOPIC_SUGGEST_LIMIT` |
| 같은 초안이 이미 생성 중 (3분 지나면 다시 허용) | 409 `CLASS_409_TOPIC_GENERATING` |
| 같은 초안에 분당 2회 초과 | 429 `CLASS_429_TOPIC` |
| 조건 위반 | 400 `CLASS_400_TOPIC_CONDITION` |
| 초안 없음 | 404 `CLASS_404_TOPIC_DRAFT` |

### 2.4 후보 선택
- `POST /api/v1/class-topic-drafts/{draftId}/selection`
- 고른 후보를 검증된 주제 버전으로 저장한다. AI는 호출하지 않는다.

```json
{ "batchNo": 1, "candidateId": 3 }
```

- `batchNo`에는 화면에 보인 배치 번호를 보낸다.
- 같은 후보를 다시 고르면 기존 버전을 그대로 돌려준다.
- 응답: `ClassTopicVersionResponse` (2.6 참고)

| 상황 | 응답 |
|---|---|
| `batchNo`가 현재 배치와 다름 (그 사이 재추천됨) | 409 `CLASS_409_TOPIC_CANDIDATE` |
| 없는 후보 ID | 404 `CLASS_404_TOPIC_CANDIDATE` |

### 2.5 직접 수정 검증
- `POST /api/v1/class-topic-drafts/{draftId}/validations`
- 저장된 버전을 바탕으로 질문·설명·양측 선택지를 함께 보낸다. 카테고리·수준은 바탕 버전 값을 그대로 쓴다.

```json
{
  "baseVersionId": 12,
  "question": "표현하지 않은 감정까지 타인이 판단해도 될까요?",
  "description": "감정 표현의 개인적 자유와 공동체의 기대를 비교해 봐요.",
  "options": [
    { "label": "판단할 수 있다", "rationale": "표현은 관계에 대한 책임의 일부다", "philosopher": "KANT" },
    { "label": "판단할 수 없다", "rationale": "...", "philosopher": null }
  ]
}
```

| 필드 | 제한 |
|---|---|
| `question` | 60자 이하 |
| `description` | 80자 이하 |
| `options` | 2개 (A, B 순서) |
| `options[].label` | 20자 이하 |
| `options[].rationale` | 40자 이하 |
| `options[].philosopher` | 비우면 바탕 버전의 철학자를 그대로 쓴다 |

- **길이·필수값 오류:** AI를 호출하지 않고 `issues`로 돌려준다. 검증 횟수는 쓰지 않는다.
- **통과:** `valid=true`가 되고 새 주제 버전이 만들어진다.
- **실패:** `valid=false`와 `issues`를 돌려준다. 마지막으로 검증된 버전은 그대로 유지된다.
- AI는 수정안을 고쳐 쓰지 않고 판정만 한다.

```json
{
  "statusCode": 200,
  "data": {
    "valid": false,
    "issues": [
      { "field": "options[1].label", "code": "TOO_LONG", "message": "선택 문구은(는) 20자 이하로 적어 주세요. (현재 23자)" }
    ],
    "validatedTopicVersionId": null,
    "remainingValidations": 3,
    "version": null
  }
}
```

- **`issues[].field` 값:** `question`, `description`, `options[i].label`, `options[i].rationale`, `options[i].philosopher`, `topic`
- **`issues[].code` 값**
  - 입력 오류 (횟수 차감 없음): `REQUIRED`, `TOO_LONG`, `DUPLICATE_OPTION`, `SAME_PHILOSOPHER`
  - AI 판정: `OPTIONS_NOT_SAME_AXIS`, `OPTION_MISMATCH_QUESTION`, `DESCRIPTION_MISMATCH`, `RATIONALE_MISMATCH`, `PHILOSOPHER_MISMATCH`, `IMPLIES_ANSWER`, `DIGNITY`, `OTHER`

| 상황 | 응답 |
|---|---|
| 검증 3회 소진 | 409 `CLASS_409_TOPIC_VALIDATION_LIMIT` |
| AI 장애 (횟수 차감 없음) | 503 `CLASS_503_TOPIC_AI` |
| `baseVersionId`가 이 초안의 버전이 아님 | 404 `CLASS_404_TOPIC_VERSION` |

### 2.6 응답 객체

`ClassTopicDraftResponse`

| 필드 | 설명 |
|---|---|
| `draftId` | 초안 ID |
| `topicSeed`, `difficulty`, `categories` | 마지막 성공 배치 기준 조건 |
| `suggestStatus` | `NONE` / `GENERATING` / `SUCCEEDED` / `NEEDS_CLARIFICATION` / `FAILED` |
| `clarificationMessage` | 맥락 부족 안내 (nullable) |
| `batchNo` | 현재 후보의 배치 번호 (0이면 후보 없음) |
| `remainingSuggestions` | 남은 제안 횟수 (최대 3) |
| `remainingValidations` | 남은 수정 검증 횟수 (최대 3) |
| `candidates[]` | `candidateId`(1~3), `question`, `description`, `category`, `difficulty`, `options[]` |
| `latestVersion` | 가장 최근 검증된 주제 버전 (nullable) |

`ClassTopicVersionResponse`

| 필드 | 설명 |
|---|---|
| `topicVersionId` | 클래스 생성에 쓰는 ID |
| `draftId`, `versionNo` | 초안 ID, 초안 안의 버전 번호 (1부터) |
| `source` | `CANDIDATE`(AI 후보 선택) / `EDITED`(직접 수정 후 검증 통과) |
| `question`, `description`, `category`, `difficulty`, `createdAt` | |
| `options[]` | `position`(A/B), `label`, `rationale`, `philosopher`, `philosopherName`, `philosopherImageKey` |

---

## 3. 클래스 생성 변경 (#547)

### 3.1 클래스 생성
- `POST /api/v1/classes`
- 기존 요청에 `topicVersionId`가 추가됐다. `battleId`와 `topicVersionId` 중 **하나만** 보낸다.

```json
{
  "name": "3학년 2반 철학 토론",
  "battleId": null,
  "topicVersionId": 3,
  "deadline": "2026-12-31T23:59:59",
  "commentRequired": true,
  "ownerParticipates": true,
  "ownerDisplayName": "김선생"
}
```

- **`battleId`를 보내면:** 기존 콘텐츠(TTS) 클래스가 된다. `battleType=TTS`
- **`topicVersionId`를 보내면:** 클래스 전용 커스텀 배틀을 새로 만든다. `battleType=CUSTOM`
  - 썸네일은 환경변수 `CLASS_CUSTOM_BATTLE_THUMBNAIL_URL`의 고정 이미지 1종이다. 비어 있으면 썸네일 없이 만든다.
- 클래스 상세 응답(`ClassDetailResponse`)에 `topicVersionId`가 추가됐다. TTS 클래스는 null이다.

| 상황 | 응답 |
|---|---|
| 둘 다 보내거나 둘 다 비움 | 400 `CLASS_400_BATTLE_SOURCE` |
| 없는 버전이거나 본인 초안의 버전이 아님 | 404 `CLASS_404_TOPIC_VERSION` |
| 같은 초안으로 이미 클래스를 만듦 (초안 1개당 클래스 1개) | 409 `CLASS_409_TOPIC_DRAFT_USED` |

### 3.2 커스텀 클래스의 진행 차이
- **TTS 단계:** 없다. TTS 완료 API(`POST /api/v1/classes/{classId}/battle/votes/tts-complete`)를 부르면 409 `CLASS_409_TTS`다.
- **사후투표:** AI 철학자 토론을 마친(COMPLETED) 뒤에만 할 수 있다. 그 전이면 409 `CLASS_409_DEBATE_INCOMPLETE`다.

---

## 4. AI 철학자 토론 (#548)

커스텀 배틀 클래스에서 사전투표를 한 뒤, 반대 입장의 AI 철학자와 3라운드 텍스트 토론을 한다.

- **권한**
  - 조회: 클래스를 볼 수 있으면 된다. 마감 후에도 읽을 수 있다.
  - 시작·반박·재시도: 활성 멤버만, 마감 전에만 할 수 있다.
- **라운드 진행:** 1라운드는 AI 주장으로 시작하고, 이어서 참가자가 반박한다. 3라운드 반박을 내면 토론이 끝나고 `COMPLETED`가 된다.
- **AI 주장 길이:** 50~500자
- **반박 길이:** 앞뒤 공백 제거 후 50~1,000자. 저장한 뒤에는 수정할 수 없다.

### 4.1 토론 조회 (재접속)
- `GET /api/v1/classes/{classId}/battle/debate`
- 저장된 대화와 상태를 돌려준다. AI는 호출하지 않는다.
- 시작 전이면 404 `CLASS_404_DEBATE`다.

### 4.2 토론 시작
- `POST /api/v1/classes/{classId}/battle/debate`
- **세션 구성:** 사전투표한 선택지의 반대편, 그리고 그 반대편 선택지의 철학자로 세션을 만든다. 이어서 1라운드 AI 주장을 생성한다.
- **사전투표 잠금:** 시작한 뒤에는 사전투표를 바꿀 수 없다.
- **중복 호출:** 이미 시작했으면 저장된 대화를 그대로 돌려준다.
- **AI 생성 실패:** 200과 함께 `status=FAILED`로 응답한다. 이때는 재시도 API를 쓴다.

| 상황 | 응답 |
|---|---|
| 커스텀 클래스가 아님 | 409 `CLASS_409_DEBATE_TYPE` |
| 사전투표 전 | 409 `CLASS_409_DEBATE_PRE_VOTE` |
| 마감됨 / 이용권 종료 | 409 `CLASS_409_CLOSED` / 409 `CLASS_409_TICKET_ENDED` |

### 4.3 반박 제출
- `POST /api/v1/classes/{classId}/battle/debate/replies`

```json
{
  "roundIndex": 1,
  "content": "속으로 슬퍼해도 밖으로 표현하지 않을 수 있어요. ...",
  "requestKey": "4f0b5c3e-6a8d-4f6b-9a51-2f3d1c7e9b10"
}
```

- `roundIndex`에는 화면에 보인 현재 라운드를 보낸다.
- `requestKey`는 중복 전송을 막는 키다. 64자 이하이고 UUID를 권장한다.
  - 같은 반박을 다시 보낼 때는 같은 키를 쓴다. 같은 키로 다시 보내면 저장된 결과를 그대로 돌려준다.
- 반박을 저장한 뒤 다음 라운드 AI 주장을 생성한다.
  - 3라운드 반박이면 AI 주장 없이 `COMPLETED`가 된다.

| 상황 | 응답 |
|---|---|
| 반박 길이 위반 | 400 `CLASS_400_DEBATE_REPLY` |
| 다른 키로 같은 라운드를 다시 제출 | 409 `CLASS_409_DEBATE_ROUND` |
| 반박을 받을 수 없는 상태 (생성 중·실패·완료) | 409 `CLASS_409_DEBATE_STATE` |
| 시작 전 | 404 `CLASS_404_DEBATE` |

### 4.4 AI 주장 재시도
- `POST /api/v1/classes/{classId}/battle/debate/retry`
- 같은 라운드의 AI 주장을 다시 생성한다.
- `status=FAILED`이거나 생성이 3분 넘게 멈췄을 때만 할 수 있다. 그 외에는 409 `CLASS_409_DEBATE_RETRY`다.

### 4.5 응답 `ClassDebateResponse`

```json
{
  "statusCode": 200,
  "data": {
    "sessionId": 7,
    "status": "WAITING_USER",
    "round": 1,
    "canReply": true,
    "canRetry": false,
    "philosopher": { "code": "KANT", "name": "칸트", "typeName": "...", "imageKey": "..." },
    "participantOption": { "optionId": 31, "position": "A", "title": "...", "stance": "..." },
    "opponentOption": { "optionId": 32, "position": "B", "title": "...", "stance": "..." },
    "messages": [
      {
        "messageId": 101, "round": 1, "speaker": "AI", "content": "...",
        "focus": "ASSUMPTION", "focusLabel": "가정", "replyToMessageId": null,
        "createdAt": "2026-10-21T10:00:00"
      }
    ],
    "aiNotice": "철학자의 관점을 응용한 AI의 주장이에요. 실제 인물이 한 말이 아니에요.",
    "replyMinLength": 50,
    "replyMaxLength": 1000
  }
}
```

| status | 의미 | 화면 |
|---|---|---|
| `GENERATING` | AI 주장 생성 중 | "생각을 정리하고 있어요" |
| `WAITING_USER` | 반박 대기 | 입력창 |
| `FAILED` | AI 주장 생성 실패 | 재시도 버튼 (`canRetry`) |
| `COMPLETED` | 3라운드 종료 | 대화 다시보기 / 사후투표 |

- `canReply`: `WAITING_USER`이고 마감 전일 때 true
- `canRetry`: 실패했거나 생성이 오래 멈췄고 마감 전일 때 true
- `focus`: AI 주장의 초점이다. 값은 `EVIDENCE`(근거) / `ASSUMPTION`(가정) / `COUNTEREXAMPLE`(반례) / `CONDITION`(조건) / `VALUE_CONFLICT`(가치충돌)이고, 참가자 반박이면 null이다.
- `replyToMessageId`: AI 주장이 응답한 참가자 반박의 ID

---

## 5. 참여 코드 확인 (#553)

### 5.1 참여 코드 확인
- `GET /api/v1/classes/join/preview?inviteCode=AB12CD`
- 참여 처리 없이 코드만 확인한다 (코드 입력 단계).
- 코드에 들어간 공백·하이픈과 대소문자는 서버가 정리한다.

```json
{
  "statusCode": 200,
  "data": {
    "inviteCode": "AB12CD",
    "name": "3학년 2반 철학 토론",
    "battleTitle": "AI가 쓴 글도 창작일까?",
    "battleThumbnailUrl": "https://...",
    "battleType": "TTS",
    "memberCount": 12,
    "capacity": 30,
    "closeAt": "2026-12-31T23:59:59"
  }
}
```

- `inviteCode`는 정리된 6자 코드다. 참여 API에 이 값을 그대로 보낸다.
- `closeAt`은 실제 마감 시각으로, 마감일과 이용권 종료일 중 이른 쪽이다.

| 상황 (판정 순서) | 응답 |
|---|---|
| 코드 형식 오류 (공백·하이픈 제거 후 6자가 아님) | 400 `COMMON_400` |
| 없는 코드 | 404 `CLASS_404_CODE` |
| 삭제된 클래스 | 409 `CLASS_409_DELETED` |
| 내가 만든 클래스 | 409 `CLASS_409_OWNER` |
| 이미 참여 중 | 409 `CLASS_409_JOINED` |
| 내보내진 클래스 | 403 `CLASS_403_REMOVED` |
| 이용권 종료 | 409 `CLASS_409_TICKET_ENDED` |
| 마감일 경과 | 409 `CLASS_409_CLOSED` |
| 정원 초과 | 409 `CLASS_409_FULL` |

- 이름 중복은 이 API가 아니라 참여 API에서 판정한다.

### 5.2 참여 API 변경 (`POST /api/v1/classes/join`)
- **삭제된 클래스:** 없는 코드와 구분해 409 `CLASS_409_DELETED`를 돌려준다.
- **이미 참여 중:** 기존대로 200과 클래스 상세를 돌려준다. 다시 요청해도 안전하게 하려는 동작이다. 그래서 "이미 참여 중" 팝업은 코드 확인 단계에서 띄운다.
- **같은 검사를 한 번 더 함:** 코드 확인 뒤 이름을 입력하는 사이에 정원이 차거나 마감될 수 있다. 그래서 참여 API에서도 같은 검사를 다시 한다.

---

## 6. 어드민 배틀 클래스 설정 (#551)

### 6.1 배틀 클래스 설정 수정
- `PATCH /api/v1/admin/battles/{battleId}/class-settings` (ADMIN 권한)
- null 필드는 변경하지 않는다.

```json
{
  "difficulty": "BEGINNER",
  "category": "철학",
  "classRecommendable": true,
  "midVoteEnabled": false
}
```

| 필드 | 설명 |
|---|---|
| `difficulty` | 추천 필터용 대상 수준 |
| `category` | 추천 필터용 카테고리 1개 |
| `classRecommendable` | 클래스 추천(1.1)에 노출할지 |
| `midVoteEnabled` | 중간투표 콘텐츠 여부 |

- 응답: `AdminBattleDetailResponse`. 끝에 `difficulty`, `category`, `classRecommendable`, `midVoteEnabled`가 추가됐다. 어드민 배틀 상세 조회에도 같은 필드가 내려간다.
- 추천 API는 이 설정이 켜진 배틀만 돌려준다. 그래서 운영에서 배틀마다 설정해야 추천 결과가 나온다.

| 상황 | 응답 |
|---|---|
| 추천을 켜는데 수준·카테고리가 비어 있음 | 400 `CLASS_400_RECOMMEND_SETTINGS` |
| 사용자가 만든(커스텀) 배틀 | 400 `CLASS_400_BATTLE` |

---

## 7. 추가된 에러 코드 모음

| 코드 | HTTP | 메시지 |
|---|---|---|
| `CLASS_400_RECOMMEND_CONDITION` | 400 | 대상 수준과 카테고리 1~3개를 골라 주세요. 주제는 50자까지 입력할 수 있어요. |
| `CLASS_400_RECOMMEND_SETTINGS` | 400 | 클래스 추천을 켜려면 수준과 카테고리를 먼저 정해 주세요. |
| `CLASS_400_BATTLE_SOURCE` | 400 | 기존 배틀 ID 와 주제 버전 ID 중 하나만 보내 주세요. |
| `CLASS_400_TOPIC_CONDITION` | 400 | 주제는 2~200자, 카테고리는 1~3개로 입력해 주세요. |
| `CLASS_400_DEBATE_REPLY` | 400 | 반박은 50자 이상 1,000자 이하로 적어 주세요. |
| `CLASS_404_TOPIC_DRAFT` | 404 | 존재하지 않는 주제 초안입니다. |
| `CLASS_404_TOPIC_CANDIDATE` | 404 | 존재하지 않는 주제 후보입니다. |
| `CLASS_404_TOPIC_VERSION` | 404 | 존재하지 않는 주제 버전입니다. |
| `CLASS_404_DEBATE` | 404 | 시작한 토론이 없습니다. |
| `CLASS_409_DELETED` | 409 | 삭제되어 더 이상 이용할 수 없는 클래스입니다. |
| `CLASS_409_JOINED` | 409 | 이미 참여 중인 클래스입니다. |
| `CLASS_409_TOPIC_DRAFT_USED` | 409 | 이 주제 초안으로 이미 만든 클래스가 있습니다. |
| `CLASS_409_TTS` | 409 | 커스텀 배틀 클래스에는 음성(TTS) 단계가 없습니다. |
| `CLASS_409_TOPIC_GENERATING` | 409 | 주제를 만들고 있어요. 잠시 후 다시 확인해 주세요. |
| `CLASS_409_TOPIC_SUGGEST_LIMIT` | 409 | 이 초안의 주제 추천 횟수를 모두 사용했습니다. |
| `CLASS_409_TOPIC_VALIDATION_LIMIT` | 409 | 이 초안의 수정 검증 횟수를 모두 사용했습니다. |
| `CLASS_409_TOPIC_CANDIDATE` | 409 | 후보가 새로 추천되었습니다. 최신 후보에서 다시 골라 주세요. |
| `CLASS_409_DEBATE_TYPE` | 409 | 커스텀 배틀 클래스에서만 토론할 수 있습니다. |
| `CLASS_409_DEBATE_PRE_VOTE` | 409 | 사전투표를 먼저 해 주세요. |
| `CLASS_409_DEBATE_STATE` | 409 | 지금은 반박을 보낼 수 없어요. |
| `CLASS_409_DEBATE_ROUND` | 409 | 이 라운드의 반박은 이미 제출되었습니다. |
| `CLASS_409_DEBATE_RETRY` | 409 | 지금은 다시 시도할 수 없어요. |
| `CLASS_409_DEBATE_INCOMPLETE` | 409 | AI 철학자와의 토론을 마친 뒤 사후투표를 할 수 있어요. |
| `CLASS_429_TOPIC` | 429 | 요청이 많아 잠시 기다리고 있어요. 잠시 후 다시 시도해 주세요. |
| `CLASS_503_TOPIC_AI` | 503 | 응답을 준비하지 못했어요. 다시 시도해 주세요. |
