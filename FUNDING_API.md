# 펀딩 API — origin/dev 통합 기준

공통 주문·이용권 처리는 최신 `domain.ticket`과 `domain.admin`을 사용합니다.
`domain.funding`은 텀블벅 XLSX, 원본 행 보관, 상세 검색과 감사 로그를 확장합니다.
모든 아래 경로의 접두사는 `/api/v1`입니다.

| 기능 | Method / 경로 |
| --- | --- |
| CSV 주문 등록·갱신 | `POST /admin/funding/imports` |
| 주문·이용권 목록 및 텀블벅 원본 정보 | `GET /admin/funding/orders` |
| 캠페인별 후원자 목록 | `GET /admin/funding/orders?campaignId=...` |
| 텀블벅 캠페인 대시보드 요약 | `GET /admin/funding/orders/tumblbug/dashboard?campaignId=...` |
| 펀딩 현황 | `GET /admin/funding/summary` |
| 코드 발급 | `POST /admin/funding/tickets/issue` |
| 관리자 코드 조회·복사 | `GET /admin/funding/tickets/{ticketId}/code` |
| 전달 기록 | `POST /admin/funding/tickets/{ticketId}/deliveries` |
| 미등록 코드 재발급 | `POST /admin/funding/tickets/{ticketId}/reissue` |
| 주문 환불·이용권 취소 | `POST /admin/funding/refunds` |
| 사용자 코드 확인 | `GET /class-tickets/preview?code=...` |
| 사용자 코드 등록 | `POST /class-tickets/register` |
| 내 이용권 | `GET /class-tickets/me` |
| XLSX 시트·열·샘플 검사 | `POST /admin/funding-orders/tumblbug/inspect` |
| 선택 시트 검토 | `POST /admin/funding-orders/tumblbug/preview` |
| Start·Run 공통 엑셀 등록 및 발급 | `POST /admin/funding-orders/tumblbug/import` |
| Start·Run 공통 구매자 수동 등록 | `POST /admin/funding-orders/tumblbug/manual` |
| 주문 저장·발급·디지털 선물 XLSX 생성 | `POST /admin/funding-orders/tumblbug/digital-gifts` |
| 텀블벅 전달 결과 기록 | `PATCH /admin/funding-orders/tumblbug/digital-gifts/delivery` |
| 이용권·등록자·등록일 검색 | `GET /admin/tickets` |
| 이용권 관리 화면 요약 | `GET /admin/tickets/summary` |

## 텀블벅 XLSX

관리자 API는 `ROLE_ADMIN`이 필요합니다. 검사는 multipart `file`, 미리보기와 발급은 `file`과 JSON `options` 파트를 사용합니다.
파일은 XLSX 10MB 이하, 최대 5,000개 데이터 행입니다. 선물 수량은 1~7이며 코드도 수량만큼 발급합니다.

```json
{
  "campaignId": "picke-class-2026",
  "campaignName": "픽케 클래스 펀딩",
  "sheetName": "15인 이용권",
  "rewardLineId": "class-ticket-15",
  "capacity": 15,
  "sourceVersion": "export-20261021",
  "plan": "START"
}
```

`campaignId`는 50자 이하이며 `rewardLineId`를 생략하면 시트명을 사용합니다. `plan`은 `START` 또는 `RUN`이며 생략 시 기존 API와의 호환을 위해 `RUN`으로 처리합니다. 캠페인을 처음 등록할 때 선택한 요금제는 고정됩니다.
필수 열은 후원번호, 이름, 선물, 추가 후원금, 총 후원 금액, 후원 일시, 알림 신청, 결제 상태, 선물 전달입니다.
미리보기는 저장하지 않습니다. 중복 또는 잘못된 행이 있으면 발급 요청 전체를 거부합니다.

**결제 완료·후원일시 존재**인 주문은 구매 수량만큼 코드를 발급합니다.
결제 예약 상태는 저장만 하고 코드를 발급하지 않습니다. `codeEligibleRows`는 결제 완료된 행 수입니다.
등록기한은 결제일 + 1년, 이용기간은 등록일 + 6개월입니다.
재발급은 기존 이용권을 무효화하고 같은 등록기한으로 새 행을 생성합니다.
코드 형식은 `XXXXXX-XXXXX-XXXXX-XXXXX-XXXXX`입니다. SHA-256 해시로 코드를 검증하고 AES-GCM 암호문으로 관리자 코드 보기를 지원합니다.

발급 주문은 최신 `FundingOrder`에 저장하고 `AdminFundingService`로 `ClassTicket`을 발급합니다.
후원번호·리워드 키·금액·알림·원본 전달 상태·원본 행 JSON은 `TumblbugImportRecord`에 보관합니다.
내부 주문번호는 캠페인·후원번호·리워드 키의 SHA-256으로 만든 50자 식별자 `TB-...`입니다.
기존 CSV에 동일 후원번호의 주문이 있는 경우에도 중복으로 취급합니다.

