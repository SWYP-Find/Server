# 배틀(Battle) API 명세

기준 코드: `src/main/java/com/swyp/picke/domain/battle/controller/BattleController.java`,  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminBattleController.java`

## 1. 사용자 API

### 1.1 오늘의 배틀 목록
- `GET /api/v1/battles/today`
- 설명: 오늘 노출 대상 배틀 목록 조회 (최대 5개)

### 1.2 배틀 목록
- `GET /api/v1/battles`
- 쿼리 파라미터:
  - `page` (기본값: `1`)
  - `size` (기본값: `10`)
  - `status` (기본값: `ALL`, 허용: `ALL`, `PENDING`, `PUBLISHED`, `REJECTED`, `ARCHIVED`)

### 1.3 배틀 상세
- `GET /api/v1/battles/{battleId}`
- 설명: 배틀 본문/선택지/태그/사용자 진행 상태 표시용 상세 조회

### 1.4 사용자 배틀 진행 상태
- `GET /api/v1/battles/{battleId}/status`
- 설명: 현재 로그인 사용자 기준 배틀 진행 단계 조회

---

## 2. 관리자 API

기준 컨트롤러: `AdminBattleController`

### 2.1 배틀 대본 붙여넣기 파싱 (미리보기용, 저장 안 함)
- `POST /api/v1/admin/battles/parse`
- 요청 본문(`AdminBattleParseRequest`):
  - `rawText` (기획자가 구글독스에서 복사한 대본 원문 전체)
- 설명: 대본을 파싱해 배틀·시나리오 등록 폼을 자동으로 채워준다. **DB 저장은 하지 않는다.** 문서 포맷 규격/파싱 규칙은 `배틀_발행_시나리오_현행_vs_개선.md` 2.6, 실제 파싱 예시는 `배틀_대본_파싱_예시.md` 참고.
- 응답(`AdminBattleParseResponse`):
  - `battlePayload` — 아래 2.2 배틀 생성 요청과 동일한 형태(`AdminBattleCreateRequest`). `status` 는 항상 `PENDING`. 문서에 없는 `thumbnailUrl`/`targetDate`/`publishAt`/`audioDuration` 은 `null`
    - `summary` = 오프닝 전문을 LLM 이 40자 이내로 한 줄 요약한 것. 요약 생성 실패 시 오프닝 원문 그대로 들어가고 `SUMMARY_GENERATION_FAILED` warning 이 붙는다
    - `description` = 오프닝 전문 원문 그대로
    - `options[].title` = **사전 투표 영역의 짧은 선택지명이 있으면 그걸 우선 사용**(예: `"유죄다" vs "무죄다"` → `유죄다`/`무죄다`), 사전 투표가 없으면 메타데이터 표의 "선택지 명칭"으로 대체
  - `scenarioPayload` — [시나리오 API](./scenario-api.md) 2.2 생성 요청과 동일한 형태(`AdminScenarioCreateRequest`). **`battleId` 는 항상 `null`** — 배틀이 아직 생성 전이라서다. 어드민이 `battlePayload` 로 배틀을 먼저 만들고, 응답으로 받은 `battleId` 를 채워 시나리오를 등록한다
  - `speakerNames` — `{"A": "플라톤", "B": "마르크스"}` 형태로 화자 A/B 에 바인딩된 철학자 이름
  - `warnings[]` — 아래 표. `blocking: true` 인 항목이 하나라도 있으면 미리보기에서 해결하기 전까지 발행하지 않는다
- **문장 단위 분리**: `scenarioPayload.nodes[].scripts[]` 는 원문의 한 발언(문단)을 그대로 담지 않고, 마침표/느낌표/물음표(`.` `!` `?`) 뒤 공백 기준으로 **문장마다 별도 script** 로 쪼갠다. 같은 발언에서 쪼개진 문장들은 `speakerName`/`speakerType`/`tone` 이 전부 동일하다. 오디오 파이프라인이 script 마다 무음(600ms)을 넣으므로 문장 단위 분리가 곧 자연스러운 끊어읽기가 된다. 종결부호 뒤에 닫는 따옴표(`"`/`'`)가 먼저 오면 그 지점은 안 끊긴다(알려진 제한사항).

**warning 코드**

| code | 의미 | blocking |
|---|---|---|
| `MISSING_HEADER` | 첫 줄에서 `— 제목` 형식을 못 찾음 | no |
| `MISSING_METADATA` / `MISSING_CATEGORY` | 메타데이터 섹션/카테고리를 못 찾음 | no |
| `UNKNOWN_CATEGORY_TAG` | 그 카테고리 문자열의 CATEGORY 태그가 DB 에 없음 | no |
| `UNKNOWN_PHILOSOPHER_TAG` | 철학자 키워드가 철학자 유형 10인이 아님 | no |
| `MISSING_PHILOSOPHER_TAG` / `MISSING_VALUE_TAG` | 태그 문자열은 맞지만 DB row 가 없음 | no |
| `UNKNOWN_VALUE_TAG` | 성향 지표가 가치관 12축 문자열(원칙/결과/이성/감성/개인/관계/변화/전통/내면/구조/이상/현실)이 아님 | no |
| `MULTIPLE_TONE_TAGS` | 대사 한 줄에 톤 태그가 여러 개 → 첫 번째만 사용 | no |
| `SPEAKER_BINDING_FALLBACK` | 발화자↔A/B 매칭을 등장 순서로 임시 배정 | no |
| `LLM_CLASSIFY_FAILED` | 감정 자동분류 실패 → 톤이 전부 `NEUTRAL` | no |
| `SUMMARY_GENERATION_FAILED` | 오프닝 한 줄 요약 생성 실패 → `summary` 에 오프닝 원문이 그대로 들어감 | no |
| `MISSING_VOICE` | 발화자 보이스가 [철학자 보이스 매핑](./philosopher-voice-api.md) 에 없음 | **yes** |
| `INCOMPLETE_SPEAKER_BINDING` | A/B 발화자를 확정하지 못함 | **yes** |

### 2.2 배틀 생성
- `POST /api/v1/admin/battles`
- 요청 본문(`AdminBattleCreateRequest`) 주요 필드:
  - `title`
  - `summary`
  - `description`
  - `thumbnailUrl`
  - `status` (`DRAFT`, `PUBLISHED`, `ARCHIVED` 등)
  - `tagIds` (카테고리 태그 ID 목록)
  - `options[]`
    - `label` (`A`, `B`, `C`, `D`)
    - `title`
    - `stance`
    - `representative`
    - `imageUrl`
    - `tagIds` (철학자/가치관 태그 ID 목록)

### 2.3 배틀 목록
- `GET /api/v1/admin/battles`
- 쿼리 파라미터:
  - `page` (기본값: `1`)
  - `size` (기본값: `10`)
  - `status` (선택)

### 2.4 배틀 상세
- `GET /api/v1/admin/battles/{battleId}`

### 2.5 배틀 수정
- `PATCH /api/v1/admin/battles/{battleId}`
- 요청 본문(`AdminBattleUpdateRequest`) 필드 구조는 생성과 동일

### 2.6 배틀 삭제
- `DELETE /api/v1/admin/battles/{battleId}`

---

## 3. 상태/정책 메모

- 배틀 전용 태그:
  - 카테고리 태그: `battle_tags`
  - 옵션 태그(철학자/가치관): `battle_option_tags`
- 옵션 개수 제한:
  - 최소 2개, 최대 4개 (`BATTLE_INVALID_OPTION_COUNT`)
- `target_date`:
  - 관리자 폼에서 직접 입력하지 않고 서버 정책으로 관리
