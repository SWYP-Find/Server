# 알림 API 명세서 (인앱 알림 + 푸시)

---

## 1. 설계 메모

- 알림은 **인앱 알림(알림함)** 과 **푸시**, 두 트랙으로 동작합니다.
  - 인앱 알림: `GET /api/v1/notifications`로 조회하는 알림함 데이터
  - 푸시: 앱이 백그라운드/종료 상태일 때 기기로 직접 발송되는 푸시. Android는 FCM, iOS는 APNs로 직접 발송됩니다.
- **NEW_BATTLE / COMMENT_LIKE / NEW_COMMENT** 는 인앱 알림 + 푸시 둘 다 발송됩니다.
- **CREDIT_EARNED / POLICY_CHANGE / PROMOTION** 은 인앱 알림만 발송됩니다 (푸시 없음).
- **VOTE_RESULT** 는 현재 보류 상태로, 발생하지 않습니다.
- 인증이 필요한 모든 API는 `Authorization: Bearer {access_token}` 헤더를 요구합니다.
- ⚠️ 이 도메인의 응답/요청 필드는 다른 API 문서(`snake_case`)와 달리 **`camelCase`** 입니다. (예: `notificationId`, `referenceId`, `perspectiveId`, `isRead`, `fcmToken`)

### 1.1 `NotificationCategory`

| 값 | 설명 |
|---|---|
| `ALL` | 전체 (목록 조회 시 필터 없음과 동일) |
| `CONTENT` | 콘텐츠 알림 (배틀, 답글 좋아요/댓글, 포인트 적립) |
| `NOTICE` | 공지사항 |
| `EVENT` | 이벤트/프로모션 |

### 1.2 `detailCode` 목록

| detailCode | category | 기본 title | 채널 | referenceId 의미 | perspectiveId |
|---|---|---|---|---|---|
| `NEW_BATTLE` | CONTENT | 새로운 배틀이 시작되었어요 | 인앱 + 푸시 | battleId | - |
| `COMMENT_LIKE` | CONTENT | 내 답글에 좋아요가 달렸어요 | 인앱 + 푸시 | commentId | perspectiveId |
| `NEW_COMMENT` | CONTENT | 내 답글에 댓글이 달렸어요 | 인앱 + 푸시 | commentId | perspectiveId |
| `CREDIT_EARNED` | CONTENT | 포인트 적립 | 인앱만 | 적립 종류별로 다름(아래 참고) | - |
| `POLICY_CHANGE` | NOTICE | 공지사항 | 인앱만 | 없음(`null`) | - |
| `PROMOTION` | EVENT | 이벤트 | 인앱만 | 없음(`null`) | - |
| `VOTE_RESULT` | CONTENT | 투표 결과가 나왔어요 | (보류, 미발생) | - | - |

`CREDIT_EARNED`의 `referenceId`는 적립 트리거에 따라 배틀 ID, 투표 ID, 제안 ID 등으로 다양하게 들어갈 수 있어 클라이언트에서 별도 화면 이동에 사용하지 않는 것을 권장합니다. `title`/`body` 텍스트만 표시하면 됩니다.

---

## 2. 디바이스(푸시 토큰) 등록/해제 API

### 2.1 `POST /api/v1/devices`

로그인 성공 직후, 또는 푸시 토큰이 갱신될 때 호출합니다. 이미 등록된 토큰이면 현재 로그인한 유저로 재할당됩니다(같은 기기에서 계정을 바꿔 로그인하는 경우 대응).

요청 헤더:

- `Authorization: Bearer {access_token}`

요청 바디:

