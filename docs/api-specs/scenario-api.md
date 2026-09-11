# 시나리오(Scenario) API 명세

기준 코드:  
`src/main/java/com/swyp/picke/domain/scenario/controller/ScenarioController.java`  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminScenarioController.java`

## 1. 사용자 API

### 1.1 배틀 시나리오 조회
- `GET /api/v1/battles/{battleId}/scenario`
- 설명: 배틀 상세에서 시나리오 노드/스크립트/분기 옵션 조회

---

## 2. 관리자 API

### 2.1 배틀 기준 시나리오 상세 조회
- `GET /api/v1/admin/battles/{battleId}/scenario`

### 2.2 시나리오 생성
- `POST /api/v1/admin/scenarios`
- 요청 본문(`AdminScenarioCreateRequest`) 주요 필드:
  - `battleId`
  - `isInteractive`
  - `status` (`DRAFT`, `PUBLISHED`, `ARCHIVED`)
  - `nodes[]`
    - `nodeName`
    - `isStartNode`
    - `autoNextNode`
    - `scripts[]`
      - `speakerName`
      - `speakerType`
      - `text` (오디오 효과 태그 `[sighing]` `[break]` 등은 인라인으로 그대로 둔다)
      - `tone` (`Tone` enum, 생략 시 `NEUTRAL`: `NEUTRAL`/`ANGRY`/`SAD`/`EMBARRASSED`/`EMPHASIS`/`WHISPERING`/`SOFT_TONE`/`BREATHY`/`EXCITED`. 대사 한 줄의 감성적인 톤 — TTS 합성 시 `[tone]` 형태로 text 앞에 주입된다)
    - `interactiveOptions[]`
      - `label`
      - `title` (선택 시 보여줄 선택지 설명, 생략 가능)
      - `nextNodeName`
  - `voiceSettings` (`Map<SpeakerType, String>`, 값은 Fish Audio `reference_id`. 철학자 발화자(A/B)는 [철학자 보이스 매핑](./philosopher-voice-api.md)에서 조회해 채운다)

### 2.3 시나리오 본문 수정
- `PUT /api/v1/admin/scenarios/{scenarioId}`
- 설명: 노드/스크립트/분기/보이스 설정 포함 전체 콘텐츠 수정

### 2.4 시나리오 상태 수정
- `PATCH /api/v1/admin/scenarios/{scenarioId}`
- 요청 본문:
```json
{
  "status": "PUBLISHED"
}
```

### 2.5 시나리오 삭제
- `DELETE /api/v1/admin/scenarios/{scenarioId}`

---

## 3. 상태/동작 메모

- 임시저장(`DRAFT`) 상태에서는 대본/설정은 DB 저장, 발행(`PUBLISHED`) 시점에 TTS 파이프라인 수행
- 발행 후 수정 시에는 변경된 스크립트 조각만 재생성하고 병합 오디오를 갱신
- `text` 또는 `tone` 이 바뀐 스크립트만 오디오를 재생성한다 (그 외 조각은 S3 캐시 재사용)
- TTS 는 Fish Audio S2.1 Pro 사용 (`fishaudio.tts.model`, 기본값 `s2.1-pro`). 톤 태그는 `[...]` 대괄호로 `text` 앞에 붙어 전송된다
