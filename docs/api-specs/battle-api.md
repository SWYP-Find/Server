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

### 2.1 배틀 생성
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

### 2.2 배틀 목록
- `GET /api/v1/admin/battles`
- 쿼리 파라미터:
  - `page` (기본값: `1`)
  - `size` (기본값: `10`)
  - `status` (선택)

### 2.3 배틀 상세
- `GET /api/v1/admin/battles/{battleId}`

### 2.4 배틀 수정
- `PATCH /api/v1/admin/battles/{battleId}`
- 요청 본문(`AdminBattleUpdateRequest`) 필드 구조는 생성과 동일

### 2.5 배틀 삭제
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
