# 태그(Tag) API 명세

기준 코드:  
`src/main/java/com/swyp/picke/domain/tag/controller/TagController.java`  
`src/main/java/com/swyp/picke/domain/admin/controller/AdminTagController.java`

## 1. 태그 타입

`TagType`
- `CATEGORY`
- `PHILOSOPHER`
- `VALUE`

---

## 2. 사용자 API

### 2.1 태그 목록 조회
- `GET /api/v1/tags`
- 쿼리 파라미터:
  - `type` (선택): `CATEGORY`, `PHILOSOPHER`, `VALUE`

---

## 3. 관리자 API

### 3.1 태그 생성
- `POST /api/v1/admin/tags`
- 요청 본문:
```json
{
  "name": "자유",
  "type": "VALUE"
}
```

### 3.2 태그 수정
- `PATCH /api/v1/admin/tags/{tagId}`
- 요청 본문:
```json
{
  "name": "연대",
  "type": "VALUE"
}
```

### 3.3 태그 삭제
- `DELETE /api/v1/admin/tags/{tagId}`

---

## 4. 매핑 정책(중요)

- 현재 태그는 **배틀 도메인에서만 사용**
- 매핑 테이블:
  - `battle_tags` (배틀-카테고리)
  - `battle_option_tags` (배틀 옵션-철학자/가치관)
- 퀴즈/폴은 태그 매핑을 사용하지 않음