```json
{
  "fcmToken": "device_push_token_string",
  "platform": "ANDROID"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `fcmToken` | `string` | Y | `platform=ANDROID`이면 FCM 토큰, `platform=IOS`이면 APNs 디바이스 토큰(`didRegisterForRemoteNotificationsWithDeviceToken`에서 받은 토큰을 hex 문자열로 변환한 값) |
| `platform` | `string` | Y | `ANDROID` \| `IOS` |

성공 응답 `200 OK`:

```json
{
  "statusCode": 200,
  "data": null,
  "error": null
}
```

---

### 2.2 `DELETE /api/v1/devices`

로그아웃 시 호출합니다. 등록되어 있지 않은 토큰을 보내도 에러 없이 `200`을 반환합니다(idempotent).

요청 헤더:

- `Authorization: Bearer {access_token}`

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `fcmToken` | `string` | Y | 해제할 디바이스 푸시 토큰 |

성공 응답 `200 OK`:

```json
{
  "statusCode": 200,
  "data": null,
  "error": null
}
```

---

## 3. 알림함 API

### 3.1 `GET /api/v1/notifications`

알림함 목록을 최신순으로 조회합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `category` | `string` | N | `ALL` \| `CONTENT` \| `NOTICE` \| `EVENT` (생략 시 전체) |
| `page` | `integer` | N | 페이지 번호 (기본값 `0`) |
| `size` | `integer` | N | 페이지 크기 (기본값 `20`) |

성공 응답 `200 OK`:

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "notificationId": 101,
        "category": "CONTENT",
        "detailCode": "NEW_BATTLE",
        "title": "새로운 배틀이 시작되었어요",
        "body": "\"민트초코, 호불호의 끝판왕\"에 지금 참여해보세요!",
        "referenceId": 55,
        "perspectiveId": null,
        "isRead": false,
        "createdAt": "2026-06-10T09:00:00"
      },
      {
        "notificationId": 100,
        "category": "CONTENT",
        "detailCode": "COMMENT_LIKE",
        "title": "내 답글에 좋아요가 달렸어요",
        "body": "\"민트초코, 호불호의 끝판왕\" 배틀에 남긴 내 답글에 좋아요가 달렸어요.",
        "referenceId": 678,
        "perspectiveId": 45,
        "isRead": true,
        "createdAt": "2026-06-09T18:30:00"
      }
    ],
    "hasNext": false
  },
  "error": null
}
```

