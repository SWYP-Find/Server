# 클래스 API 명세

기준 코드:  
`src/main/java/com/swyp/picke/domain/ticket/controller/ClassTicketController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassroomController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassBattleRecommendationController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassTopicController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassMemberController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassBattleController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassDebateController.java`  
`src/main/java/com/swyp/picke/domain/classroom/controller/ClassResultController.java`  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminBattleController.java`  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminFundingController.java`

## 목차

0. 공통
1. 이용권
2. 클래스 생성·조회·수정·삭제
3. Picke 배틀 추천
4. 커스텀 배틀 주제
5. 참여·멤버
6. 클래스 배틀 진행 (투표·TTS·의견·댓글)
7. AI 철학자 토론 (커스텀 배틀)
8. 결과·AI 리포트
9. 어드민 (배틀 클래스 설정, 펀딩·이용권)
10. 에러 코드 모음

---

## 0. 공통

### 0.1 요청·응답

- 어드민을 포함한 모든 API는 로그인이 필요하다 (`Authorization: Bearer {accessToken}`). 어드민 API는 ADMIN 권한도 필요하다.
- 응답 필드는 camelCase다.
- 시각은 KST 기준 `LocalDateTime`이고 `2026-12-31T23:59:59` 형식이다.
- 필수값 누락, 잘못된 enum 값, JSON 형식 오류는 400 `COMMON_400`이다. 각 API 표에는 따로 적지 않는다.
- 응답은 아래 형식으로 감싸서 내려간다. 에러는 `error.code`로 판별한다.

```json
{ "statusCode": 200, "data": { } }
```

```json
{ "statusCode": 409, "error": { "code": "CLASS_409_FULL", "message": "정원이 가득 찬 클래스입니다." } }
```

### 0.2 공통 값

| 값 | 종류 |
|---|---|
| `difficulty` | `BEGINNER` / `INTERMEDIATE` / `ADVANCED` |
| `category` | 한글 이름으로 주고받는다: `철학` / `사회` / `문학` / `과학` / `예술` / `역사`. 추천 API 쿼리는 enum 이름(`PHILOSOPHY`)도 받는다 |
| `philosopher` | 철학자 코드 (`KANT` 등, `PhilosopherType`) |
| `myRole` / `role` | `OWNER`(운영자) / `MEMBER`(참여자) |
| `battleType` | `TTS`(기존 콘텐츠 배틀) / `CUSTOM`(AI 주제로 만든 클래스 전용 배틀) |
| `status` (투표 진행 단계) | `NONE` → `PRE_VOTE` → `TTS_LISTENING` → `POST_VOTE` → `COMPLETED` |

### 0.3 클래스 권한 규칙

| 구분 | 누가 | 언제 | 거절 시 응답 |
|---|---|---|---|
| 읽기 (상세·명단·투표 통계·의견·댓글 목록·토론 조회) | 운영자 또는 참여 중인 멤버 | 마감 후에도 가능 | 403 `CLASS_403_MEMBER` |
| 쓰기 (투표·의견·댓글·토론) | 참여 중인 멤버만. `ownerParticipates=false`인 운영자는 쓸 수 없다 | 실제 마감 시각(`closeAt`) 전까지 | 403 `CLASS_403_MEMBER`, 이용권 종료 409 `CLASS_409_TICKET_ENDED`, 마감일 경과 409 `CLASS_409_CLOSED` |
| 결과·리포트 | 읽기 권한이 있는 사람 | 이용권 종료 + 30일까지 | 409 `CLASS_409_RESULT_EXPIRED` |

- 클래스가 없거나 삭제됐으면 404 `CLASS_404`다.
- 실제 마감 시각 `closeAt`은 운영자가 정한 마감일과 이용권 종료일 중 이른 쪽이다. 마감일이 없으면 이용권 종료일이다.

### 0.4 화면 흐름

**운영자: 이용권 등록**
1. 코드 확인: `GET /class-tickets/preview`
2. 등록: `POST /class-tickets/register`

**운영자: Picke 배틀로 클래스 만들기**
1. 추천: `GET /class-battle-recommendations`
2. 클래스 생성: `POST /classes`에 `battleId`를 보낸다.

**운영자: 커스텀 배틀로 클래스 만들기**
1. 초안 생성: `POST /class-topic-drafts`
2. AI 제안: `POST /class-topic-drafts/{draftId}/suggestions`
3. 다음 둘 중 하나로 주제 버전을 만든다.
   - 후보 선택: `POST /class-topic-drafts/{draftId}/selection`
   - 직접 수정 검증: `POST /class-topic-drafts/{draftId}/validations`
4. 클래스 생성: `POST /classes`에 `topicVersionId`를 보낸다.

**참여자: 참여**
1. 코드 확인: `GET /classes/join/preview`
2. 이름 입력 뒤 참여: `POST /classes/join`

**참여자: TTS 배틀 진행**
1. 사전투표: `POST /classes/{id}/battle/votes/pre`
2. (중간투표가 있는 콘텐츠만) 선택 지점마다 콘텐츠 중 선택: `POST /classes/{id}/battle/votes/mid`
3. TTS 청취 완료: `POST /classes/{id}/battle/votes/tts-complete`
4. 사후투표: `POST /classes/{id}/battle/votes/post`
5. 의견 작성: `POST /classes/{id}/battle/perspectives`

**참여자: 커스텀 배틀 진행**
1. 사전투표: `POST /classes/{id}/battle/votes/pre`
2. AI 토론 시작: `POST /classes/{id}/battle/debate`
3. 반박 제출 ×3: `POST /classes/{id}/battle/debate/replies`
4. 사후투표: `POST /classes/{id}/battle/votes/post`
5. 의견 작성: `POST /classes/{id}/battle/perspectives`

**마감 후: 결과 확인**
- 결과: `GET /classes/{id}/result`
- 내 AI 피드백: `GET /classes/{id}/result/my-report`
- 인사이트(운영자): `GET /classes/{id}/result/insight`

---

## 1. 이용권

- 펀딩 후원자가 받은 이용권 코드를 등록하면 클래스를 만들 수 있다.
- 이용 기간은 등록 시점부터 6개월이다.
- 정원은 리워드 티어(5 / 15 / 30석)를 따른다.
- 계정당 사용 중인(ACTIVE) 이용권은 1개다.
- 코드의 공백·하이픈·대소문자는 무시한다.

### 1.1 이용권 코드 확인 (미리보기)
- `GET /api/v1/class-tickets/preview?code=PICKE-AB12-CD34`
- 코드를 소비하지 않고 정원과 등록 기한을 보여준다.

```json
{
  "statusCode": 200,
  "data": { "capacity": 30, "registerBy": "2027-09-01T00:00:00", "usageMonths": 6, "registeredByMe": false }
}
```

- `registerBy`: 이 코드를 등록할 수 있는 마지막 시각 (결제 확정일 + 1년)
- `registeredByMe`: 내가 이미 등록한 코드면 true다. 이때는 등록 대신 내 이용권 조회(1.3)로 보내면 된다.

| 상황 | 응답 |
|---|---|
| 빈 코드 | 400 `COMMON_400` |
| 없는 코드 | 404 `TICKET_404_CODE` |
| 재발급·환불로 무효화된 코드 | 409 `TICKET_409_REVOKED` |
| 등록 기한 경과 | 409 `TICKET_409_REG_EXP` |
| 다른 계정이 이미 등록 | 409 `TICKET_409_CLAIMED` |
| 내가 이미 사용 중인 이용권이 있음 | 409 `TICKET_409_ACTIVE` |

### 1.2 이용권 코드 등록
- `POST /api/v1/class-tickets/register`

```json
{ "code": "PICKE-AB12-CD34" }
```

- 등록하면 즉시 ACTIVE가 되고 6개월 뒤 만료된다.
- 응답: `ClassTicketResponse` (1.3과 같다)
- 내가 이미 등록한 코드를 다시 보내면 거절하지 않고 기존 결과를 돌려준다. 이용 기간은 바뀌지 않는다.
- 에러는 1.1과 같다.

### 1.3 내 이용권 조회
- `GET /api/v1/class-tickets/me`
- 사용 중(ACTIVE) 이용권을 돌려준다. 없으면 가장 최근에 만료된(EXPIRED) 이용권을 돌려준다.
- 둘 다 없으면 404 `TICKET_404`다.

```json
{
  "statusCode": 200,
  "data": {
    "ticketId": 3,
    "capacity": 30,
    "status": "ACTIVE",
    "activatedAt": "2026-10-01T10:00:00",
    "expiresAt": "2027-04-01T10:00:00",
    "remainingDays": 162,
    "readGraceEndsAt": "2027-05-01T10:00:00"
  }
}
```