전달 결과 요청 필드는 `campaignId`, `rewardLineId`, `supporterNumbers`, `status`(`SENT`/`FAILED`), `failedReason`입니다.
실패에는 사유가 필요합니다. 공통 이력에는 `TUMBLBUG_DIGITAL_GIFT` 채널 및 `SUCCESS`/`FAILED` 상태로 기록합니다.
이미 등록·취소되었거나 결과를 기록한 코드는 건너뛰고, 재발급 코드는 새 이용권 ID로 구분합니다.
Start는 공통 `import` 또는 `manual` 경로로 등록하고 1:1 메시지로 직접 전달합니다. Run은 같은 경로에 더해 `digital-gifts` XLSX 경로를 쓸 수 있습니다. 실제 텀블벅 전달은 관리자가 수행합니다.

## 펀딩 구매자 수동 등록

관리자 화면의 `구매자 등록`에서 한 건을 입력할 때 사용합니다. XLSX를 올리는 `import`와 달리 한 구매자 정보를 JSON으로 등록합니다.

- `POST /api/v1/admin/funding-orders/tumblbug/manual`
- 관리자(`ROLE_ADMIN`) 전용이며 요청·응답은 캐시되지 않습니다.
- 동일 캠페인·후원번호·리워드 행 조합이 이미 있으면 409 `FUNDING_DUPLICATE_SUPPORTER`입니다.
- `paymentStatus=PAID_CONFIRMED`이면 구매 수량만큼 이용권 코드를 발급해 `issuedCodes[]`에 반환합니다. `RESERVED`이면 코드를 발급하지 않습니다.
- `giftDeliveryStatus=SENT`는 실제 이메일/SMS를 발송하지 않고, 관리자가 텀블벅 또는 별도 수단으로 전달을 완료했다는 수동 이력을 기록합니다. `PENDING`은 전달 기록을 남기지 않습니다.

**요청 예시**

```json
{
  "plan": "RUN",
  "campaignId": "picke-class-2026",
  "campaignName": "픽케 클래스 펀딩",
  "supporterNumber": "SUPPORTER-001",
  "rewardLineId": "class-ticket-15",
  "buyerName": "홍길동",
  "rewardName": "클래스 15인 이용권",
  "giftText": "선물 메시지",
  "capacity": 15,
  "quantity": 1,
  "supportedAt": "2026-11-01T10:00:00",
  "rewardAmount": 19900,
  "additionalSupportAmount": 0,
  "totalSupportAmount": 19900,
  "paymentStatus": "PAID_CONFIRMED",
  "giftDeliveryStatus": "PENDING"
}
```

`capacity`는 5·15·30, `quantity`는 1~7입니다. 결제 상태는 `RESERVED` 또는 `PAID_CONFIRMED`, 전달 상태는 `PENDING` 또는 `SENT`입니다. `SENT`는 결제 완료 주문에만 허용됩니다. 금액은 0 이상이고 총 후원액은 리워드 금액과 추가 후원액 합계 이상이어야 합니다.

응답에는 `fundingOrderId`, 캠페인·구매자·리워드 요약, 결제·선물 전달 상태, 발급 코드 `issuedCodes: [{ ticketId, code }]`가 들어갑니다. 이용권 코드 원문은 관리자 응답에서만 확인할 수 있습니다.

현재 수동 입력 요청은 구매자 이름만 받고 이메일·전화번호 필드는 받지 않습니다. 화면의 구매자 등록 폼에서 연락처까지 입력·보관해야 한다면 DTO/API 확장이 필요합니다. CSV·텀블벅 엑셀 등록 경로와 수동 등록 경로는 입력 계약이 서로 다릅니다.

## 관리자 주문·이용권 대시보드

### 주문 목록·전달 상태

`GET /api/v1/admin/funding/orders`는 `campaignId`, `paymentStatus`, `ticketStatus`, `codeStatus`, `keyword`, `capacity`, `deliveryStatus`, `page`, `size`를 지원합니다. `keyword`는 주문번호·후원자명·이메일·전화번호·텀블벅 후원번호를 검색합니다. 페이지는 0부터 시작하며 기본 크기는 20, 최대 크기는 100입니다.

주문 `deliveryStatus`는 살아 있는 이용권들의 최신 전달 기록을 합산한 값입니다. 모두 성공(`SUCCESS`)이면 `SENT`, 하나라도 최신 기록이 실패(`FAILED`)면 `FAILED`, 그 외(미발급·미전달 포함)는 `PENDING`입니다. 같은 주문 단위 규칙으로 필터링하며 목록과 count query가 동일한 조건을 사용합니다.