탭(클릭) 시 화면 이동 로직은 [5. detailCode별 처리 가이드](#5-detailcode별-처리-가이드) 참고.

---

### 3.2 `GET /api/v1/notifications/unread`

알림함 벨 아이콘 배지 표시용으로, 미읽음 알림 존재 여부만 가볍게 확인합니다. 목록 조회와 달리 페이지네이션 없이 boolean 하나만 반환합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `category` | `string` | N | `ALL` \| `CONTENT` \| `NOTICE` \| `EVENT` (생략 시 전체 기준) |

성공 응답 `200 OK`:

```json
{
  "statusCode": 200,
  "data": {
    "hasUnread": true
  },
  "error": null
}
```

---

### 3.3 `GET /api/v1/notifications/{notificationId}`

알림 상세를 조회합니다. (목록과 동일한 필드 + `readAt`)

요청 헤더:

- `Authorization: Bearer {access_token}`

성공 응답 `200 OK`:

```json
{
  "statusCode": 200,
  "data": {
    "notificationId": 100,
    "category": "CONTENT",
    "detailCode": "COMMENT_LIKE",
    "title": "내 답글에 좋아요가 달렸어요",
    "body": "\"민트초코, 호불호의 끝판왕\" 배틀에 남긴 내 답글에 좋아요가 달렸어요.",
    "referenceId": 678,
    "perspectiveId": 45,
    "isRead": true,
    "createdAt": "2026-06-09T18:30:00",
    "readAt": "2026-06-09T19:00:00"
  },
  "error": null
}
```

예외 응답 `404 - 알림 없음`:

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "NOTIFICATION_404",
    "message": "존재하지 않는 알림입니다."
  }
}
```

---

### 3.4 `PATCH /api/v1/notifications/{notificationId}/read`

알림 1건을 읽음 처리합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

성공 응답 `200 OK`:

```json
{
  "statusCode": 200,
  "data": null,
  "error": null
}
```

---

### 3.5 `PATCH /api/v1/notifications/read-all`

알림함의 모든 알림을 읽음 처리합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

성공 응답 `200 OK`: 처리 직후 미읽음 알림 존재 여부(`hasUnread`)를 함께 내려줍니다. 모든 알림을 읽었다면 `hasUnread`는 `false`입니다.

```json
{
  "statusCode": 200,
  "data": {
    "hasUnread": false
  },
  "error": null
}
```

---

## 4. 푸시 처리 가이드 (Android: FCM / iOS: APNs)

`NEW_BATTLE`, `COMMENT_LIKE`, `NEW_COMMENT` 발생 시 등록된 디바이스로 푸시가 발송됩니다. Android는 FCM, iOS는 FCM을 거치지 않고 APNs로 직접 발송되며, 플랫폼별 메시지 구성이 다르므로 주의해주세요.

### 4.1 Android (FCM)

- **data-only** 메시지로 발송됩니다 (notification 블록 없음).
- 앱이 직접 `data`를 파싱해 로컬 알림(Notification)을 생성하고, 알림 탭 시에도 `data`로 라우팅을 처리해야 합니다.

### 4.2 iOS (APNs 직접 발송)

- FCM을 거치지 않고 서버가 APNs로 직접 발송하는 **alert(`aps.alert`) + 커스텀 데이터** 페이로드입니다. 커스텀 데이터(`type`, `battleId`, `url` 등)는 `aps`와 동일 레벨의 top-level 키로 들어갑니다.
- OS가 자동으로 알림을 표시합니다. 알림 탭 시 `userInfo`의 커스텀 키(또는 `url`의 유니버설 링크)로 라우팅합니다.
- `apple-app-site-association`이 `paths: ["*"]` 와일드카드로 설정되어 있어, `url`에 포함된 모든 경로가 유니버설 링크로 동작합니다. 별도 서버 설정 변경 없이 사용 가능합니다.
- **Firebase SDK가 더 이상 필요하지 않습니다.** 디바이스 등록 시 보내는 토큰은 FCM 토큰이 아니라 `didRegisterForRemoteNotificationsWithDeviceToken`에서 받는 APNs 디바이스 토큰(hex 문자열)입니다.
- 서버의 APNs 환경(sandbox/production)은 전역 설정 1개로 고정됩니다. 개발/TestFlight 빌드(sandbox 토큰)와 App Store 빌드(production 토큰)를 동시에 지원하려면 별도 협의가 필요합니다.

### 4.3 `data` 페이로드 포맷

**NEW_BATTLE**

```json
{
  "type": "BATTLE",
  "battleId": "55",
  "url": "https://picke.store/battle/55"
}
```

**COMMENT_LIKE / NEW_COMMENT**

```json
{
  "type": "COMMENT",
  "perspectiveId": "45",
  "commentId": "678",
  "url": "https://picke.store/perspective/45?commentId=678"
}
```

> `data`의 모든 값은 FCM 규격상 문자열(string)입니다. ID 값을 정수로 파싱해서 사용해주세요.

탭 시 `type` 값으로 분기:

- `"BATTLE"` → `battleId`로 배틀 상세 화면 이동
- `"COMMENT"` → `perspectiveId`로 관점(댓글) 화면 이동 후 `commentId` 위치로 스크롤/하이라이트

푸시 데이터에는 알림함의 `notificationId`가 포함되어 있지 않습니다. 푸시 탭 후 별도로 읽음 처리가 필요하면 알림함 목록에서 동일 `referenceId`/`perspectiveId`를 가진 항목을 찾아 `PATCH /api/v1/notifications/{notificationId}/read`를 호출해주세요.

---

## 5. detailCode별 처리 가이드

| detailCode | 탭(클릭) 시 동작 |
|---|---|
| `NEW_BATTLE` | `referenceId`(=battleId)로 배틀 상세 화면 이동 |
| `COMMENT_LIKE` / `NEW_COMMENT` | `perspectiveId`로 관점 화면 이동 후 `referenceId`(=commentId) 위치로 스크롤/하이라이트 |
| `CREDIT_EARNED` | 별도 이동 없음. `title` / `body` 텍스트만 표시 |
| `POLICY_CHANGE` / `PROMOTION` | 별도 이동 없음. `title` / `body` 텍스트만 표시 |

---

## 6. 관리자 API

모든 API는 `ROLE_ADMIN` 권한을 가진 계정만 호출 가능합니다 (`Authorization: Bearer {access_token}`, 403 시 `COMMON_403` 등 공통 권한 에러).

### 6.1 공지/이벤트 (`/api/v1/admin/notices`)

즉시 발송되는 공지사항/이벤트 알림을 관리합니다.

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/v1/admin/notices` | 공지사항/이벤트 작성 (즉시 전체 발송) |
| `GET` | `/api/v1/admin/notices` | 공지 목록 조회 (`category`, `page`, `size` 쿼리) |
| `GET` | `/api/v1/admin/notices/{noticeId}` | 공지 상세 조회 |
| `POST` | `/api/v1/admin/notices/test` | 특정 유저 대상 테스트 푸시 발송 (알림 설정 ON/OFF 무관) |

