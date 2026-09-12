# 철학자 보이스 매핑(Philosopher Voice) API 명세

기준 코드:
`src/main/java/com/swyp/picke/domain/admin/controller/AdminPhilosopherVoiceController.java`
`src/main/java/com/swyp/picke/domain/scenario/service/PhilosopherVoiceService.java`

철학자 이름 → Fish Audio 보이스(`reference_id`) 매핑을 관리한다. [배틀 대본 붙여넣기 파서](./battle-api.md) 가
시나리오 발화자(옵션 A/B 철학자)에 맞는 보이스를 여기서 조회해 `scenario.voiceSettings` 를 채운다.
전부 관리자 전용 API. 사용자 API 없음.

## 1. 관리자 API

### 1.1 목록 조회
- `GET /api/v1/admin/philosopher-voices`
- 응답(`PhilosopherVoiceResponse[]`): `id`, `name`, `referenceId`, `voiceLabel`, `imageKey`, `note`
  - **`imageKey`는 DB의 raw 저장 키가 아니라 `ResourceUrlProvider`로 변환된 호출 가능한 경로다** (다른 이미지 필드인 배틀 썸네일·옵션 이미지와 동일 규칙). 값이 있으면 그대로 `<img src>`에 써도 된다. `null`이면 이미지가 등록 안 된 것 — 프론트 기본 이미지로 대체

### 1.2 생성
- `POST /api/v1/admin/philosopher-voices`
- 요청 본문(`PhilosopherVoiceRequest`):
  - `name` (필수, 유니크)
  - `referenceId` (필수, Fish Audio 보이스 모델 ID)
  - `voiceLabel` (선택, 표시용 라벨. 예: `"미호크 장정진"`)
  - `imageKey` (선택, 철학자 이미지 저장 키. 예: `"images/philosophers/rousseau.png"`)
  - `note` (선택)
- 이름 중복 시 `PHILOSOPHER_VOICE_409_DUP`

### 1.3 수정
- `PATCH /api/v1/admin/philosopher-voices/{id}`
- 요청 본문은 생성과 동일. `name` 은 응답에서 그대로 유지되고 `referenceId`/`voiceLabel`/`note` 만 갱신된다
- 대상이 없으면 `PHIL_VOICE_404`

### 1.4 삭제
- `DELETE /api/v1/admin/philosopher-voices/{id}`
- 대상이 없으면 `PHIL_VOICE_404`

## 2. 상태/동작 메모

- 테이블은 `spring.jpa.hibernate.ddl-auto=update` 가 자동 생성한다. 소프트 삭제 없음 (참조하는 FK가 없어 hard delete)
- 초기 시드: `docs/db/20260910_seed_philosopher_voice.sql` (`ON CONFLICT (name) DO NOTHING`) — 환경별 1회 수동 실행 필요. 재실행해도 안전하고 어드민에서 수정한 값을 덮지 않는다
- **NARRATOR / USER 고정 보이스는 이 테이블이 아니라 config** (`fishaudio.voice-id.narrator`, `fishaudio.voice-id.user`). 이 API 는 A/B(철학자) 보이스 전용
- 붙여넣기 파서가 발화자 이름으로 조회했는데 매핑이 없으면 `MISSING_VOICE` warning(blocking) 을 내고, 관리자가 미리보기에서 보이스를 고르거나 이 API 로 매핑을 추가한 뒤 다시 파싱해야 한다
- 배틀별로 다른 보이스를 쓰고 싶으면 이 테이블을 바꾸지 않고 시나리오의 `voiceSettings` 를 직접 오버라이드하면 된다(이 테이블은 파서가 채우는 기본값 소스일 뿐)
- **`imageKey`**: `PhilosopherType` enum(철학자 유형 10인)만 실제 이미지가 있고, 그 밖의 철학자(예: 루소, 홉스, 쇼펜하우어)는 원래 이름을 해시해서 10개 이미지 중 하나를 무작위로 배정하는 폴백이 있었다(`PhilosopherType.resolveImageKey`) — 엉뚱한 철학자 이미지가 뜨는 문제가 있었음. 프론트는 배틀 생성 화면에서 이 API의 `imageKey` 로 정확한 이미지를 조회해 쓰고, 값이 없으면(아직 등록 안 된 철학자) 기본 이미지로 대체해야 한다. `imageKey` 는 `null` 이어도 되며, 랜덤 배정 로직으로 대신하지 않는다