기존 주문 응답에 `tickets[]`의 이용권별 전달 결과와 `deliveryStatus`를 포함합니다. 텀블벅 주문은 `tumblbug`에 요금제·후원일시·상품·금액·후원번호 등 원본 정보를 포함합니다. 코드 원문·해시는 주문 목록에 포함하지 않습니다. 전체 코드 확인은 관리자 전용 `GET /api/v1/admin/funding/tickets/{ticketId}/code`로 하고, 조회 감사 로그를 남기며 캐시하지 않습니다.

### 펀딩 현황

`GET /api/v1/admin/funding/summary`의 주문 필드는 `totalOrders`, `paidConfirmed`, `issuable`, `held`, `refunded`, `canceled`입니다. `issuable`은 결제 확정·결제일 존재·미발급 수량이 남은 주문 수이고, `held`는 결제 확정인데 결제일이 없거나 결제 대기·예약 상태인 주문 수입니다. 이용권 필드는 무효화되지 않은 코드 행 기준 `issued`, `undelivered`, `delivered`, `registered`이며, 등록기한 알림 대상 수 `registerReminderD30`, `registerReminderD7`도 반환합니다.

`POST /api/v1/admin/funding/imports`의 `issuableCount`, `heldCount`는 업로드 이후 전체 주문을 같은 기준으로 집계합니다. 결제 완료 주문은 `quantity`만큼 발급하므로 수량이 2 이상이라는 이유만으로 보류하지 않습니다. 코드 발급 규칙은 [클래스 API 명세 §9.2–9.3](docs/api-specs/class-api.md)을 참고하세요.

### 텀블벅 캠페인 대시보드

`GET /api/v1/admin/funding/orders/tumblbug/dashboard?campaignId=...`는 주문 단위 `supporterCount`, `paidConfirmed`, `delivered`, `undelivered`와 코드 단위 `issuedCodes`, `deliveredCodes`, `undeliveredCodes`를 반환합니다. 주문은 살아 있는 이용권이 모두 전달된 경우에만 전달 완료로 집계하고, 코드 수치는 이용권별 최신 전달 결과를 기준으로 계산합니다.

### 이용권·등록자 요약과 검색

`GET /api/v1/admin/tickets/summary`는 `totalTickets`, `inUse`, `unregistered`, `expiringWithin30Days`를 반환합니다. `totalTickets`는 REVOKED를 제외하고, `unregistered`는 등록기한이 지난 미등록 이용권도 포함합니다. `expiringWithin30Days`는 등록기한이 아니라 이용권 종료일이 30일 이내인 현재 이용 중 이용권 수이며 `inUse`와 겹칠 수 있습니다.

`GET /api/v1/admin/tickets`는 `keyword`, `capacity`, `status`, `registeredFrom`, `registeredTo`, `page`, `size`를 지원합니다. 연락처와 코드는 마스킹하며 코드 검색은 SHA-256 해시 또는 통합 이후 저장된 끝 4자리로 비교합니다. 기존 해시로 끝 4자리를 복원할 수는 없습니다.

### 클래스·콘텐츠 관리 API 명세

클래스 목록·상세·요약·CSV 내보내기는 [클래스 API 명세 §9.10](docs/api-specs/class-api.md)을 참고하세요. 콘텐츠 관리 목록·요약은 [배틀 API 명세 §2.8](docs/api-specs/battle-api.md)을 참고하세요.

코드 암호화를 위해 운영 환경에 `TICKET_CODE_ENCRYPTION_KEY`로 Base64 인코딩한 32바이트 키를 설정해야 합니다. 기존 해시 전용 코드 행은 원문 복구가 불가능하며, 미등록 기존 코드는 재발급해야 새 코드 조회가 가능합니다.

클래스는 기존 `class_tickets`와 기존 이용권 확인 로직을 계속 사용합니다.
스테이징된 `FundingTicketAccess`, `FundingTicketPolicy`는 계약 파일로만 유지합니다.
현재 호출자가 없는 별도 정책 구현은 제공하지 않습니다.

## 추가 스키마

`V20261021_02__tumblbug_import_extensions.sql`과 `V20261025_01__funding_plan_and_ticket_code_vault.sql`을 기존 클래스 스키마 이후 적용합니다.
`funding_campaigns`, `tumblbug_import_records`, `funding_admin_audit_logs`, 코드 접미사,
전달 채널 길이 확장, 요금제, 암호화 코드 보관, 주문 수량당 복수 코드 제약 변경을 포함합니다. 별도 Funding 주문·이용권·코드·전달 테이블은 생성하지 않습니다.
이 저장소에는 Flyway 의존성이 없으므로 기존 DB 반영 절차에서 SQL을 적용해야 합니다.
