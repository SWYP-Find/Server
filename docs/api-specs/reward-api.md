# 보상(Reward) API 명세서

## 1. 설계 메모

- AdMob의 **SSV(Server-Side Verification)** 콜백 수신을 위한 API입니다.
- 모든 필드명은 AdMob 가이드라인에 따라 `snake_case`를 사용합니다.
- **중복 지급 방지**: `transaction_id`를 고유 식별자로 사용하여 동일 요청 재유입 시 차단(Idempotency)합니다.
- **유저 식별**: `custom_data` 필드에 담긴 값을 내부 `user_id`로 매핑하여 처리합니다.
- **타입 검증**: `reward_item` 값은 내부 `RewardType` Enum과 매핑하며, 정의되지 않은 값(예: "123")은 에러 처리합니다.
- **데이터 보존**: 보상 요청의 성공 이력을 `ad_reward_history` 테이블에 적재합니다.

---

## 2. AdMob 보상 콜백 API

### 2.1 `GET /api/v1/admob/reward`

광고 시청 완료 후 구글 서버에서 보내는 보상 지급 콜백 수신.

**쿼리 파라미터:**

| Parameter | Type | Required | 설명 |
|-----------|:----:|:---:|------|
| `ad_unit_id` | `String` | Y | 광고 단위 ID |
| `custom_data` | `String` | Y | 유저 식별자 (내부 User ID) |
| `reward_amount` | `int` | Y | 보상 수량 |
| `reward_item` | `String` | Y | 보상 아이템 이름 (e.g., "POINT") |
| `timestamp` | `long` | Y | 요청 생성 시간 |
| `transaction_id` | `String` | Y | **중복 방지용 고유 ID** |
| `signature` | `String` | N | 검증용 서명 |
| `key_id` | `String` | N | 검증용 공개키 ID |

**응답 (성공):**

```json
{
  "statusCode": 200,
  "data": "OK",
  "error": null
}
```

**응답 (중복 요청 시):**

```JSON
{
    "statusCode": 200,
    "data": "Already Processed",
    "error": null
}
```

---

## 3. 내 보상 이력 API

### 3.1 GET /api/v1/me/rewards/history

로그인한 사용자의 보상 획득 이력 조회.쿼리 파라미터

```JSON
{
   "statusCode": 200,
   "data": {
   "items": [
       {
           "history_id": 105,
           "reward_type": "POINT",
           "reward_amount": 100,
           "transaction_id": "unique_trans_id_20260327_001",
           "created_at": "2026-03-27T18:00:00Z"
       }
   ], 
     "next_cursor": 104
   }, 
     "error": null
   }
```

## 4. 에러 코드

### 4.1 보상 관련 에러 코드

### 🚨 보상 API 에러 응답 JSON 샘플

**1. 유저를 찾을 수 없을 때 (REWARD_INVALID_USER)**
- 상황: `custom_data`로 넘어온 ID가 DB에 없는 유저일 경우
```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "REWARD_INVALID_USER",
    "message": "해당 유저를 찾을 수 없습니다. (custom_data: 1)"
  }
}
```

**2. 잘못된 보상 타입일 때 (REWARD_INVALID_TYPE)**
- 상황: `reward_item`에 Enum에 정의되지 않은 값(예: "123")이 들어온 경우
```json
{
  "statusCode": 400,
  "data": null,
  "error": {
    "code": "REWARD_INVALID_TYPE",
    "message": "지원하지 않는 reward_item 타입입니다. (입력값: 123)"
  }
}
```

**3. 서명 검증 실패 시 (REWARD_VERIFICATION_FAILED)**
- 상황: AdMob이 보낸 `signature`가 올바르지 않아 위변조가 의심될 경우
```json
{
  "statusCode": 401,
  "data": null,
  "error": {
    "code": "REWARD_VERIFICATION_FAILED",
    "message": "AdMob 서명 검증에 실패하였습니다. 요청의 유효성을 확인하세요."
  }
}
```
---

##  공통 에러 코드

| Error Code | HTTP Status | 설명                                  |
|------------|:-----------:|-------------------------------------|
| `REWARD_INVALID_USER` | `404` | custom_data에 해당하는 유저가 존재하지 않음       |
| `REWARD_INVALID_TYPE` | `400` | 지원하지 않는 reward_item 타입 (Enum 미매칭)   |
| `REWARD_VERIFICATION_FAILED` | `401` | AdMob 서명(Signature) 검증 실패           |

---