**`POST /api/v1/admin/notices` 요청 바디**

```json
{
  "category": "NOTICE",
  "title": "서비스 점검 안내",
  "body": "8/30 02:00~04:00 점검이 진행됩니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `category` | `string` | Y | `CONTENT` \| `NOTICE` \| `EVENT` |
| `title` | `string` | Y | 알림 제목 |
| `body` | `string` | Y | 알림 본문 |

응답은 `AdminNoticeDetailResponse` (`notificationId`, `category`, `detailCode`, `title`, `body`, `referenceId`, `createdAt`).

**`POST /api/v1/admin/notices/test` 요청 바디**

```json
{
  "userId": 123,
  "title": "테스트 알림",
  "body": "테스트 발송입니다."
}
```

### 6.2 예약 알림 (`/api/v1/admin/notification-schedules`)

매일 지정된 시각에 전체 유저에게 자동 발송되는 예약 알림(`NotificationSchedule`)을 관리합니다. 매분 `NotificationScheduleDispatcher`가 `enabled=true`인 예약 중 `sendTime`(시:분)이 현재 시각과 일치하는 건을 찾아 발송하며, 같은 날 중복 발송되지 않도록 `lastSentDate`로 발송 이력을 관리합니다.

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/v1/admin/notification-schedules` | 예약 알림 등록 |
| `GET` | `/api/v1/admin/notification-schedules` | 예약 알림 전체 목록 조회 |
| `GET` | `/api/v1/admin/notification-schedules/{scheduleId}` | 예약 알림 상세 조회 |
| `PUT` | `/api/v1/admin/notification-schedules/{scheduleId}` | 예약 알림 수정 (제목/부제목/발송시간/on-off 전체 교체) |
| `PATCH` | `/api/v1/admin/notification-schedules/{scheduleId}/toggle` | 예약 알림 On/Off만 전환 |
| `DELETE` | `/api/v1/admin/notification-schedules/{scheduleId}` | 예약 알림 삭제 |

**`POST` / `PUT` 요청 바디**

```json
{
  "title": "오늘의 질문",
  "subtitle": "지금 확인해보세요",
  "sendTime": "19:00:00",
  "enabled": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | `string` | Y | 알림 제목 |
| `subtitle` | `string` | Y | 알림 부제목(본문) |
| `sendTime` | `string` (`HH:mm:ss`) | Y | 매일 발송할 시각 (KST 기준, 분 단위로 매칭) |
| `enabled` | `boolean` | Y | 활성화 여부 |

**`PATCH .../toggle` 요청 바디**

```json
{
  "enabled": false
}
```

**응답 (`AdminNotificationScheduleResponse`)**

```json
{
  "statusCode": 200,
  "data": {
    "id": 3,
    "title": "오늘의 질문",
    "subtitle": "지금 확인해보세요",
    "sendTime": "19:00:00",
    "enabled": true,
    "createdAt": "2026-08-20T10:00:00"
  },
  "error": null
}
```

목록 조회(`GET /api/v1/admin/notification-schedules`) 응답은 `{ "schedules": [AdminNotificationScheduleResponse, ...] }` 형태입니다.

예외 응답 `404 - 예약 알림 없음`:

```json
{
  "statusCode": 404,
  "data": null,
  "error": {
    "code": "NOTIFICATION_404_SCHEDULE",
    "message": "존재하지 않는 알림 예약입니다."
  }
}
```

---

## 7. 에러 코드

| Error Code | HTTP Status | 설명 |
|---|:---:|---|
| `COMMON_400` | `400` | 요청 파라미터가 잘못되었습니다. (예: `fcmToken`/`platform` 누락) |
| `USER_404` | `404` | 존재하지 않는 사용자입니다. |
| `NOTIFICATION_404` | `404` | 존재하지 않는 알림입니다. (본인 소유가 아닌 알림 포함) |
| `NOTIFICATION_404_SCHEDULE` | `404` | 존재하지 않는 예약 알림입니다. |
