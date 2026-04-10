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
      - `text`
    - `interactiveOptions[]`
      - `label`
      - `nextNodeName`
  - `voiceSettings` (`Map<SpeakerType, String>`)

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
