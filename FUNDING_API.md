# 펀딩 API — origin/dev 통합 기준

공통 주문·이용권 처리는 최신 `domain.ticket`과 `domain.admin`을 사용합니다.
`domain.funding`은 텀블벅 XLSX, 원본 행 보관, 상세 검색과 감사 로그를 확장합니다.
모든 아래 경로의 접두사는 `/api/v1`입니다.

| 기능 | Method / 경로 |
| --- | --- |
| CSV 주문 등록·갱신 | `POST /admin/funding/imports` |
| 주문·이용권 목록 및 텀블벅 원본 정보 | `GET /admin/funding/orders` |
| 펀딩 현황 | `GET /admin/funding/summary` |
| 코드 발급 | `POST /admin/funding/tickets/issue` |
| 전달 기록 | `POST /admin/funding/tickets/{ticketId}/deliveries` |
| 미등록 코드 재발급 | `POST /admin/funding/tickets/{ticketId}/reissue` |
| 주문 환불·이용권 취소 | `POST /admin/funding/refunds` |
| 사용자 코드 확인 | `GET /class-tickets/preview?code=...` |
| 사용자 코드 등록 | `POST /class-tickets/register` |
| 내 이용권 | `GET /class-tickets/me` |
| XLSX 시트·열·샘플 검사 | `POST /admin/funding-orders/tumblbug/inspect` |
| 선택 시트 검토 | `POST /admin/funding-orders/tumblbug/preview` |
| 주문 저장·발급·디지털 선물 XLSX 생성 | `POST /admin/funding-orders/tumblbug/digital-gifts` |
| 텀블벅 전달 결과 기록 | `PATCH /admin/funding-orders/tumblbug/digital-gifts/delivery` |
| 이용권·등록자·등록일 검색 | `GET /admin/tickets` |

## 텀블벅 XLSX

관리자 API는 `ROLE_ADMIN`이 필요합니다. 검사는 multipart `file`, 미리보기와 발급은 `file`과 JSON `options` 파트를 사용합니다.
파일은 XLSX 10MB 이하, 최대 5,000개 데이터 행입니다.

```json
{
  "campaignId": "picke-class-2026",
  "campaignName": "픽케 클래스 펀딩",
  "sheetName": "15인 이용권",
  "rewardLineId": "class-ticket-15",
  "capacity": 15,
  "sourceVersion": "export-20261021"
}
```

`campaignId`는 50자 이하이며 `rewardLineId`를 생략하면 시트명을 사용합니다.
필수 열은 후원번호, 이름, 선물, 추가 후원금, 총 후원 금액, 후원 일시, 알림 신청, 결제 상태, 선물 전달입니다.
미리보기는 저장하지 않습니다. 중복 또는 잘못된 행이 있으면 발급 요청 전체를 거부합니다.

최신 정책에 따라 **수량 1·결제 확정·결제일 존재**인 주문만 발급합니다.
다른 주문은 보관하고 발급을 보류합니다. `codeEligibleRows`에도 이 조건을 적용합니다.
등록기한은 결제일 + 1년, 이용기간은 등록일 + 6개월입니다.
재발급은 기존 이용권을 무효화하고 같은 등록기한으로 새 행을 생성합니다.
코드 형식은 `XXXXXX-XXXXX-XXXXX-XXXXX-XXXXX`이며 SHA-256 해시만 저장합니다.

발급 주문은 최신 `FundingOrder`에 저장하고 `AdminFundingService`로 `ClassTicket`을 발급합니다.
후원번호·리워드 키·금액·알림·원본 전달 상태·원본 행 JSON은 `TumblbugImportRecord`에 보관합니다.
내부 주문번호는 캠페인·후원번호·리워드 키의 SHA-256으로 만든 50자 식별자 `TB-...`입니다.
기존 CSV에 동일 후원번호의 주문이 있는 경우에도 중복으로 취급합니다.

전달 결과 요청 필드는 `campaignId`, `rewardLineId`, `supporterNumbers`, `status`(`SENT`/`FAILED`), `failedReason`입니다.
실패에는 사유가 필요합니다. 공통 이력에는 `TUMBLBUG_DIGITAL_GIFT` 채널 및 `SUCCESS`/`FAILED` 상태로 기록합니다.
이미 등록·취소되었거나 결과를 기록한 코드는 건너뛰고, 재발급 코드는 새 이용권 ID로 구분합니다.
코드 원문은 XLSX 생성 응답에서만 반환하며 실제 텀블벅 업로드는 관리자가 수행합니다.

## 검색과 클래스 연동

주문 검색은 기존 `/admin/funding/orders` 한 곳에서 `keyword`, `capacity`, `paymentStatus`, `deliveryStatus`, `ticketStatus`, `codeStatus`, `page`, `size`를 지원합니다.
기존 주문 응답 필드를 유지하고 `tumblbug`에 원본 금액·알림·후원번호 등을, `deliveryStatus`에 최근 전달 결과를 추가합니다.
이용권 검색은 `keyword`, `capacity`, `status`, `registeredFrom`, `registeredTo`, `page`, `size`를 지원합니다.
이용권 검색 응답의 연락처·코드를 마스킹하며 전체 코드 검색은 해시로 비교합니다.
끝 4자리 검색은 통합 이후 발급·재발급한 코드에서 지원합니다. 기존 해시로 접미사를 복원할 수는 없습니다.

클래스는 기존 `class_tickets`와 기존 이용권 확인 로직을 계속 사용합니다.
스테이징된 `FundingTicketAccess`, `FundingTicketPolicy`는 계약 파일로만 유지합니다.
현재 호출자가 없는 별도 정책 구현은 제공하지 않습니다.

## 추가 스키마

`V20261021_02__tumblbug_import_extensions.sql`을 기존 클래스 스키마 이후 적용합니다.
`funding_campaigns`, `tumblbug_import_records`, `funding_admin_audit_logs`, 코드 접미사,
전달 채널 길이 확장을 포함합니다. 별도 Funding 주문·이용권·코드·전달 테이블은 생성하지 않습니다.
이 저장소에는 Flyway 의존성이 없으므로 기존 DB 반영 절차에서 SQL을 적용해야 합니다.