- `remainingDays`: 만료까지 남은 일수. 만료됐으면 0이다.
- `readGraceEndsAt`: 결과를 읽을 수 있는 마지막 시각 (`expiresAt` + 30일)

---

## 2. 클래스 생성·조회·수정·삭제

### 2.1 클래스 생성
- `POST /api/v1/classes`

```json
{
  "name": "3학년 2반 철학 토론",
  "battleId": 12,
  "topicVersionId": null,
  "deadline": "2026-12-31T23:59:59",
  "commentRequired": true,
  "ownerParticipates": true,
  "ownerDisplayName": "김선생",
  "requestKey": "3f2b8c1e-7d4a-4f6b-9a0e-2c5d8e1f4a7b"
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `name` | O | 클래스 이름 1~20자 |
| `battleId` | △ | 기존 콘텐츠(TTS) 배틀 ID. `topicVersionId`와 둘 중 **하나만** 보낸다 |
| `topicVersionId` | △ | 검증된 커스텀 주제 버전 ID (4장). 클래스 전용 커스텀 배틀을 새로 만든다 |
| `deadline` | X | 참여 마감일. null이면 이용권 종료일까지. 지금 이후이고 이용권 종료일 이전이어야 한다 |
| `commentRequired` | O | 의견 작성 필수 여부. 참여 완료 판정에 쓴다 |
| `ownerParticipates` | O | 운영자도 참여자로 들어갈지. true면 운영자가 1석을 차지하고 투표·의견을 쓸 수 있다 |
| `ownerDisplayName` | X | 운영자가 참여할 때 클래스 안에서 쓸 이름 (1~20자). 비우면 닉네임을 쓴다 |
| `requestKey` | X (권장) | 중복 생성 방지 키 (1~64자, 영문·숫자·`-`·`_`). 생성 화면을 열 때 UUID를 하나 만들고, 재전송할 때도 같은 값을 보낸다 |

- 정원은 이용권 정원을 그대로 쓴다.
- 초대코드 6자는 서버가 만든다.
- 커스텀 배틀의 썸네일은 환경변수 `CLASS_CUSTOM_BATTLE_THUMBNAIL_URL`의 고정 이미지 1종이다.
- 응답: `ClassDetailResponse` (2.3 참고)
- **중복 요청 방지**: 버튼 연타·네트워크 재전송 대비. 같은 운영자의 생성 요청은 이용권 행 잠금으로 하나씩 처리한다.
  - `requestKey`를 보내면: 같은 키·같은 내용이면 새로 만들지 않고 그 클래스를 200으로 돌려준다. 같은 키·다른 내용이면 409 `CLASS_409_REQUEST_KEY`다. 키가 다르면 내용이 같아도 새로 만든다.
  - `requestKey`가 없으면: 10초 안에 같은 이름·같은 배틀(또는 주제 버전)로 다시 보낸 요청을 중복으로 보고 직전 클래스를 200으로 돌려준다.
  - 이름·운영자 표시 이름의 앞뒤 공백은 같은 내용으로 본다.

| 상황 | 응답 |
|---|---|
| 사용 중인 이용권 없음 | 403 `CLASS_403_TICKET` |
| `battleId`·`topicVersionId`를 둘 다 보내거나 둘 다 비움 | 400 `CLASS_400_BATTLE_SOURCE` |
| 없는 배틀 | 404 `BATTLE_404` |
| 공개되지 않았거나 삭제된 배틀 | 400 `CLASS_400_BATTLE` |
| 없는 주제 버전이거나 본인 초안의 버전이 아님 | 404 `CLASS_404_TOPIC_VERSION` |
| 같은 초안으로 이미 클래스를 만듦 (초안 1개당 클래스 1개) | 409 `CLASS_409_TOPIC_DRAFT_USED` |
| 마감일이 지금 이전이거나 이용권 종료일 이후 | 400 `CLASS_400_DEADLINE` |
| 같은 `requestKey`로 다른 내용을 보냄 | 409 `CLASS_409_REQUEST_KEY` |
| `requestKey`로 만든 클래스가 이미 삭제됨 | 409 `CLASS_409_DELETED` |
| `requestKey` 형식 오류 | 400 `COMMON_400` |
| 초대코드 생성 실패 (드묾) | 500 `CLASS_500_CODE` |

### 2.2 내 클래스 목록
- `GET /api/v1/classes`
- 내가 운영하는 클래스와 참여 중인 클래스를 최신 생성순으로 돌려준다. 삭제된 클래스는 빠진다.

```json
{
  "statusCode": 200,
  "data": [
    {
      "classId": 5,
      "name": "3학년 2반 철학 토론",
      "myRole": "OWNER",
      "battleId": 12,
      "battleTitle": "AI가 쓴 글도 창작일까?",
      "battleThumbnailUrl": "https://...",
      "battleType": "TTS",
      "memberCount": 12,
      "capacity": 30,
      "closeAt": "2026-12-31T23:59:59",
      "closed": false,
      "createdAt": "2026-10-21T10:00:00"
    }
  ]
}
```

- `memberCount`: 좌석을 차지한 인원이다. 운영자가 참여하면 포함되고, 나갔지만 활동 이력이 있어 좌석을 유지한 멤버도 포함된다.

### 2.3 클래스 상세
- `GET /api/v1/classes/{classId}`
- 운영자 또는 참여 중인 멤버만 볼 수 있다. 아니면 403 `CLASS_403_MEMBER`다.

```json
{
  "statusCode": 200,
  "data": {
    "classId": 5,
    "name": "3학년 2반 철학 토론",
    "inviteCode": "A7K2MP",
    "myRole": "OWNER",
    "ownerId": 1,
    "battle": { "battleId": 12, "title": "...", "summary": "...", "thumbnailUrl": "https://..." },
    "battleType": "TTS",
    "topicVersionId": null,
    "memberCount": 12,
    "capacity": 30,
    "deadline": "2026-12-31T23:59:59",
    "ticketExpiresAt": "2027-04-01T10:00:00",
    "closeAt": "2026-12-31T23:59:59",
    "closed": false,
    "commentRequired": true,
    "ownerParticipates": true,
    "createdAt": "2026-10-21T10:00:00"
  }
}
```

- `inviteCode`: 운영자·멤버 모두에게 내려간다. 멤버도 공유하기 버튼으로 코드를 공유할 수 있다.
- `topicVersionId`: 커스텀 배틀의 주제 버전 ID다. TTS 클래스는 null이다.
- `deadline`: 운영자가 정한 마감일이다. null이면 마감일이 없다.

### 2.4 클래스 수정 (운영자)
- `PATCH /api/v1/classes/{classId}`
- 이름 변경과 마감일 연장만 할 수 있다. null 필드는 변경하지 않는다.

```json
{ "name": "3학년 2반 철학 토론", "deadline": "2027-01-31T23:59:59" }
```

| 상황 | 응답 |
|---|---|
| 운영자가 아님 | 403 `CLASS_403_OWNER` |
| 이미 마감됨 | 409 `CLASS_409_CLOSED` |
| 이름이 공백뿐 | 400 `COMMON_400` |
| 마감일이 기존 마감일보다 앞임 (연장만 가능) | 400 `CLASS_400_DEADLINE_EXT` |
| 마감일이 이용권 종료일 이후 | 400 `CLASS_400_DEADLINE` |

### 2.5 클래스 삭제 (운영자)
- `DELETE /api/v1/classes/{classId}`
- 소프트 삭제다. 이후 목록·상세에서 보이지 않는다.
- 삭제된 클래스의 초대코드로 참여하면 409 `CLASS_409_DELETED`다.
- 운영자가 아니면 403 `CLASS_403_OWNER`다.

---

## 3. Picke 배틀 추천

### 3.1 조건에 맞는 배틀 추천
- `GET /api/v1/class-battle-recommendations`
- 쿼리 파라미터:

| 이름 | 필수 | 설명 |
|---|---|---|
| `difficulty` | O | 대상 수준 |
| `categories` | O | 카테고리 1~3개. `categories=철학,사회` 또는 `categories=철학&categories=사회` |
| `keyword` | X | 찾고 싶은 주제. 50자 이하이고 쉼표·공백으로 여러 단어를 넣을 수 있다 (앞 5단어만 사용) |

- **후보:** 공개(PUBLISHED) 상태이고 삭제되지 않았으며, 어드민이 클래스 추천을 켠 배틀이다 (9.1). 그중 수준이 같고 카테고리가 하나라도 겹치는 배틀만 남긴다.
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

## 4. 커스텀 배틀 주제

AI가 주제 후보를 제안하면 운영자가 하나를 고르거나 직접 고친다. 고른 결과는 검증된 "주제 버전"으로 저장되고, 클래스 생성(2.1)에는 이 `topicVersionId`만 쓸 수 있다.

### 4.1 주제 초안 생성
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

- 응답: `ClassTopicDraftResponse` (4.6 참고). 처음에는 `suggestStatus=NONE`이고 `batchNo=0`이다.

| 상황 | 응답 |
|---|---|
| 사용 중인 이용권 없음 | 403 `CLASS_403_TICKET` |
| 주제 길이·카테고리 개수 위반 | 400 `CLASS_400_TOPIC_CONDITION` |

### 4.2 주제 초안 조회
- `GET /api/v1/class-topic-drafts/{draftId}`
- 저장된 후보, 상태, 남은 횟수를 돌려준다. AI는 호출하지 않는다.
- 제안 중에 화면을 나갔다가 돌아오면 이 API로 결과를 이어서 본다.
- 본인 초안이 아니거나 없는 초안이면 404 `CLASS_404_TOPIC_DRAFT`다.

### 4.3 AI 주제 제안 (최초·재추천)
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
| 같은 이용권으로 분당 2회 초과 | 429 `CLASS_429_TOPIC` |
| 조건 위반 | 400 `CLASS_400_TOPIC_CONDITION` |
| 초안 없음 | 404 `CLASS_404_TOPIC_DRAFT` |

### 4.4 후보 선택
- `POST /api/v1/class-topic-drafts/{draftId}/selection`
- 고른 후보를 검증된 주제 버전으로 저장한다. AI는 호출하지 않는다.

```json
{ "batchNo": 1, "candidateId": 3 }
```

- `batchNo`에는 화면에 보인 배치 번호를 보낸다.
- 같은 후보를 다시 고르면 기존 버전을 그대로 돌려준다.
- 응답: `ClassTopicVersionResponse` (4.6 참고)

| 상황 | 응답 |
|---|---|
| `batchNo`가 현재 배치와 다름 (그 사이 재추천됨) | 409 `CLASS_409_TOPIC_CANDIDATE` |
| 없는 후보 ID | 404 `CLASS_404_TOPIC_CANDIDATE` |

### 4.5 직접 수정 검증
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

### 4.6 응답 객체

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

## 5. 참여·멤버

### 5.1 참여 코드 확인
- `GET /api/v1/classes/join/preview?inviteCode=A7K2MP`
- 참여 처리 없이 코드만 확인한다 (코드 입력 단계).
- 코드에 들어간 공백·하이픈과 대소문자는 서버가 정리한다.

```json
{
  "statusCode": 200,
  "data": {
    "inviteCode": "A7K2MP",
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

### 5.2 초대코드로 참여
- `POST /api/v1/classes/join`

```json
{ "inviteCode": "A7K2MP", "displayName": "김학생" }
```

- `displayName`: 클래스 안에서 쓸 이름이다. 앞뒤 공백 제거 후 1~20자이고, 같은 클래스 안에서 겹칠 수 없다.
- 응답: `ClassDetailResponse` (2.3)
- **이미 참여 중:** 거절하지 않고 200과 기존 상세를 돌려준다. 다시 요청해도 안전하게 하려는 동작이다. 그래서 "이미 참여 중" 팝업은 코드 확인(5.1)에서 띄운다.
- **나갔던 멤버:** 같은 코드로 다시 참여할 수 있다. 활동 이력으로 좌석을 유지하고 있었으면 좌석을 새로 쓰지 않는다.
- **같은 검사를 한 번 더 함:** 코드 확인 뒤 이름을 입력하는 사이에 정원이 차거나 마감될 수 있다. 그래서 5.1의 검사를 여기서도 다시 한다.

| 상황 | 응답 |
|---|---|
| 5.1의 에러 (이미 참여 중 제외) | 5.1과 같음 |
| 이름이 비었거나 20자 초과 | 400 `COMMON_400` |
| 이름 중복 | 409 `CLASS_409_NAME` |

### 5.3 클래스 명단
- `GET /api/v1/classes/{classId}/members`
- 운영자 행이 먼저 오고, 이후 참여 순이다.
- 표시 이름·역할·아바타만 내려가고 계정 식별자는 내려가지 않는다.

```json
{
  "statusCode": 200,
  "data": [
    { "memberId": 10, "displayName": "김선생", "role": "OWNER", "characterUrl": "https://...", "me": true, "joinedAt": "2026-10-21T10:00:00" },
    { "memberId": 11, "displayName": "김학생", "role": "MEMBER", "characterUrl": "https://...", "me": false, "joinedAt": "2026-10-21T11:00:00" }
  ]
}
```

- `memberId`: 내보내기(5.6)에 쓴다.
- `me`: 조회한 사람 본인이면 true다 (이름 수정 버튼 노출용).
- 권한이 없으면 403 `CLASS_403_MEMBER`다.

### 5.4 내 표시 이름 변경
- `PATCH /api/v1/classes/{classId}/members/me`

```json
{ "displayName": "김학생2" }
```

- 응답: `ClassMemberResponse` (5.3의 항목 하나)

| 상황 | 응답 |
|---|---|
| 참여 중인 멤버가 아님 | 403 `CLASS_403_MEMBER` |
| 이름이 비었거나 20자 초과 | 400 `COMMON_400` |
| 이름 중복 | 409 `CLASS_409_NAME` |

### 5.5 클래스 나가기
- `DELETE /api/v1/classes/{classId}/members/me`
- 나간 뒤에도 같은 코드로 다시 참여할 수 있다.
- 투표·의견 등 활동 이력이 있으면 좌석은 반환되지 않는다.

계정 탈퇴 시에도 같은 좌석 규칙을 적용한다. 탈퇴자가 운영자인 ACTIVE 클래스는 소프트 삭제하고, 참여 중인 멤버십은 `LEFT`로 바꾼다. 탈퇴자가 쓴 의견·댓글은 숨김 처리하며 이력 참조를 위해 DB 행은 보존한다.

| 상황 | 응답 |
|---|---|
| 운영자 (나갈 수 없고 클래스를 삭제해야 함) | 403 `CLASS_403_OWNER_LEAVE` |
| 참여 중인 멤버가 아님 | 403 `CLASS_403_MEMBER` |

### 5.6 멤버 내보내기 (운영자)
- `DELETE /api/v1/classes/{classId}/members/{memberId}`
- 내보내진 멤버는 같은 클래스에 다시 참여할 수 없다.
- 좌석 반환 규칙은 나가기(5.5)와 같다.

| 상황 | 응답 |
|---|---|
| 운영자가 아님 | 403 `CLASS_403_OWNER` |
| 없는 멤버이거나 이미 나간 멤버 | 404 `CLASS_404_MEMBER` |
| 운영자 본인 행 | 403 `CLASS_403_OWNER_LEAVE` |

---

## 6. 클래스 배틀 진행

- 경로에 배틀 ID는 없다. 클래스에 붙은 배틀을 쓴다.
- 투표·참여자 수·의견은 이 클래스 안에서만 집계하고, 일반 배틀과 섞이지 않는다.
- 크레딧 차감, 성향 점수, 일반 참여자 수에는 영향이 없다.
- 권한은 0.3의 읽기·쓰기 규칙을 따른다.

### 6.1 클래스 배틀 상세
- `GET /api/v1/classes/{classId}/battle`
- 마감 후에도 조회할 수 있다.

```json
{
  "statusCode": 200,
  "data": {
    "classId": 5,
    "myRole": "MEMBER",
    "closeAt": "2026-12-31T23:59:59",
    "closed": false,
    "commentRequired": true,
    "canParticipate": true,
    "participationCompleted": false,
    "midVoteEnabled": false,
    "battle": { }
  }
}
```

- `canParticipate`: 지금 투표·의견을 쓸 수 있는지다. 참여 중인 멤버이고 마감 전일 때 true다. 참여하지 않는 운영자는 false다. false면 투표·의견 버튼을 막는다.
- `participationCompleted`: 참여 완료 여부다. 사후투표를 마쳤고, 의견이 필수면 공개된 내 의견이 1건 이상일 때 true다.
- `midVoteEnabled`: 중간투표가 있는 기존 콘텐츠(TTS)인지다. 배틀의 중간투표 설정이 켜져 있고 시나리오에 선택 지점(선택지가 있는 노드)이 있을 때 true다. true면 콘텐츠의 선택 화면에서 고른 값을 6.3.1로 보낸다. 커스텀 배틀은 항상 false다.
- `battle`: 일반 배틀 상세(`BattleUserDetailResponse`)와 같은 구조다. 단 `userVoteStatus`, `currentStep`, `participantsCount`는 이 클래스 기준이다. 구조는 `battle-api.md`를 참고한다.

### 6.2 사전투표
- `POST /api/v1/classes/{classId}/battle/votes/pre`

```json
{ "optionId": 31 }
```

```json
{ "statusCode": 200, "data": { "voteId": 100, "status": "PRE_VOTE" } }
```

- 배틀 시작 전(`NONE`/`PRE_VOTE`)에는 다시 보내서 바꿀 수 있다.

| 상황 | 응답 |
|---|---|
| TTS 청취 이후 또는 AI 토론 시작 이후 변경 | 409 `CLASS_409_PRE_VOTE_LOCKED` |
| 이 배틀의 선택지가 아님 | 404 `BATTLE_OPT_404` |

### 6.3 TTS 청취 완료 (TTS 클래스만)
- `POST /api/v1/classes/{classId}/battle/votes/tts-complete`
- 사전투표 후 오디오 재생이 끝났을 때 호출한다.
- 단계가 `POST_VOTE`로 넘어가고, 이후 사전투표가 잠긴다. 이 호출이 있어야 사후투표를 할 수 있다.
- 이미 완료(`COMPLETED`)된 진행은 되돌리지 않는다.

| 상황 | 응답 |
|---|---|
| 커스텀 배틀 클래스 | 409 `CLASS_409_TTS` |
| 사전투표 전 | 404 `VOTE_404` |

### 6.3.1 중간투표 (중간투표가 있는 TTS 콘텐츠만)
- `POST /api/v1/classes/{classId}/battle/votes/mid`
- 콘텐츠 재생 중 "선택의 시간"에서 고른 값을 선택 지점마다 기록한다. 클래스용 별도 화면은 없고, 기존 콘텐츠 선택 결과를 수집하는 용도다.
- 선택 지점(checkpoint)은 선택지가 있는 시나리오 노드다. 값은 시나리오 조회(`GET /battles/{battleId}/scenario`)의 노드 ID와 `interactiveOptions[].optionId`를 쓴다.

```json
{ "checkpointId": 801, "optionId": 9001 }
```

```json
{ "statusCode": 200, "data": { "voteId": 100, "status": "TTS_LISTENING" } }
```

- 지점마다 1건만 저장한다. 같은 지점을 다시 보내면 덮어쓴다. 사후투표 전까지 바꿀 수 있다(콘텐츠를 다시 재생한 경우).
- 처음 보내면 단계가 `TTS_LISTENING`으로 바뀌고, 이후 사전투표가 잠긴다. 이미 `POST_VOTE`면 단계는 그대로 둔다.
- 필수 중간투표: 시작 노드부터 내가 고른 선택지를 따라가며 만나는 선택 지점에 모두 답해야 사후투표(6.4)를 할 수 있다.
- 선택 당시 선택지 문구를 함께 저장한다. 시나리오를 수정해도 결과·리포트에는 고른 문구가 남는다.

| 상황 | 응답 |
|---|---|
| 중간투표가 없는 배틀(설정 꺼짐·선택 지점 없음)이거나 커스텀 배틀 클래스 | 409 `CLASS_409_MID_VOTE` |
| 없는 선택 지점이거나 그 지점의 선택지가 아님 | 404 `CLASS_404_MID_VOTE_OPTION` |
| 사전투표 전 | 404 `VOTE_404` |
| 사후투표 완료 후 | 409 `CLASS_409_MID_VOTE_LOCKED` |

### 6.4 사후투표
- `POST /api/v1/classes/{classId}/battle/votes/post`
- 요청은 6.2와 같고, 응답의 `status`는 `COMPLETED`다.
- 제출한 뒤에는 바꿀 수 없다.

| 상황 | 응답 |
|---|---|
| 사전투표 기록 없음 | 404 `VOTE_404` |
| 진행 단계가 `NONE` | 409 `VOTE_409_PRE` |
| TTS 클래스인데 청취 완료(6.3) 전 | 409 `CLASS_409_TTS_INCOMPLETE` |
| 중간투표 콘텐츠인데 고른 경로의 선택 지점에 답하지 않음 | 409 `CLASS_409_MID_VOTE_REQUIRED` |
| 이미 제출 | 409 `VOTE_409_SUB` |
| 커스텀 배틀인데 AI 토론을 마치지 않음 | 409 `CLASS_409_DEBATE_INCOMPLETE` |
| 이 배틀의 선택지가 아님 | 404 `BATTLE_OPT_404` |

### 6.5 내 투표 내역
- `GET /api/v1/classes/{classId}/battle/votes/me`

```json
{
  "statusCode": 200,
  "data": {
    "battleTitle": "AI가 쓴 글도 창작일까?",
    "preVote": { "optionId": 31, "label": "A", "title": "창작이다" },
    "midVotes": [ { "checkpointId": 801, "optionId": 9002, "label": "무죄" } ],
    "postVote": { "optionId": 32, "label": "B", "title": "창작이 아니다" },
    "status": "COMPLETED",
    "opinionChanged": true
  }
}
```

- 투표 전이면 404 `VOTE_404`다.
- `midVotes`는 선택 지점별 중간투표(6.3.1)다. 지점 ID 순이고, 중간투표가 없는 배틀이거나 아직 고르지 않았으면 빈 배열이다.
- `opinionChanged`는 사전·사후만 비교한다.

### 6.6 투표 통계
- `GET /api/v1/classes/{classId}/battle/vote-stats`
- 이 클래스에 지금 참여 중인(ACTIVE) 멤버들의 사후투표만 집계한다. 나가거나 내보내진 멤버의 투표는 빠지고, 다시 참여하면 다시 포함된다.

```json
{
  "statusCode": 200,
  "data": {
    "options": [
      { "optionId": 31, "title": "창작이다", "imageUrl": "https://...", "voteCount": 7, "ratio": 58.3 },
      { "optionId": 32, "title": "창작이 아니다", "imageUrl": "https://...", "voteCount": 5, "ratio": 41.7 }
    ],
    "totalCount": 12,
    "updatedAt": "2026-10-21T12:00:00"
  }
}
```

### 6.7 의견 등록
- `POST /api/v1/classes/{classId}/battle/perspectives`

```json
{ "content": "저는 ... 라고 생각해요." }
```

```json
{ "statusCode": 200, "data": { "perspectiveId": 50, "status": "PUBLISHED", "createdAt": "2026-10-21T12:00:00" } }
```

- 사후투표한 선택지에 귀속된다.
- 의견 작성이 필수인 클래스(`commentRequired=true`)의 의견은 삭제할 수 없고 수정만 할 수 있다. 리포트 자료로 쓰이기 때문이다. 삭제하면 409 `CLASS_409_PERSPECTIVE_REQUIRED`다.
- 바로 `PUBLISHED`로 저장된다.
- 사후투표 전이면 404 `BATTLE_OPT_404`다.

### 6.8 의견 목록
- `GET /api/v1/classes/{classId}/battle/perspectives`
- 이 클래스 안에서 작성된 의견만 커서 기반으로 조회한다.

| 쿼리 | 필수 | 설명 |
|---|---|---|
| `cursor` | X | 이전 응답의 `nextCursor` |
| `size` | X | 기본 20 |
| `optionId` | X | 선택지 필터 |
| `sort` | X | `latest`(기본) / `popular` |

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "perspectiveId": 50,
        "user": { "userTag": "...", "nickname": "...", "characterType": "...", "characterImageUrl": "https://..." },
        "option": { "optionId": 31, "label": "A", "title": "창작이다", "stance": "..." },
        "content": "...",
        "likeCount": 3,
        "commentCount": 1,
        "isLiked": false,
        "isMyPerspective": true,
        "createdAt": "2026-10-21T12:00:00"
      }
    ],
    "nextCursor": "2026-10-21T12:00:00",
    "hasNext": false
  }
}
```

- `user.nickname`에는 전체 서비스 닉네임 대신 이 클래스의 표시 이름(5.2·5.4)이 내려간다.
- `user.userTag`는 클래스에서는 null이다. 계정 식별자를 클래스 안에 노출하지 않는다.
- 나가거나 내보내진 멤버의 의견은 목록에서 빠진다. 다시 참여하면 다시 보인다.

### 6.9 내 의견
- `GET /api/v1/classes/{classId}/battle/perspectives/me`
- 이 클래스 안에서 내가 쓴 의견 목록이다.
- 항목 구조는 6.8과 같고, `isMyPerspective` 대신 `status`가 들어간다.

### 6.10 의견의 댓글·수정·삭제·좋아요
클래스 의견도 일반 관점 API를 그대로 쓴다. 구조는 `comments-api.md`, `perspectives-api.md`, `likes-api.md`를 참고한다.

클래스 의견·댓글이면 의견 상세, 댓글 목록, 댓글 등록 응답의 `user.nickname`도 클래스 표시 이름이고 `user.userTag`는 null이다.
나가거나 내보내진 멤버의 댓글은 댓글 목록에서 빠진다. 그 멤버의 의견은 상세 조회·댓글 조회·댓글 등록 모두 404 `PERSPECTIVE_404`다.

| 기능 | API | 클래스 권한 검사 |
|---|---|---|
| 의견 상세·수정·삭제 | `GET` / `PATCH` / `DELETE /api/v1/perspectives/{perspectiveId}` | 조회는 읽기 규칙, 수정·삭제는 쓰기 규칙 (0.3) |
| 댓글 목록 | `GET /api/v1/perspectives/{perspectiveId}/comments` (`/labeled` 포함) | 읽기 규칙 |
| 댓글 작성·수정·삭제 | `POST` / `PATCH` / `DELETE /api/v1/perspectives/{perspectiveId}/comments/...` | 쓰기 규칙 |
| 의견 좋아요 수 | `GET /api/v1/perspectives/{perspectiveId}/likes` | 읽기 규칙 |
| 의견 좋아요 등록·취소 | `POST` / `DELETE /api/v1/perspectives/{perspectiveId}/likes` | 쓰기 규칙 |
| 댓글 좋아요 등록·취소 | `POST` / `DELETE /api/v1/comments/{commentId}/likes` | 쓰기 규칙 |
| 의견·댓글 신고 | `POST /api/v1/perspectives/{perspectiveId}/reports`, `POST /api/v1/perspectives/{perspectiveId}/comments/{commentId}/reports` | 읽기 규칙 (마감 후에도 신고 가능). 댓글이 경로의 관점에 속하지 않으면 404 `COMMENT_NOT_FOUND` |

---

## 7. AI 철학자 토론 (커스텀 배틀)

커스텀 배틀 클래스에서 사전투표를 한 뒤, 반대 입장의 AI 철학자와 3라운드 텍스트 토론을 한다.

- **권한**
  - 조회: 클래스를 볼 수 있으면 된다. 마감 후에도 읽을 수 있다.
  - 시작·반박·재시도: 0.3의 쓰기 규칙을 따른다.
- **라운드 진행:** 1라운드는 AI 주장으로 시작하고, 이어서 참가자가 반박한다. 3라운드 반박을 내면 토론이 끝나고 `COMPLETED`가 된다.
- **AI 주장 길이:** 200~500자
- **반박 길이:** 앞뒤 공백 제거 후 50~500자. 저장한 뒤에는 수정할 수 없다.

### 7.1 토론 조회 (재접속)
- `GET /api/v1/classes/{classId}/battle/debate`
- 저장된 대화와 상태를 돌려준다. AI는 호출하지 않는다.
- 시작 전이면 404 `CLASS_404_DEBATE`다.

### 7.2 토론 시작
- `POST /api/v1/classes/{classId}/battle/debate`
- **세션 구성:** 사전투표한 선택지의 반대편, 그리고 그 반대편 선택지의 철학자로 세션을 만든다. 이어서 1라운드 AI 주장을 생성한다.
- **사전투표 잠금:** 시작한 뒤에는 사전투표를 바꿀 수 없다.
- **중복 호출:** 이미 시작했으면 저장된 대화를 그대로 돌려준다.
- **AI 생성 실패:** 200과 함께 `status=FAILED`로 응답한다. 이때는 재시도 API를 쓴다.

| 상황 | 응답 |
|---|---|
| 커스텀 클래스가 아님 | 409 `CLASS_409_DEBATE_TYPE` |
| 사전투표 전 | 409 `CLASS_409_DEBATE_PRE_VOTE` |

### 7.3 반박 제출
- `POST /api/v1/classes/{classId}/battle/debate/replies`

```json
{
  "roundIndex": 1,
  "content": "속으로 슬퍼해도 밖으로 표현하지 않을 수 있어요. ...",
  "requestKey": "4f0b5c3e-6a8d-4f6b-9a51-2f3d1c7e9b10"
}
```

- `roundIndex`에는 화면에 보인 현재 라운드를 보낸다.
- 저장 전에 차단 대상인지 검사한다. 차단되면 저장하지 않고 라운드도 쓰지 않으므로, 고쳐서 같은 라운드로 다시 보내면 된다.
  - 개인정보는 서버 규칙으로 검사한다.
  - 위협·위협성 혐오·미성년자 성적 표현·자해 방법·폭력적 불법행위는 OpenAI Moderation으로 검사한다. 폭력·범죄를 다루는 정상 논증은 막지 않는다.
  - 검사 API에 장애가 나면 막지 않고 통과시킨다.
- `requestKey`는 중복 전송을 막는 키다. 64자 이하이고 UUID를 권장한다.
  - 같은 반박을 다시 보낼 때는 같은 키를 쓴다. 같은 키로 다시 보내면 저장된 결과를 그대로 돌려준다.
- 반박을 저장한 뒤 다음 라운드 AI 주장을 생성한다.
  - 3라운드 반박이면 AI 주장 없이 `COMPLETED`가 된다.

| 상황 | 응답 |
|---|---|
| 반박 길이 위반 | 400 `CLASS_400_DEBATE_REPLY` |
| 개인정보(전화번호·이메일·주민등록번호·카드번호) 포함 | 400 `CLASS_400_DEBATE_PERSONAL_INFO` |
| 위협 등 차단 대상 표현 | 400 `CLASS_400_DEBATE_BLOCKED` |
| 다른 키로 같은 라운드를 다시 제출 | 409 `CLASS_409_DEBATE_ROUND` |
| 반박을 받을 수 없는 상태 (생성 중·실패·완료) | 409 `CLASS_409_DEBATE_STATE` |
| 시작 전 | 404 `CLASS_404_DEBATE` |

### 7.4 AI 주장 재시도
- `POST /api/v1/classes/{classId}/battle/debate/retry`
- 같은 라운드의 AI 주장을 다시 생성한다.
- `status=FAILED`이거나 생성이 3분 넘게 멈췄을 때만 할 수 있다. 그 외에는 409 `CLASS_409_DEBATE_RETRY`다.

### 7.5 응답 `ClassDebateResponse`

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
    "replyMaxLength": 500
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

## 8. 결과·AI 리포트

- 정량 통계는 언제든 볼 수 있다. 마감 전에는 현재 데이터, 마감 후에는 마감 시점 스냅샷이다.
- AI 리포트는 마감 후 스케줄러가 한 번 만든다. API는 저장된 결과를 읽기만 한다.
- 이용권 종료 + 30일이 지나면 409 `CLASS_409_RESULT_EXPIRED`다.

리포트 상태 `ClassReportViewState`:

| 값 | 의미 | 화면 문구 |
|---|---|---|
| `BEFORE_CLOSE` | 마감 전 | "{closeAt} 이후 AI 피드백이 제공돼요" |
| `NOT_ELIGIBLE` | 참여 미완료라 개인 리포트 없음 | |
| `PENDING` / `GENERATING` | 생성 대기 / 생성 중 | "생각을 정리하고 있어요" |
| `SUCCEEDED` | 생성 완료 | |
| `LIMITED` | 자료가 부족해 템플릿으로 대신함 | "판단의 이유를 살펴볼 글이 아직 충분하지 않아요" |
| `FAILED` | 생성 실패 | "AI 요약을 준비하지 못했어요" |

### 8.1 클래스 결과 (정량 통계)
- `GET /api/v1/classes/{classId}/result`

```json
{
  "statusCode": 200,
  "data": {
    "classId": 5,
    "myRole": "OWNER",
    "closeAt": "2026-12-31T23:59:59",
    "closed": true,
    "viewableUntil": "2027-05-01T10:00:00",
    "statsBasis": "SNAPSHOT",
    "cutoffAt": "2026-12-31T23:59:59",
    "stats": {
      "activeMemberCount": 12,
      "completedCount": 10,
      "participationRate": 83.3,
      "commentWriterCount": 9,
      "commentRate": 75.0,
      "postVoteCount": 11,
      "midVoteEnabled": true,
      "midVoteCount": 10,
      "options": [
        { "optionId": 31, "title": "창작이다", "stance": "...", "preVoteCount": 8, "postVoteCount": 6, "postVoteRatio": 54.5 }
      ],
      "midVoteCheckpoints": [
        {
          "checkpointId": 801,
          "voteCount": 10,
          "options": [
            { "optionId": 9001, "label": "유죄", "voteCount": 4, "ratio": 40.0 },
            { "optionId": 9002, "label": "무죄", "voteCount": 6, "ratio": 60.0 }
          ]
        }
      ]
    },
    "myReportState": "SUCCEEDED",
    "insightState": "SUCCEEDED"
  }
}
```

- `statsBasis`: `LIVE`(현재 데이터) / `SNAPSHOT`(마감 스냅샷). `LIVE`이면 `cutoffAt`은 null이다.
- 비율은 소수 첫째 자리 %이고, 분모가 0이면 0이다.
- `midVoteCount`는 한 지점이라도 답한 활성 참여자 수다. `midVoteCheckpoints`는 선택 지점별 분포이고, `ratio`는 그 지점 응답자 중 비율이다.
- `midVoteEnabled`가 false면 `midVoteCount`는 0, `midVoteCheckpoints`는 빈 배열이다. 화면에서 중간투표 칸을 숨긴다. 중간투표 도입 전에 만든 스냅샷도 false로 내려간다.
- `myReportState`: 참여 멤버만 있다. 참여하지 않는 운영자는 null이다.
- `insightState`: 운영자에게만 있다. 그 외에는 null이다.
- 이 두 값으로 리포트 탭을 보여줄지 정한다.

### 8.2 내 AI 피드백
- `GET /api/v1/classes/{classId}/result/my-report`
- 참여 완료자에게 마감 후 제공한다.
- 글 없이 투표만 했으면 `LIMITED`이고, 서버 템플릿으로 채워진다.

```json
{
  "statusCode": 200,
  "data": {
    "classId": 5,
    "state": "SUCCEEDED",
    "closeAt": "2026-12-31T23:59:59",
    "preOption": { "optionId": 31, "title": "창작이다", "stance": "..." },
    "midVotes": [],
    "postOption": { "optionId": 32, "title": "창작이 아니다", "stance": "..." },
    "perspectiveCount": 1,
    "commentCount": 2,
    "feedback": {
      "headline": "...",
      "judgmentBasis": "...",
      "strength": { "title": "...", "body": "..." },
      "nextFocus": { "title": "...", "body": "..." },
      "nextQuestion": "...",
      "evidence": [
        { "field": "judgment_basis", "sourceType": "PERSPECTIVE", "sourceId": 50, "quote": "...", "content": "...", "createdAt": "2026-10-21T12:00:00" }
      ],
      "analysisStatus": "SUFFICIENT",
      "coverage": "full",
      "excludedCount": 0
    },
    "generatedAt": "2027-01-01T00:01:00"
  }
}
```

- `feedback`은 `state`가 `SUCCEEDED`나 `LIMITED`일 때만 있다. 그 외에는 null이다.
- `strength`, `nextFocus`는 기록으로 확인되지 않으면 null이다.
- `midVotes`는 중간투표가 있는 콘텐츠에서 선택 지점마다 고른 선택지다(6.5와 같은 형태). 없으면 빈 배열이다. 중간 선택은 AI 피드백 입력에도 들어간다.
- 글 없이 투표만 한 `LIMITED` 템플릿은 중간 선택이 있으면 "… 콘텐츠 중간에는 '무죄' 쪽을 골랐어요."를 덧붙인다.
- `evidence[].content`는 현재 기준 원문이다. 삭제·숨김된 글은 목록에서 빠진다.
- `coverage=partial`은 분량 제한으로 일부 글을 빼고 분석했다는 뜻이다. 뺀 글 수는 `excludedCount`에 있다.
- 참여하지 않는 운영자는 403 `CLASS_403_MEMBER`다.

### 8.2.1 내 활동 (리포트 화면)
- `GET /api/v1/classes/{classId}/result/my-activity?cursor={createdAt}&size=20`
- 참여 중인 멤버만 조회할 수 있다. 기본 페이지 크기는 20, 최대 100이다.
- `receivedLikeCount`는 공개된 내 의견·댓글에 받은 좋아요 수 합계다. `comments`는 공개 상태인 내 댓글 목록이며, 최신순 커서 페이지네이션을 사용한다.
- 잘못된 `cursor`는 400 `COMMON_400`이다.

```json
{
  "statusCode": 200,
  "data": {
    "classId": 5,
    "receivedLikeCount": 12,
    "commentCount": 2,
    "comments": [
      { "commentId": 61, "perspectiveId": 50, "perspectiveTitle": "창작이다", "stance": "...", "content": "...", "likeCount": 2, "createdAt": "2026-10-21T12:00:00" }
    ],
    "nextCursor": null,
    "hasNext": false
  }
}
```

### 8.3 클래스 인사이트 (운영자)
- `GET /api/v1/classes/{classId}/result/insight`
- 공개 의견을 요약한 AI 인사이트와 근거·대표 의견이다.

```json
{
  "statusCode": 200,
  "data": {
    "classId": 5,
    "state": "SUCCEEDED",
    "retryable": false,
    "cutoffAt": "2026-12-31T23:59:59",
    "generatedAt": "2027-01-01T00:01:00",
    "insightText": "...",
    "coverage": "full",
    "excludedCount": 0,
    "evidence": [
      { "sourceType": "PERSPECTIVE", "id": 50, "optionId": 31, "stance": "...", "content": "...", "createdAt": "2026-10-21T12:00:00" }
    ],
    "highlights": []
  }
}
```

- `insightText`: 200자 이하다.
- `evidence`: 인사이트가 근거로 삼은 공개 글 1~5건이다.
- `highlights`: 운영자에게 추천하는 대표 의견 최대 2건이다. 작성자 정보는 넣지 않는다.
- 의견 작성자가 3명 미만이면 `LIMITED`다.
- 생성에 실패하면 `FAILED`이고 `retryable`이 true다.
- 운영자가 아니면 403 `CLASS_403_OWNER`다.

### 8.4 클래스 인사이트 재시도 (운영자)
- `POST /api/v1/classes/{classId}/result/insight/retry`
- `FAILED` 상태일 때만 할 수 있다.
- 대기열에 다시 넣고 `PENDING`을 돌려준다. 1분 안에 다시 생성된다.

| 상황 | 응답 |
|---|---|
| 다시 시도할 수 없는 상태 | 409 `CLASS_409_REPORT_RETRY` |
| 운영자가 아님 | 403 `CLASS_403_OWNER` |

---

## 9. 어드민

### 9.1 배틀 클래스 설정 수정
- `PATCH /api/v1/admin/battles/{battleId}/class-settings`
- null 필드는 변경하지 않는다.

```json
{ "difficulty": "BEGINNER", "category": "철학", "classRecommendable": true, "midVoteEnabled": false }
```

| 필드 | 설명 |
|---|---|
| `difficulty` | 추천 필터용 대상 수준 |
| `category` | 추천 필터용 카테고리 1개 |
| `classRecommendable` | 클래스 추천(3.1)에 노출할지 |
| `midVoteEnabled` | 중간투표 콘텐츠 여부 |

- 응답: `AdminBattleDetailResponse`. 끝에 `difficulty`, `category`, `classRecommendable`, `midVoteEnabled`가 추가됐다. 어드민 배틀 상세 조회에도 같은 필드가 내려간다.
- 추천 API는 이 설정이 켜진 배틀만 돌려준다. 그래서 운영에서 배틀마다 설정해야 추천 결과가 나온다.
- 수준은 기획 확정 전까지 임시로 초급이다. 수준이 비어 있던 기존 배틀은 SQL로 초급을 채웠고, 어드민이 새로 만드는 배틀도 초급으로 저장된다. 어드민 배틀 목록에도 `difficulty`가 내려간다.

| 상황 | 응답 |
|---|---|
| 추천을 켜는데 수준·카테고리가 비어 있음 | 400 `CLASS_400_RECOMMEND_SETTINGS` |
| 사용자가 만든(커스텀) 배틀 | 400 `CLASS_400_BATTLE` |

### 9.2 펀딩 명단 CSV 가져오기
- `POST /api/v1/admin/funding/imports` (`multipart/form-data`, 파일 필드 `file`)
- 파일은 UTF-8 CSV다. 헤더는 다음 순서다: `platform, campaign_id, order_id, reward_tier, quantity, supporter_name, supporter_email, supporter_phone, payment_status, paid_at`
- `order_id` 기준으로 추가하거나 갱신한다. 같은 파일을 여러 번 올려도 안전하다.
- 행 단위 오류는 `errors`로 돌려주고, 나머지 행은 반영한다.
- 환불·취소(`REFUNDED`/`CANCELED`)된 주문은 결제 확정(`PAID_CONFIRMED`)으로 되돌리지 않는다.

```json
{
  "statusCode": 200,
  "data": {
    "fileHash": "sha256...",
    "totalRows": 120,
    "created": 100,
    "updated": 5,
    "unchanged": 13,
    "errorCount": 2,
    "issuableCount": 98,
    "heldCount": 4,
    "errors": [ { "line": 7, "orderId": "T-0007", "reason": "..." } ]
  }
}
```

- `platform`: `TUMBLBUG` / `WADIZ` / `MANUAL`
- `reward_tier`: `SEAT_5` / `SEAT_15` / `SEAT_30`
- `payment_status`: `RESERVED` / `PAYMENT_PENDING` / `PAID_CONFIRMED` / `PAYMENT_FAILED` / `CANCELED` / `REFUNDED`
- 헤더가 다르거나 파일을 읽을 수 없으면 400 `FUNDING_400_CSV`다.

### 9.3 이용권 코드 발급
- `POST /api/v1/admin/funding/tickets/issue`

```json
{ "fundingOrderIds": [1, 2, 3] }
```

- 본문을 비우거나 `fundingOrderIds`를 비우면 발급 가능한 전체 주문에 발급한다. 발급 가능한 주문은 결제 확정, 수량 1, 미발급인 주문이다.
- 이미 살아 있는 이용권이 있는 주문은 건너뛴다. 그래서 다시 실행해도 중복 발급되지 않는다.

```json
{
  "statusCode": 200,
  "data": {
    "issued": [
      {
        "ticketId": 3, "fundingOrderId": 1, "orderId": "T-0001",
        "supporterName": "홍길동", "supporterEmail": "...", "supporterPhone": "...",
        "capacity": 30, "registerBy": "2027-09-01T00:00:00",
        "code": "XXXXXX-XXXXX-XXXXX-XXXXX-XXXXX"
      }
    ],
    "skipped": [ { "fundingOrderId": 2, "orderId": "T-0002", "reason": "ALREADY_ISSUED" } ]
  }
}
```

- **`code` 원문은 이 응답에서만 볼 수 있다.** 서버는 해시만 저장한다. 그래서 전달 전까지 어드민 화면에서 보관해야 한다.
- `skipped[].reason`: `ALREADY_ISSUED` / `NOT_ISSUABLE` / `NOT_FOUND`

### 9.4 이용권 전달 기록
- `POST /api/v1/admin/funding/tickets/{ticketId}/deliveries`

```json
{ "channel": "EMAIL", "status": "SUCCESS", "failureReason": null }
```

- `channel`: `EMAIL` / `SMS` / `MANUAL`
- `status`: `SUCCESS` / `FAILED`. `SUCCESS`면 코드 상태가 `DELIVERED`로 바뀐다.
- `failureReason`: 실패 사유. 255자 이하다.
- 응답: `attemptId`, `ticketId`, `channel`, `status`, `failureReason`, `attemptedAt`, `codeStatus`, `deliveredAt`
- 이미 등록됐거나(CLAIMED) 무효화된(REVOKED) 이용권이면 409 `TICKET_409_STATE`다.

### 9.5 이용권 코드 재발급
- `POST /api/v1/admin/funding/tickets/{ticketId}/reissue`
- 코드 분실 등에 쓴다. 기존 코드를 무효화(REVOKED)하고, 같은 주문·정원·등록 기한으로 새 코드를 발급한다.
- 응답: 9.3의 `issued[]` 항목 하나. 새 `code`도 이 응답에서만 볼 수 있다.

| 상황 | 응답 |
|---|---|
| 이미 등록됨 | 409 `TICKET_409_CLAIMED` |
| 이미 무효화됨 | 409 `TICKET_409_REVOKED` |
| 등록 기한 경과 | 409 `TICKET_409_REG_EXP` |

### 9.6 펀딩 환불 처리
- `POST /api/v1/admin/funding/refunds`

```json
{ "fundingOrderIds": [1, 2] }
```

```json
{ "statusCode": 200, "data": { "refunded": 1, "revokedTickets": 1, "alreadyRefunded": [2] } }
```

- 주문을 `REFUNDED`로 바꾸고, 살아 있는 이용권을 무효화(REVOKED)한다.
- 이미 환불된 주문은 `alreadyRefunded`로 돌려준다.
- 없는 주문 ID가 섞여 있으면 404 `FUNDING_404_ORDER`다.

### 9.7 펀딩 주문·이용권 목록
- `GET /api/v1/admin/funding/orders`

| 쿼리 | 설명 |
|---|---|
| `paymentStatus` | 결제 상태 필터 |
| `ticketStatus` | `UNREGISTERED` / `ACTIVE` / `EXPIRED` / `REGISTER_EXPIRED` / `REVOKED` |
| `codeStatus` | `ISSUED` / `DELIVERED` / `CLAIMED` / `REVOKED` |
| `keyword` | order_id / 후원자명 / 이메일 부분 일치 |
| `page`, `size` | 기본 0, 20 |

- 응답: `items[]`, `page`, `size`, `totalElements`, `hasNext`
- `items[]`에는 주문 정보가 들어간다: `fundingOrderId`, `platform`, `campaignId`, `orderId`, `rewardTier`, `quantity`, 후원자 정보, `paymentStatus`, `paidAt`, `issuable`
- `items[].ticket`은 살아 있는 이용권 요약이고, 없으면 null이다. 필드는 `ticketId`, `capacity`, `codeStatus`, `status`, `deliveredAt`, `registerBy`, `activatedAt`, `expiresAt`, `registeredUserId`, `registeredNickname`다.
- 코드 원문과 해시는 내려가지 않는다.

### 9.8 펀딩·이용권 현황 요약
- `GET /api/v1/admin/funding/summary`
- 응답 필드:
  - 주문: `totalOrders`, `paidConfirmed`, `issuable`, `held`, `refunded`, `canceled`
  - 이용권: `issued`, `undelivered`, `delivered`, `registered`
  - 등록기한 알림 대상 수: `registerReminderD30`, `registerReminderD7` (9.9와 같은 기준)
- 이용권 수치는 살아 있는(REVOKED가 아닌) 행 기준이다.

### 9.9 등록기한 임박 알림
PRD "등록기한 30일·7일 전 알림은 허용된 연락 수단에서만 발송". 미등록 이용권은 앱 계정에 연결되지 않아 푸시를 보낼 수 없다. 그래서 코드 전달(9.4)처럼 운영자가 후원자 연락처로 보내고 결과를 기록한다. 대상은 조회 시점에 계산하므로 별도 배치가 없다.

**대상 조회**
- `GET /api/v1/admin/funding/register-reminders?stage=D30`

| `stage` | 대상 등록기한 |
|---|---|
| `D30` | 지금부터 7일 초과 ~ 30일 이내 |
| `D7` | 지금부터 7일 이내 (지난 건 제외) |

- 대상: 전달 완료(`codeStatus=DELIVERED`)·미등록(`status=UNREGISTERED`)이고, 그 단계 알림을 아직 `SUCCESS`로 기록하지 않은 이용권이다. 등록기한이 빠른 순이다.
- 30일 알림을 놓치고 7일 안으로 들어온 이용권은 `D7` 대상만 된다.
- 미전달(`ISSUED`) 이용권은 대상이 아니다. 코드 전달(9.4)부터 해야 한다.

```json
{
  "statusCode": 200,
  "data": {
    "stage": "D30",
    "count": 1,
    "items": [
      {
        "ticketId": 12, "fundingOrderId": 3, "platform": "TUMBLBUG", "orderId": "ORD-30",
        "supporterName": "홍길동", "supporterEmail": "a@test.com", "supporterPhone": "010-0000-0000",
        "capacity": 15, "registerBy": "2026-11-17T10:00:00", "daysLeft": 20
      }
    ]
  }
}
```

- `daysLeft`: 등록기한까지 남은 일수 (KST 날짜 기준)

**발송 기록**
- `POST /api/v1/admin/funding/tickets/{ticketId}/register-reminders`

```json
{ "stage": "D30", "channel": "EMAIL", "status": "SUCCESS", "failureReason": null }
```

- `channel`: `EMAIL` / `SMS` / `MANUAL`
- `status`: `SUCCESS`면 그 단계 대상에서 빠진다. `FAILED`는 이력만 남고 대상에 그대로 있다(재시도 가능).
- 응답: `reminderId`, `ticketId`, `stage`, `channel`, `status`, `failureReason`, `attemptedAt`

| 상황 | 응답 |
|---|---|
| 전달 완료·미등록 이용권이 아님 (미전달·등록 완료·무효화) | 409 `TICKET_409_STATE` |
| 등록기한 경과 | 409 `TICKET_409_REG_EXP` |
| `stage`·`channel`·`status` 누락 | 400 `COMMON_400` |

---

## 10. 에러 코드 모음

### 10.1 클래스

| 코드 | HTTP | 메시지 |
|---|---|---|
| `CLASS_400_BATTLE` | 400 | 클래스에 사용할 수 없는 배틀입니다. |
| `CLASS_400_BATTLE_SOURCE` | 400 | 기존 배틀 ID 와 주제 버전 ID 중 하나만 보내 주세요. |
| `CLASS_400_DEADLINE` | 400 | 마감일은 현재 시각 이후, 이용권 종료일 이전이어야 합니다. |
| `CLASS_400_DEADLINE_EXT` | 400 | 마감일은 기존 마감일보다 뒤로 연장만 할 수 있습니다. |
| `CLASS_400_RECOMMEND_CONDITION` | 400 | 대상 수준과 카테고리 1~3개를 골라 주세요. 주제는 50자까지 입력할 수 있어요. |
| `CLASS_400_RECOMMEND_SETTINGS` | 400 | 클래스 추천을 켜려면 수준과 카테고리를 먼저 정해 주세요. |
| `CLASS_400_TOPIC_CONDITION` | 400 | 주제는 2~200자, 카테고리는 1~3개로 입력해 주세요. |
| `CLASS_400_DEBATE_REPLY` | 400 | 반박은 50자 이상 500자 이하로 적어 주세요. |
| `CLASS_400_DEBATE_PERSONAL_INFO` | 400 | 전화번호·이메일 같은 개인정보는 지우고 보내 주세요. |
| `CLASS_400_DEBATE_BLOCKED` | 400 | 다른 사람을 위협하거나 해칠 수 있는 표현은 보낼 수 없어요. 내용을 고쳐 주세요. |
| `CLASS_403_TICKET` | 403 | 사용 중인 이용권이 없어 클래스를 만들 수 없습니다. |
| `CLASS_403_OWNER` | 403 | 클래스 운영자만 할 수 있습니다. |
| `CLASS_403_MEMBER` | 403 | 클래스 멤버만 볼 수 있습니다. |
| `CLASS_403_REMOVED` | 403 | 내보내진 클래스에는 다시 참여할 수 없습니다. |
| `CLASS_403_OWNER_LEAVE` | 403 | 운영자는 클래스를 나가거나 내보내질 수 없습니다. 클래스를 삭제해 주세요. |
| `CLASS_404` | 404 | 존재하지 않는 클래스입니다. |
| `CLASS_404_CODE` | 404 | 존재하지 않는 초대코드입니다. |
| `CLASS_404_MEMBER` | 404 | 존재하지 않는 멤버입니다. |
| `CLASS_404_TOPIC_DRAFT` | 404 | 존재하지 않는 주제 초안입니다. |
| `CLASS_404_TOPIC_CANDIDATE` | 404 | 존재하지 않는 주제 후보입니다. |
| `CLASS_404_TOPIC_VERSION` | 404 | 존재하지 않는 주제 버전입니다. |
| `CLASS_404_DEBATE` | 404 | 시작한 토론이 없습니다. |
| `CLASS_409_CLOSED` | 409 | 마감된 클래스입니다. |
| `CLASS_409_TICKET_ENDED` | 409 | 이용권 이용이 종료된 클래스입니다. |
| `CLASS_409_FULL` | 409 | 정원이 가득 찬 클래스입니다. |
| `CLASS_409_DELETED` | 409 | 삭제되어 더 이상 이용할 수 없는 클래스입니다. |
| `CLASS_409_REQUEST_KEY` | 409 | 같은 요청 키로 다른 내용의 클래스를 만들 수 없습니다. 새 요청 키로 다시 시도해 주세요. |
| `CLASS_409_JOINED` | 409 | 이미 참여 중인 클래스입니다. |
| `CLASS_409_NAME` | 409 | 이미 사용 중인 이름입니다. 다른 이름을 입력해 주세요. |
| `CLASS_409_OWNER` | 409 | 내가 운영하는 클래스에는 참여자로 들어갈 수 없습니다. |
| `CLASS_409_PRE_VOTE_LOCKED` | 409 | 배틀이 시작된 뒤에는 사전투표를 변경할 수 없습니다. |
| `CLASS_409_TTS` | 409 | 커스텀 배틀 클래스에는 음성(TTS) 단계가 없습니다. |
| `CLASS_409_TTS_INCOMPLETE` | 409 | 오디오를 끝까지 들은 뒤 사후투표를 할 수 있어요. |
| `CLASS_409_MID_VOTE` | 409 | 중간투표가 없는 배틀입니다. |
| `CLASS_409_MID_VOTE_LOCKED` | 409 | 사후투표를 마친 뒤에는 중간투표를 바꿀 수 없어요. |
| `CLASS_409_MID_VOTE_REQUIRED` | 409 | 콘텐츠 중간의 선택을 마친 뒤 사후투표를 할 수 있어요. |
| `CLASS_404_MID_VOTE_OPTION` | 404 | 존재하지 않는 중간투표 선택지입니다. |
| `CLASS_409_PERSPECTIVE_REQUIRED` | 409 | 의견 작성이 필수인 클래스의 의견은 삭제할 수 없어요. 내용을 수정해 주세요. |
| `CLASS_409_RESULT_EXPIRED` | 409 | 결과 열람 기간이 끝났습니다. |
| `CLASS_409_REPORT_RETRY` | 409 | 지금은 AI 요약을 다시 시도할 수 없습니다. |
| `CLASS_409_TOPIC_DRAFT_USED` | 409 | 이 주제 초안으로 이미 만든 클래스가 있습니다. |
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
| `CLASS_500_CODE` | 500 | 초대코드 생성에 실패했습니다. 다시 시도해 주세요. |
| `CLASS_503_TOPIC_AI` | 503 | 응답을 준비하지 못했어요. 다시 시도해 주세요. |

### 10.2 이용권·펀딩

| 코드 | HTTP | 메시지 |
|---|---|---|
| `TICKET_404_CODE` | 404 | 유효하지 않은 이용권 코드입니다. |
| `TICKET_404` | 404 | 등록된 이용권이 없습니다. |
| `TICKET_409_CLAIMED` | 409 | 이미 등록된 이용권 코드입니다. |
| `TICKET_409_REVOKED` | 409 | 재발급되어 더 이상 사용할 수 없는 이용권 코드입니다. |
| `TICKET_409_REG_EXP` | 409 | 등록 기한이 지난 이용권 코드입니다. |
| `TICKET_409_ACTIVE` | 409 | 이미 사용 중인 이용권이 있습니다. 만료 후 등록할 수 있습니다. |
| `TICKET_409_STATE` | 409 | 현재 상태에서는 처리할 수 없는 이용권입니다. |
| `FUNDING_400_CSV` | 400 | CSV 파일을 읽을 수 없습니다. 템플릿 헤더와 UTF-8 인코딩을 확인해 주세요. |
| `FUNDING_404_ORDER` | 404 | 존재하지 않는 펀딩 주문입니다. |

### 10.3 클래스에서 함께 쓰는 공통 코드

| 코드 | HTTP | 클래스에서 나오는 곳 |
|---|---|---|
| `COMMON_400` | 400 | 초대코드·이용권 코드 형식, 표시 이름·클래스 이름 공백 |
| `BATTLE_404` | 404 | 클래스 생성 시 없는 배틀 |
| `BATTLE_OPT_404` | 404 | 이 배틀의 선택지가 아닌 투표, 사후투표 전 의견 등록 |
| `VOTE_404` | 404 | 사전투표 전 TTS 완료·사후투표·내 투표 조회 |
| `VOTE_409_PRE` | 409 | 진행 단계가 `NONE`인 상태의 사후투표 |
| `VOTE_409_SUB` | 409 | 사후투표 재제출 |
