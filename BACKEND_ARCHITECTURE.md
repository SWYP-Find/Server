# 픽케 클래스 기능 백엔드 아키텍처 (Funding 구현 범위)

> **2026-10-21 origin/dev 통합 안내**: 현재 API·스키마·발급 정책은 [FUNDING_API.md](FUNDING_API.md)를 기준으로 확인합니다.
> 아래 본문은 기존 PRD 분석과 최초 설계를 보존한 자료이며, `[구현 완료]` 등 당시 표기가 현재 코드와 다를 수 있습니다.
> 공통 주문·이용권·발급·등록·환불은 최신 `domain.ticket`/`domain.admin`으로 통합했습니다.
> 기존 `/tickets/me`, `/ticket-codes/*`, 별도 Funding 이용권 테이블과 `V20261015_01` 설계는 대체되었습니다.
> `FundingTicketAccess`, `FundingTicketPolicy`는 유지하며 기존 클래스 구현은 `ClassTicket`을 계속 참조합니다.

> 분석 기준: 픽케 클래스 기능 PRD v1.2(37쪽), 2026-10-14 현재 UI 폴더의 PNG 12장, 현재 Server 소스  
> 결과물: 화면과 요구사항에 대응하는 Spring Boot 백엔드 설계 및 현재 API 구현 여부  
> 표기: **[존재]** 현재 코드에 경로/기능 확인, **[부분 존재]** 일반 콘텐츠 기능은 있으나 클래스 범위 기능은 없음, **[신규 제안]** 현재 코드에서 확인되지 않음  
> 요구 상태: **확정**은 PRD의 확정 요구, **구현 기본안**은 PRD가 제안한 정책, **확인 필요**는 출시 전 합의 항목

---

## 0. 전체 시스템 요약

### 전체 흐름

~~~text
Mobile App
  ├─ 기존 로그인/JWT
  └─ 이용권 코드 등록 및 이용권 상태 확인 (이번 구현 범위)
       ↓
User REST API (/api/v1/**)
       ↓
Controller → Service → Repository → PostgreSQL

Admin Dashboard (현재 별도 Next.js 앱)
  ├─ 펀딩 구매자·이용권 코드 운영
  └─ 클래스·멤버·결과·변경 이력 조회/내보내기 (이번 구현은 별도 Classroom 범위)
       ↓
Admin REST API (/api/v1/admin/**, ROLE_ADMIN)
       ↓
Admin Controller → 도메인 Service → Repository → PostgreSQL

Funding Domain
  ├─ 구매자/주문 등록·결제 확인
  ├─ 이용권/등록 코드 발급·전달·등록
  └─ Classroom 생성 시 이용권 유효성 조회 계약 제공

Classroom Domain (최소 연동만)
  └─ 클래스 담당자가 소유권을 유지하며 ticket 참조와 funding 유효성 확인만 연결
~~~

### 핵심 도메인

| 도메인 | 역할 | 현재 상태 |
|---|---|---|
| User/Auth | 로그인한 이용권 소유자·운영자 계정 및 JWT | 기존 구현 |
| Funding | 펀딩 주문, 클래스 이용권, 등록 코드, 전달 상태 | 이번 구현 대상 |
| Classroom | 이용권 연결과 생성 시 유효성 확인만 | 다른 개발자 소유, 최소 연동만 |
| Participation | 투표·논쟁·댓글·완료 상태 | 이번 범위 제외 |
| AI | AI 주제·논쟁·리포트 기능 | 구현하지 않음 |
| Admin Operations | Funding 구매자·코드 운영 | 기존 일반 관리자 API 일부만 존재; 클래스 운영은 별도 범위 |

현재 공통 기반에는 BaseEntity, ApiResponse, PageResponse, GlobalExceptionHandler, PostgreSQL, JWT 인증과 ADMIN 권한 경로가 있습니다. 관리자 화면은 별도 Next.js 앱으로 이관된 것으로 소스 주석에서 확인됩니다. 현재 UserRole에는 ADMIN이 있고 SecurityConfig가 /api/v1/admin/**를 ADMIN 전용으로 보호합니다.

일반 배틀·투표·관점·댓글 API는 기존 자산으로 기록합니다. 이번 Funding 구현은 이를 수정하거나 클래스 활동 데이터로 확장하지 않습니다.

### 인증과 계정 경계

- Funding 사용자: 기존 Bearer Access Token으로 이용권 조회·등록을 수행합니다.
- Funding 관리자: 기존 ADMIN 권한으로 구매자/주문 등록과 코드 발급·전달 상태를 관리합니다.
- 클래스 게스트/멤버 인증 및 클래스 운영자 권한은 별도 Classroom 구현 범위입니다. 이번에는 해당 인증 구조를 추가하지 않습니다.
- 텀블벅 결제는 앱에서 처리하지 않습니다. 펀딩 결제 확인과 리워드 코드는 관리자 운영 흐름으로 처리합니다.

---

## 1. Spring Boot 패키지 구조

현재 프로젝트의 domain별 패키지 구조와 Controller / Service / Repository / Entity / DTO 분리를 따릅니다. 이번 문서의 구현 범위는 Funding 도메인과 Classroom 최소 연동 계약입니다.

펀딩 주문에서 클래스 이용권(`Ticket`)과 등록 코드(`TicketCode`)를 발급하므로 주 도메인 패키지는 `domain.funding`로 둡니다. 클래스 쪽에는 Funding 연동 계약만 `domain.classroom`에 둡니다. API와 DB도 `/tickets`, `/ticket-codes`, `funding_tickets`, `funding_ticket_codes`로 명칭을 맞춥니다.

~~~text
domain/
 ├─ funding/                    # 이번 구현 대상: 주문, 이용권, 코드, 전달 이력
 │   ├─ controller/
 │   ├─ service/
 │   ├─ repository/
 │   ├─ entity/
 │   ├─ dto/request/
 │   ├─ dto/response/
 │   └─ enums/
 │
 ├─ classroom/                  # 별도 개발 영역; Funding 유효성 계약만 추가
 │   ├─ FundingTicketPolicy.java
 │   └─ FundingTicketAccess.java
 │
 ├─ user/                        # 기존 User, 프로필, 사용자 기록
 ├─ oauth/                       # 기존 로그인/JWT
 ├─ battle/                      # 기존 배틀 콘텐츠, 클래스에서는 읽기 전용 참조
 ├─ vote/                        # 기존 일반 배틀 투표, 클래스 투표는 분리
 ├─ perspective/                 # 기존 일반 관점/댓글, 클래스 댓글은 분리
 └─ admin/                       # 기존 사용자·대시보드 등 일반 관리자 API

global/
 ├─ common/BaseEntity.java        # 기존 공통 필드 사용
 ├─ common/response/              # 기존 ApiResponse, PageResponse 사용
 ├─ common/exception/              # 공통 예외 구조 재사용
 └─ config/SecurityConfig.java     # /api/v1/admin/** 기존 ADMIN 보호 규칙 사용
~~~

### 다른 개발자와의 코드 소유 경계

- Funding 구현은 `domain/funding/**`의 주문·이용권·등록 코드 파일을 소유합니다. 기존 사용자/auth와 battle 등 타 도메인 파일은 필요한 변경 없이 조회/참조만 합니다.
- Classroom 담당자는 클래스·멤버십·활동·관리자 화면을 소유합니다. 이번 변경은 `domain/classroom`에 Funding 정책 interface와 반환 record만 둡니다. Classroom Entity/Controller/Repository는 구현하지 않습니다.
- Classroom의 최소 연동은 `Classroom.ticketId` 참조(기존 필드가 없다면 FK만 추가)와 클래스 생성 시 Funding의 `FundingTicketPolicy.requireUsableBy(userId, ticketId)` 호출입니다. Funding은 소유자·ACTIVE 상태·만료 시각을 검증하고 `capacity`, `expiresAt`을 포함한 얇은 `FundingTicketAccess` 값을 반환합니다. 같은 Spring 애플리케이션 내부 서비스 계약이며 별도 HTTP API를 추가하지 않습니다.
- AI 도메인/기능은 이번 구현에서 제외합니다. AI Controller, Entity, Repository, Client, Worker, API를 만들거나 변경하지 않습니다.
- 공통 `ErrorCode` 추가가 필요하면 Funding 접두어 오류만 통합 담당자가 반영합니다. SecurityConfig의 `/api/v1/admin/**` ADMIN 규칙은 기존 것을 사용합니다.

### 이번 코드 반영 범위

- `funding`: FundingCampaign, FundingOrder, Ticket, TicketCode, DeliveryAttempt, AdminAuditLog Entity와 Repository, Service/ServiceImpl, 설명이 포함된 Enum, record DTO, 사용자/관리자 Controller를 구현합니다.
- `classroom`: Funding ticket 검사 interface와 `FundingTicketAccess` record만 추가합니다. 클래스 기능 Entity/API는 다른 개발자 작업 영역으로 남깁니다.
- 구매자 연락처는 `FUNDING_CONTACT_ENCRYPTION_KEY` 환경 변수(Base64 인코딩 32바이트)로 AES-256-GCM 암호화합니다. 관리자 목록은 마스킹 연락처만 반환하고 검색은 연락처 전체 값의 HMAC 일치 검색을 지원합니다.
- Funding 테이블 DDL은 `src/main/resources/db/migration/V20261015_01__create_fundings_domain.sql`에 추가합니다. 프로젝트의 기존 DB 반영 절차에서 적용해야 합니다.
- 운영자는 텀블벅 명단 Excel을 직접 내려받고 서버에 업로드합니다. 서버는 선택 시트에서 화면의 9개 열을 자동 판별하고 미리보기로 확인합니다. 확정 시 유효한 후원 행 전체를 저장하고 결제 완료 행에만 코드를 발급해 텀블벅 업로드용 Excel을 만듭니다. 텀블벅 업로드와 완료 확인은 관리자가 수행합니다.

이 경계에서는 Funding이 구현 대상이고 Classroom은 이용권 참조와 생성 전 검증만 연동합니다. Classroom 파일을 수정해야 한다면 `ticketId`와 단일 유효성 호출만 클래스 담당자가 반영합니다. AI 기능은 구현하지 않습니다.

---

## 2. 화면별 설계

UI 폴더에는 총 12장의 화면 이미지가 있습니다. 앱 화면은 8개 이미지/상태 묶음, 관리자 화면은 4개 화면으로 분석했습니다. PRD의 S01~S22 클래스 참여·생성 화면은 이 폴더에 시각 시안이 없으므로, 아래에서 **PRD 전용 화면 흐름**으로 명시합니다.

이 절은 전체 자료에서 확인한 화면과 데이터 요구를 보존합니다. 다만 이번 구현은 Funding 화면/기능에 한정합니다. Classroom 화면 중 이용권 유효성 연결 외 기능과 AI 관련 화면·기능은 각각 다른 개발 범위이며, 여기서 구현하지 않습니다.

### 사용자 앱

#### 1) 마이페이지의 클래스 이용권 진입 화면
이미지: 2026-10-14 232458

- 목적: 사용자가 클래스 이용권 메뉴로 이동합니다.
- 액션: 클래스 이용권 메뉴 탭.
- 데이터: 기존 사용자 프로필은 기존 마이페이지 API에서 받고, 이용권 존재/상태 표시는 별도 이용권 API에서 받습니다.
- 프론트 처리: 메뉴명·아이콘·고정 문구.
- 백엔드 처리: 이용권 상태와 표시할 정원·기간.
- API: [존재] GET /api/v1/me/mypage, [구현 완료] GET /api/v1/tickets/me.
- 인증: 로그인 필요.
- 예외/이동: 미보유면 코드 등록 안내, 보유면 이용권 상세. 로딩·네트워크 오류는 앱 상태.
- 논의: 기존 mypage 응답에 이용권 필드를 합칠지 별도 endpoint로 조회할지. 본 설계는 별도 endpoint를 권장해 User 도메인 변경을 피합니다.

#### 2) 이용권 등록 코드 입력 화면
이미지: 2026-10-14 232009, 232203

- 목적: 펀딩 리워드로 받은 이용권 등록 코드를 입력합니다.
- 액션: 코드 입력, 코드 확인, FAQ 열기.
- 표시 데이터: 코드 입력값은 앱 상태. 이용권 정원과 등록기한은 검증 후 백엔드 응답.
- 프론트 처리: 라벨·고정 안내·입력 형식·버튼 활성 조건.
- 백엔드 처리: 코드 존재/상태/등록기한/다른 코드 유형 여부.
- API: [구현 완료] POST /api/v1/ticket-codes/preview.
- 인증: 로그인 필요, 미리보기는 코드를 소비하지 않습니다.
- 예외/이동: 잘못된 코드면 입력을 보존하고 오류 안내, 유효하면 등록 확인 화면.
- 논의: 코드 입력의 하이픈·공백 정규화 및 복사/붙여넣기 동작.

#### 3) 이용권 FAQ 펼침 화면
이미지: 2026-10-14 232243

- 목적: 코드를 어디서 받는지와 클래스 참여 코드와의 차이를 안내합니다.
- 표시 데이터: “펀딩 리워드 안내에서 확인”, “참여 코드는 모임 입장에 사용하는 별도 코드” 문구.
- 프론트 처리 권장: FAQ 문구는 고정 안내이므로 App에서 관리합니다.
- 백엔드: 불필요.
- 논의: 운영 중 환불/전달 경로가 바뀌면 문구 배포를 누가 담당할지 정합니다.

#### 4) 코드 확인 결과 및 오류 모달
이미지: 2026-10-14 232144

- 목적: 이미 등록된 이용권, 잘못된 코드, 네트워크 문제, 만료 코드를 구분합니다.
- 백엔드 구분: CODE_INVALID, CODE_ALREADY_CLAIMED, CODE_EXPIRED, CODE_REVOKED, NETWORK_ERROR.
- 프론트 처리: 오류 아이콘, 재시도 버튼, 입력 보존, 확인/뒤로가기.
- 인증: 코드 미리보기는 로그인 필요.
- 예외 문구: 코드 오류는 다음 문구로 통일합니다. “코드가 올바르지 않아요. 입력한 코드를 다시 확인해 주세요.”
- 주의: 네트워크 실패는 서버 ErrorCode로 가장하지 않고 앱 네트워크 상태로 처리합니다.

#### 5) 이용권 등록 확인 화면
이미지: 2026-10-14 232220

- 목적: 상품 정원, 기간, 등록 시 시작되는 이용 조건을 확인한 뒤 사용자가 등록을 확정합니다.
- 백엔드 데이터: 상품명/정원, 등록기한, 등록 즉시 시작될 활성화·종료 시각, 클래스 생성 제한 없음.
- 프론트 처리: 확인 문구·버튼·등록 후 기간이 되돌릴 수 없다는 설명.
- API: [구현 완료] POST /api/v1/ticket-codes/claim.
- 인증: 로그인 필요. Idempotency-Key 헤더 권장.
- 이동: 성공 시 등록 완료, 중복 탭은 첫 결과 반환.
- PRD/UI 차이: UI 예시는 30인 상품 하나지만 PRD는 5/15/30인 3종을 정의합니다.

#### 6) 이용권 등록 완료 화면
이미지: 2026-10-14 232308

- 목적: 활성화 결과와 공통 이용 종료일을 보여줍니다.
- 데이터: 정원, activatedAt, expiresAt, 이용권 상태.
- 프론트: “이용권이 등록됐어요” 등 고정 문구와 버튼.
- 백엔드: 날짜 계산과 이용권 활성 상태.
- 이동: 이용권 확인하기 또는 마이페이지.
- 예외: 재요청 시 종료일을 다시 계산하거나 연장하지 않습니다.

#### 7) 클래스 이용권 확인 화면
이미지: 2026-10-14 232329

- 목적: 현재 활성 이용권의 상품·기간·정원을 확인하고 추가 등록 흐름을 엽니다.
- API: [구현 완료] GET /api/v1/tickets/me.
- 인증: 로그인 필요.
- 표시 데이터: 상태, 정원, 사용 가능 기간, 클래스 생성 가능 여부.
- PRD/UI 차이: PRD의 기본안은 펀딩 캠페인 기준 1인 1개지만 UI에는 “이용권 추가등록하기”가 있습니다. 추가 캠페인/복수 보유 의미인지 출시 전 확정이 필요합니다. UI가 보유하지 않은 등록 코드를 확인해 주는지 역시 현재 계약에 없습니다.

#### 8) 앱 진입 및 이용권 상태별 분기
이미지: 2026-10-14 232458 및 이용권 화면 3종

- 메뉴는 서버 상태를 바탕으로 등록 안내/활성 이용권/기간 종료 상태로 분기합니다.
- “현재 이용권 없음”은 서버에서 NONE으로 내려주며, Loading/Network Error는 앱이 처리합니다.
- UI에 미등록·이용 중·등록만료·이용종료가 표시되지만, PRD의 구매자 정보는 코드 등록 전 앱 계정과 연결되지 않습니다. 등록 전 이용권 상태를 특정 사용자의 P03 화면에서 보여주는 방법은 정책 확인이 필요합니다.

### 관리자 대시보드

현재 관리자 UI는 별도 Next.js 앱을 사용합니다. 아래는 첨부 이미지에서 확인된 클래스 관리 화면 4종입니다.

#### 1) 클래스 관리 목록
이미지: 2026-10-14 233753

- 목적: 전체 클래스와 정원·인원·선택 콘텐츠·운영 상태를 검색합니다.
- 데이터: 전체/진행 중/멤버 수/만료 임박 집계 카드, 클래스명·생성자·이용권·인원·선택 콘텐츠·상태.
- 검색/필터: 클래스명 또는 생성자, 상태, 이용권 정원, 배틀 유형.
- 정렬/페이지: 페이지 번호 UI 확인, 정렬 조건은 시안에 없음. 서버 pagination을 권장합니다.
- 액션: 조회, CSV 내보내기, 행을 눌러 상세.
- API: [별도 Classroom 구현 범위] GET /api/v1/admin/classes, GET /api/v1/admin/classes/export.csv. 이번 Funding 구현에서는 추가하지 않습니다.
- 권한: ADMIN.
- 논의: 필터 변경 시 서버 조회, 기본 정렬(createdAt desc), CSV 내보내기 적용 필터.

#### 2) 이용권·등록자 목록
이미지: 2026-10-14 233819

- 목적: 발급된 코드, 상품 정원, 등록 사용자, 이용 기간, 연결 클래스 수를 관리합니다.
- 데이터: 전체 이용권/이용 중/미등록/만료 임박 카드, 코드, 구매자 정보, 이용권·등록자, 구분, 기간, 생성 클래스 수.
- 검색/필터: 코드·구매자·등록자 키워드, 상품 정원(5/15/30), 상태, 등록 기간.
- 액션: 이용권 코드 발급, 조회, 페이지 이동.
- API: [구현 완료] GET /api/v1/admin/tickets, POST /api/v1/admin/tickets/{ticketId}/codes.
- 권한: ADMIN.
- 개인정보: 이메일 등은 기본 마스킹. 발급 완료 직후 코드 원문은 제한된 일회 노출로 전달하는 안을 권장합니다.
- 논의: UI 표에 표시된 전체 코드 노출과 코드 원문 비저장 원칙 간 조정.

#### 3) 펀딩 구매자 목록
이미지: 2026-10-14 233945

- 목적: 외부 구매자, 리워드, 코드 발급·전달·앱 등록 상태를 운영합니다.
- 데이터: 구매자 수/코드 발급/전달 완료/미전달, 구매자, 연락처, 펀딩 주문, 구매 상품, 전달 상태, 등록 상태.
- 검색/필터: 이름·이메일·전화번호, 상품 정원, 전달 상태, 등록 상태.
- 액션: 구매자 등록, 조회, 전달 결과 갱신.
- API: [구현 완료] GET /api/v1/admin/funding-orders, POST /api/v1/admin/funding-orders, 텀블벅 명단 시트 확인/미리보기/코드 파일 생성, PATCH 전달 결과.
- 권한: ADMIN.
- 개인정보: 구매자 연락처는 암호화 저장·응답 마스킹, 열람은 관리자 권한 및 감사 로그 대상.
- UI 차이: 첨부 이미지는 개별 등록 버튼과 목록만 보여줍니다. 엑셀 매핑·미리보기·디지털 선물 파일 다운로드 화면은 별도 관리자 프론트엔드 구현이 필요합니다.

#### 4) 클래스 상세와 탭
이미지: 2026-10-14 234004

- 목적: 선택한 클래스의 이용권 연결, 운영 설정, 참여 현황, 결과와 변경 이력을 봅니다.
- 기본 정보: 클래스 ID/생성자/생성 시각, 이용권 연결과 유효기간, 선택 콘텐츠, 정원/현재 인원, 참여 마감, 댓글 필수, 운영자 참여.
- 탭: 기본 정보, 멤버·참여, 결과·AI, 변경 이력.
- 액션: CSV 내보내기, 목록 복귀. 시안에서 편집/삭제 버튼은 확인되지 않습니다.
- API: [별도 Classroom 구현 범위] GET /api/v1/admin/classes/{id}, GET /members, GET /results, GET /history, CSV export. 이번 Funding 구현에서는 추가하지 않습니다.
- 권한: ADMIN.
- 논의: 관리자 수정/삭제 UI는 이미지에 없어 기본 기능으로 추가하지 않습니다. 운영상 필요하면 별도 티켓으로 정합니다.

### PRD에서만 확인되는 앱 화면 흐름 (첨부 UI 이미지 없음)

| PRD 화면 ID | 정의된 화면/흐름 | 백엔드 핵심 |
|---|---|---|
| S01 | 클래스 홈 | 이용권/내 클래스 요약, 생성·코드 입장 진입 |
| S02~03 | 방식 선택·주제 조건 | LIBRARY/CUSTOM, 키워드·수준·카테고리 |
| S04 | 기존 콘텐츠 목록 | 배틀 카탈로그 검색, 미리듣기 정보 |
| S05 | AI 주제 제안 | 후보 8개 기본 계약, 직접 수정 검증 |
| S06~07 | 클래스 설정·공유 완료 | 생성, 정원·마감·옵션, 참여 코드 |
| S08~09 | 코드 입장·이름 입력 | 코드 미리검증, 정원 경쟁 제어, 게스트 세션 |
| S10~11 | 내 클래스·상세 | 진행/종료 목록, 구성원 권한별 상세 |
| S12 | 멤버 목록·이름 수정 | 명단, 본인 이름 변경, 운영자 내보내기 |
| S13 | 클래스 관리 | 운영자 수정/공유/삭제, 참가자 공유/나가기 |
| S14 | 클래스 요약 | 운영자 통계와 공개 의견, AI 인사이트 |
| S15 | 내 리포트 | 참여자 본인 리포트만 조회 |
| S16~20 | 기존 콘텐츠 배틀 | 클래스 격리 투표·댓글·완료 |
| S21~22 | 커스텀 AI 논쟁 | 3라운드, AI Job, 본인 반박 저장 |

기본 정책은 PRD의 권한/역할과 데이터 범위를 따릅니다. 별도 관리자 웹 UI가 없는 S01~S22 흐름을 관리자 화면으로 가정하지 않습니다.

---

## 3. 필요한 백엔드 구성요소

| 구성요소 | 책임 |
|---|---|
| FundingAccessController | 내 이용권 조회, 코드 미리 확인·등록 |
| FundingTicketService | 코드 상태/기한/소유자 검증, 원자적 활성화 |
| FundingAdminController | 구매자·주문 수동 등록, 코드 발급, 전달 상태 관리 |
| FundingAdminService | PAID 확인, 상품 정원 매핑, 재발급·환불 상태 처리, 감사 기록 |
| FundingTicketPolicy | 클래스 담당자가 클래스 생성 전에 호출하는 내부 서비스 계약. 소유자·ACTIVE·기간 검증 후 정원/만료 정보를 반환 |
| Funding repositories/entities | 캠페인·주문·이용권·코드·전달 이력 영속화와 동시성 제어 |
| Existing User/Auth | 기존 사용자와 JWT 권한 재사용; User 도메인은 Funding 목적으로 확장하지 않음 |
| Classroom domain | 별도 담당자 소유. 이번 범위에서 클래스 기능 구현 없이 위 정책 호출 및 ticketId 연결만 통합 |

Funding 사용자 Controller와 Admin Controller는 인증·응답 DTO를 분리하고, 업무 불변식은 Funding Service에서 수행합니다. 관리자 액션은 같은 핵심 Service의 상태 변경 메서드를 호출하되, 일반 사용자가 임의의 userId를 전달해 실행하지 못하게 합니다. Classroom/AI Controller와 Service는 이번 구현 대상이 아닙니다.

---

## 4. API 명세

### 공통 계약

프로젝트에는 공통 응답이 이미 있습니다.

~~~json
{
  "statusCode": 200,
  "data": {},
  "error": null
}
~~~

- 성공 래퍼: 기존 ApiResponse를 사용합니다. 현재 onSuccess는 statusCode 200으로 생성합니다. 새 API의 HTTP status와 body statusCode를 일치시키려면 작은 overload가 필요합니다.
- 실패 래퍼: 기존 형식은 {statusCode, data:null, error:{code,message}}입니다. HTTP status는 의미를 표현하고 ErrorCode는 화면 분기용 안정 키로 씁니다.
- 목록: 기존 PageResponse를 사용합니다. 요청 page는 0부터, 응답 page는 현재 구현처럼 1부터 맞추거나 기존 AdminUserSearchResponse처럼 hasNext 계약을 명시합니다. 이 기능에서는 PageResponse로 통일을 권장합니다.
- 인증: 로그인 사용자 API는 Authorization: Bearer {accessToken}. Refresh는 현재 X-Refresh-Token을 사용합니다. 게스트 API는 별도 signed guest token을 사용합니다.
- 쓰기 중복 방지: Funding 코드 claim과 구매자/주문 등록은 Idempotency-Key 또는 Funding 고유 키로 중복 처리를 보장합니다. Classroom 쓰기와 AI turn은 해당 담당 범위입니다.

### 기존 API 및 Funding 사용자 API

| 상태 | Method / URL | 목적 및 입력 | 응답 | Auth | HTTP / 주요 오류 |
|---|---|---|---|---|---|
| [존재] | POST /api/v1/auth/login/{provider} | 기존 소셜 로그인 | 기존 LoginResponse | 없음 | 200, AUTH_* |
| [존재] | POST /api/v1/auth/refresh | X-Refresh-Token 갱신 | 기존 LoginResponse | Refresh Token | 200, AUTH_REFRESH_TOKEN_EXPIRED |
| [존재] | POST /api/v1/auth/logout | 로그아웃 | {success:true} | USER | 200 |
| [존재] | GET /api/v1/me/mypage | 기존 마이페이지 | 기존 MypageResponse | USER | 200 |
| [부분 존재] | GET /api/v1/battles?page=1&size=10 | 공개 배틀 카탈로그 기본 목록 | 기존 BattleListResponse | 기존 정책 | 200 |
| [부분 존재] | GET /api/v1/battles/{battleId} | 배틀 상세/콘텐츠 조회 | 기존 상세 응답 | 기존 정책 | 200, BATTLE_NOT_FOUND |
| [구현 완료] | GET /api/v1/tickets/me | 내 이용권 상태 조회 | NONE/ACTIVE/EXPIRED/REVOKED와 기간 | USER | 200, 401 |
| [구현 완료] | POST /api/v1/ticket-codes/preview | {code}; 코드를 소비하지 않는 검증 | 상품·정원·registerBy·등록 시 종료 예정일 | USER | 200, 422 TICKET_CODE_INVALID, 410 TICKET_CODE_EXPIRED, 409 TICKET_CODE_UNAVAILABLE |
| [구현 완료] | POST /api/v1/ticket-codes/claim | {code}; 같은 코드의 같은 사용자 재요청은 최초 결과 반환 | ticketId·capacity·activatedAt·expiresAt | USER | 200, 409 TICKET_ALREADY_ACTIVE, 409 TICKET_CODE_UNAVAILABLE |

Classroom 생성 시 내부 연동 계약은 `FundingTicketPolicy.requireUsableBy(userId, ticketId)`이며, `capacity`와 `expiresAt`을 반환합니다. 별도 HTTP endpoint는 추가하지 않습니다. 클래스 생성·입장·활동 및 AI API는 다른 구현 범위이며 이번 Funding API 목록에는 포함하지 않습니다. 기존 battle API는 그대로 둡니다.

### 관리자 API

| 상태 | Method / URL | 입력/조회 | 응답 | Auth | HTTP / 주요 오류 |
|---|---|---|---|---|---|
| [존재] | GET /api/v1/admin/users/search?keyword=&page=0&size=20 | 기존 앱 사용자 검색 | items·hasNext | ADMIN | 200 |
| [존재] | GET /api/v1/admin/dashboard/summary | 기존 일반 대시보드 요약 | 기존 통계 DTO | ADMIN | 200 |
| [구현 완료] | GET /api/v1/admin/tickets?keyword=&capacity=&status=&registeredFrom=&registeredTo=&page=0&size=20 | 코드/등록자 목록 | PageResponse<TicketAdminRowResponse> | ADMIN | 200 |
| [구현 완료] | POST /api/v1/admin/funding-orders | 단건 구매자·주문·상품·결제확인 수동 등록 | fundingOrderId, ticketIds | ADMIN | 201, 409 FUNDING_ORDER_DUPLICATE |
| [구현 완료] | POST /api/v1/admin/funding-orders/tumblbug/inspect | 후원자 XLSX의 전체 시트에서 헤더 자동 판별 및 샘플 행 확인 | 시트별 인식/누락 열, 샘플 행 | ADMIN | 200, 400 형식 오류, 413 10MB 초과 |
| [구현 완료] | POST /api/v1/admin/funding-orders/tumblbug/preview | 선택 시트에서 9개 열 자동 판별, 결제·중복·수량 검토 | 행별 저장/코드 발급 가능 여부 | ADMIN | 200, 400 옵션 오류, 422 필수 열 누락 |
| [구현 완료] | POST /api/v1/admin/funding-orders/tumblbug/digital-gifts | 유효한 행 저장, 결제 완료 행에 코드 발급, 업로드용 XLSX 반환 (최대 5,000행) | 후원번호와 코드 텍스트 파일 | ADMIN | 200, 409 중복, 413 파일/행 제한, 422 필수 열/행 오류 |
| [구현 완료] | PATCH /api/v1/admin/funding-orders/tumblbug/digital-gifts/delivery | 텀블벅 업로드 결과를 후원번호 목록으로 기록 | 반영/미매칭 건수 | ADMIN | 200, 400 상태 오류 |
| [구현 완료] | GET /api/v1/admin/funding-orders?keyword=&capacity=&deliveryStatus=&claimStatus=&page=0&size=20 | 구매자 목록 | 마스킹 연락처·상품·전달/등록 상태 | ADMIN | 200 |
| [구현 완료] | POST /api/v1/admin/tickets/{ticketId}/codes | 등록 코드 발급 | 1회 노출 code·발급 상태 | ADMIN | 201, 409 TICKET_CODE_ALREADY_ISSUED |
| [구현 완료] | PATCH /api/v1/admin/deliveries/{id} | {channel,status,failedReason} 전달 결과 기록 | 전달 상태·시각 | ADMIN | 200 |
| [구현 완료] | POST /api/v1/admin/tickets/{ticketId}/reissue | 미등록 코드 폐기 후 재발급 | 새 코드·기존 registerBy | ADMIN | 201, 409 FUNDING_REISSUE_NOT_ALLOWED |
| [구현 완료] | POST /api/v1/admin/tickets/{ticketId}/refund | 실제 외부 환불을 확인한 후 주문·연결 이용권 취소 | FundingOrder REFUNDED, Ticket/TicketCode REVOKED | ADMIN | 200, 409 FUNDING_REFUND_NOT_CONFIRMED |

### 주요 JSON 예시

코드 미리보기:

~~~json
{
  "code": "PICKE-3E8T-U3AB-C5D6-F7GH-JKLM-NPQR-STUV"
}
~~~

~~~json
{
  "statusCode": 200,
  "data": {
    "codeStatus": "VALID",
    "capacity": 30,
    "registerBy": "2027-10-10T05:00:00Z",
    "activatedAtIfClaimed": "2026-10-15T03:00:00Z",
    "expiresAtIfClaimed": "2027-04-15T03:00:00Z"
  }
}
~~~

코드 등록:

~~~json
{
  "statusCode": 200,
  "data": {
    "ticketId": 4102,
    "status": "ACTIVE",
    "capacity": 30,
    "activatedAt": "2026-10-15T03:00:00Z",
    "expiresAt": "2027-04-15T03:00:00Z"
  }
}
~~~

클래스 생성 API request/response는 이번 Funding 설계에서 정의하지 않습니다. Classroom 담당자가 위 내부 `FundingTicketPolicy` 계약과 `ticketId`만 연결합니다.

공통 오류 예시:

~~~json
{
  "statusCode": 422,
  "data": null,
  "error": {
    "code": "TICKET_CODE_INVALID",
    "message": "코드가 올바르지 않아요. 입력한 코드를 다시 확인해 주세요."
  }
}
~~~

권장: ErrorCode만 전달하고 앱이 표시 문구를 매핑합니다. 기존 GlobalExceptionHandler는 ErrorCode의 message도 응답하므로, 위 문구를 서버에서 제공할지 앱 상수로 관리할지는 앱 개발자와 합의합니다.

---

## 5. DB / Entity 설계

모든 주요 Entity는 기존 BaseEntity를 상속해 id, createdAt, updatedAt을 재사용합니다. 이 필드들을 도메인별로 다시 정의하지 않습니다. 현재 데이터베이스는 PostgreSQL입니다. 현재 application.yml은 hibernate ddl-auto: update를 설정하고 SQL migration 파일도 저장소에 있으므로, 실제 배포용 스키마 변경 절차는 개발자가 기존 배포 규칙을 확인한 후 한 방식으로 적용해야 합니다.

| Entity | 핵심 필드 | 관계/제약 |
|---|---|---|
| User (기존) | 기존 userTag, role, status | 기존 엔티티 수정 최소화. owner/claim 연결 FK |
| FundingCampaign | platform, externalCampaignId, name, status, registerPolicyVersion | platform+externalCampaignId UNIQUE |
| FundingOrder | campaign, externalOrderId, rewardLineId, rewardType, quantity, capacity, paymentStatus, paidAt, buyerName, buyerContactEncrypted, buyerContactSearchHash, sourceVersion | campaign+externalOrder+rewardLine UNIQUE; paidAt 필수 발급 조건 |
| Ticket | fundingOrder, ownerUser, capacity, registerBy, activatedAt, expiresAt, status | fundingOrder+unitIndex UNIQUE; ownerUser nullable until claim; 한 사용자 활성/미등록 권한 제한 |
| TicketCode | ticket, codeHash, status, issuedAt, deliveredAt, claimedAt, revokedAt | codeHash UNIQUE; 원문 코드 저장 금지 |
| DeliveryAttempt | ticketCode, channel, status, maskedDestination, failedReason, attemptedAt, completedAt | 현재는 코드 발급 시 전달 대기 행을 만들고 관리자가 결과를 기록; 코드 원문/전체 연락처 로그 금지 |
| Classroom (별도 담당) | ticketId | Funding 연동에 필요한 비고유 FK만 합의·연결. 클래스 Entity/기능 구현은 이번 범위 아님 |
| AdminAuditLog | adminUser, action, targetType, targetId, reason, metadata | 개인정보 최소화, 관리자 상세 이력에 사용 |

### 관계 요약

~~~text
FundingCampaign 1 ── N FundingOrder 1 ── N Ticket
                                             ├── 0..1 User (claim 전에는 미연결)
                                             ├── N TicketCode
                                             └── 0..N Classroom (별도 Classroom 담당이 ticketId로 참조)
~~~

Membership, Participation, TopicVersion, ClassVote, Report, AIJob 등은 이번 Funding 범위에서 Entity/테이블로 추가하지 않습니다.

### Enum 후보

- FundingPaymentStatus: RESERVED, PAYMENT_PENDING, PAID_CONFIRMED, PAYMENT_FAILED, CANCELED, REFUNDED
- TicketStatus: UNREGISTERED, ACTIVE, EXPIRED, REGISTER_EXPIRED, REVOKED
- TicketCodeStatus: ISSUED, DELIVERED, CLAIMED, REVOKED

### 수동 운영 기준 엔티티 검토

운영자가 명단 Excel을 내려받아 서버에서 미리보기·검토한 뒤 발급을 확정하는 MVP에는 현재 여섯 엔티티만으로 핵심 업무를 표현할 수 있습니다. Excel 수집 배치용 Entity는 이번 범위에 필요하지 않습니다. 다만 운영 전 다음 정의를 확정해야 합니다.

- 텀블벅 가져오기는 `externalSupporterNumber`와 `rewardLineId` 조합을 후원자·리워드의 중복 방지 키로 사용하고, 일반 수동 등록은 `externalOrderId`와 `rewardLineId`를 사용합니다. 실제 텀블벅 Excel 샘플로 자동 판별 별칭을 검증해야 합니다. 현재 환불은 해당 Ticket이 속한 FundingOrder의 Ticket 전체를 취소하며, 같은 외부 후원번호의 다른 rewardLine까지 취소하지는 않습니다.
- `FundingCampaign.registerPolicyVersion`은 현재 발급 정책 선택에 사용되지 않습니다. 정책 버전을 보존하지 않을 계획이면 제거하고, 유지한다면 버전별로 고정할 정책 값을 정의합니다. 캠페인 상태를 변경하는 운영 경로도 필요한지 결정합니다.
- 현재 `DeliveryAttempt`는 코드 발급 시 전달 채널(`MANUAL` 또는 `TUMBLBUG_DIGITAL_GIFT`)과 `PENDING`으로 생성되고 관리자가 결과를 한 번 기록합니다. 실제 발송 시도·재시도 이력은 아닙니다. 수동 MVP에서는 전달 결과 기록으로 사용하고, 재시도마다 이력이 필요해질 때 시도 단위로 확장합니다.
- `AdminAuditLog`는 주문 등록·코드 발급·전달 결과·환불 변경을 기록합니다. 구매자 정보 조회·내보내기도 감사 대상으로 포함할지 정하고, `metadata`에는 연락처 원문이나 코드 원문을 저장하지 않습니다.

이 항목들은 추가 Excel 배치 Entity가 필요하다는 뜻이 아니라, 수동 운영에서도 외부 ID 매핑·환불 범위·정책·전달 기록·감사 범위를 확정해야 한다는 뜻입니다.

### 제약과 JPA 주의점

- UNIQUE: 외부 주문 라인, 후원번호·리워드 라인, TicketCode.codeHash. Classroom.ticketId에는 UNIQUE를 두지 않아 하나의 이용권을 여러 클래스에서 사용할 수 있습니다.
- INDEX: Funding 상태/생성일, ticket owner/status, funding order paidAt/status. 검색 키워드와 마스킹 연락처는 원문 전체 인덱스를 피하고 필요한 검색 해시/정규화 키를 별도 검토합니다.
- Nullable: Ticket.ownerUser는 코드 등록 전 null입니다. Classroom FK의 필수 여부와 삭제/환불 시 동작은 Classroom 담당자와 합의합니다.
- Cascade: User 삭제에 funding 기록을 광범위 Cascade하지 않습니다. 서비스에서 비공개 처리·파기 큐를 제어합니다.
- Fetch: 모든 연관은 기본 LAZY. 목록 API는 projection/DTO query를 사용하고 Entity를 직접 JSON 직렬화하지 않습니다.
- 이용권 코드는 `PICKE-` 뒤에 O 제외 A-Z와 1-9로 만든 난수 28자를 4자씩 고정 그룹으로 표시합니다(약 142비트). 서버에는 SHA-256 해시만 저장하고, 구 PK 형식도 기존 코드 호환을 위해 등록을 허용합니다. Classroom 참여 코드 설계는 별도 Classroom 담당 범위입니다.
- code 원문은 관리자 발급 직후 한 번만 반환합니다. 분실 시 기존 미등록 코드를 폐기하고 같은 이용권·등록기한으로 재발급합니다.

---

## 6. 비즈니스 로직

### 펀딩 주문에서 이용권 발급

1. ADMIN이 구매자/주문/상품/결제 완료 상태를 수동 등록합니다. 모집 종료나 목표 달성만으로 결제완료 처리하지 않습니다.
2. 플랫폼·캠페인·주문·리워드 행 고유 키로 중복 주문을 확인합니다.
3. 리워드 ID를 5/15/30인 capacity로 매핑합니다. 불명확한 리워드는 보류합니다.
4. 확인된 결제완료 행에만 Ticket와 TicketCode를 발급합니다.
5. registerBy는 최종 결제 확인일 + 달력 기준 1년입니다.
6. 전달 상태는 발급과 분리하여 관리자 액션/전달 시도 기록으로 관리합니다.

### 코드 미리보기/등록

1. 로그인과 입력 형식을 확인하고 코드 정규화 후 codeHash로 조회합니다.
2. Preview는 등록기한, 결제 상태, 코드 상태, 소유 상태만 검사하고 코드를 소비하지 않습니다.
3. Claim 트랜잭션은 TicketCode 행을 PESSIMISTIC_WRITE로 잠급니다.
4. User 행도 잠그고 보유 제한·기존 동일 요청을 확인합니다.
5. 조건 통과 시 Ticket.ownerUser를 현재 사용자로 연결하고 activatedAt/ expiresAt을 저장합니다. expiresAt은 activatedAt + 달력 기준 6개월입니다.
6. TicketCode는 CLAIMED로 전이합니다. code·소유자·활성·종료 시각은 한 트랜잭션으로 커밋합니다.
7. 같은 사용자의 재시도는 기존 결과 반환. 타 계정 경쟁은 한 요청만 성공.
8. 환불 확정은 Funding에서 REVOKED 처리합니다. Classroom 데이터 변경이나 활동 차단 구현은 별도 소유 범위이며, 클래스 생성 시 Funding 유효성 확인으로 신규 연결을 막습니다. 오래된 결제 업로드가 환불을 되돌리지 않도록 상태 전이 우선순위를 둡니다.

### Classroom 최소 연동

1. Funding은 코드 등록 후 `Ticket.ownerUser`, `capacity`, `activatedAt`, `expiresAt`, `status`를 보유합니다.
2. Classroom 담당자는 클래스 생성 시 같은 애플리케이션의 `FundingTicketPolicy.requireUsableBy(userId, ticketId)`를 호출합니다.
3. 정책은 소유자 일치, `ACTIVE`, 현재 시각이 `expiresAt` 이전인지 검사하고 `FundingTicketAccess(capacity, expiresAt)`을 반환합니다.
4. Classroom은 자체 Entity에 `ticketId`를 비고유 FK로 연결합니다. 이용권 하나로 복수 클래스를 허용하므로 unique 제약은 두지 않습니다.
5. Funding은 Classroom의 테이블/상태/멤버십을 직접 변경하지 않습니다. 환불 이후 기존 클래스에서 쓰기를 막는 동작은 별도 Class 담당자가 Funding 상태를 재검증하는 방식으로 결정합니다.

이번 범위에서 클래스 생성 API, 클래스 CRUD, 입장/멤버/투표/댓글, 배틀 카탈로그, 요약·리포트 API와 관련 Entity/Service/Repository를 구현하지 않습니다. AI 기능, AI API, AI 테이블, Provider Client와 Worker도 구현하지 않습니다.

---

## 7. 예외 처리

| 조건 | HTTP | ErrorCode 제안 | 화면/처리 |
|---|---:|---|---|
| 코드 문자열 누락, 형식 오류, 미발급 코드 | 422 | TICKET_CODE_INVALID | “코드가 올바르지 않아요. 입력한 코드를 다시 확인해 주세요.”, 입력 유지 |
| 코드 등록기한 경과 | 410 | TICKET_CODE_EXPIRED | 등록 기한 안내 |
| 다른 사용자가 먼저 등록했거나 사용 불가 | 409 | TICKET_CODE_UNAVAILABLE | 계정/소유자 정보 미노출 |
| 코드가 운영자에 의해 취소됨 | 409 | TICKET_CODE_REVOKED | 관리자 문의 안내 |
| 사용자가 이미 활성 이용권을 보유 | 409 | FUNDING_TICKET_ALREADY_ACTIVE | 기존 이용권 안내 |
| 결제 미확정·취소·환불 | 409 | FUNDING_NOT_PAYABLE | 관리자 확인 안내 |
| 취소된 이용권을 Classroom에서 사용 | 409 | FUNDING_TICKET_REVOKED | 클래스 생성 불가 안내 |
| 만료/미등록/다른 사람 소유 이용권을 Classroom에서 사용 | 403/409 | FUNDING_TICKET_FORBIDDEN / FUNDING_TICKET_NOT_ACTIVE | 소유자·이용 기간을 확인 |
| 중복 주문 또는 중복 캠페인 | 409 | FUNDING_ORDER_DUPLICATE / FUNDING_CAMPAIGN_DUPLICATE | 주문 키 확인 후 기존 주문 조회 |
| 필수 필드 누락/형식 오류, 지원하지 않는 정원·수량·연락처 | 400 | FUNDING_INVALID_REQUEST / FUNDING_REWARD_UNSUPPORTED / FUNDING_QUANTITY_INVALID / FUNDING_CONTACT_INVALID | 관리자 입력값 교정 |
| 존재하지 않는 주문, 이용권, 전달 기록 | 404 | FUNDING_ORDER_NOT_FOUND / FUNDING_TICKET_NOT_FOUND / FUNDING_DELIVERY_NOT_FOUND | 운영자 검색 조건 확인 |
| 이미 코드가 발급되었거나 재발급 조건 위반 | 409 | TICKET_CODE_ALREADY_ISSUED / FUNDING_REISSUE_NOT_ALLOWED | 기존 코드 상태 확인 |
| 환불 외부 확인 누락 또는 주문 환불 불가 | 409 | FUNDING_REFUND_NOT_CONFIRMED | 외부 플랫폼 환불 증빙 확인 |
| 전달 상태가 잘못되었거나 실패 사유 누락 | 400 | FUNDING_INVALID_DELIVERY_STATUS / COMMON_400 | 전달 결과와 실패 사유 입력 |
| 등록/검색 날짜 범위가 역전됨 | 400 | FUNDING_INVALID_DATE_RANGE | 시작일·종료일 수정 |
| 관리자 API 인증/권한 실패 | 401/403 | AUTH_UNAUTHORIZED / AUTH_FORBIDDEN | 로그인 또는 관리자 권한 확인 |
| 연락처 암호화 키 설정 누락/오류 | 500 | FUNDING_ENCRYPTION_NOT_CONFIGURED / FUNDING_ENCRYPTION_FAILED | 운영 설정 점검, 응답에서 키/원문 미노출 |
| 네트워크 끊김 | 서버 응답 없음 | 앱 로컬 NETWORK_ERROR | 입력 보존, 재조회 후 재시도 |

Funding 업무 예외는 `ErrorCode`에 각각 추가하며, 공통 Bean Validation 실패는 기존 `COMMON_INVALID_PARAMETER` 형식을 유지합니다. 중복 주문/캠페인 DB 제약은 GlobalExceptionHandler에서 Funding 충돌 코드로 바꿉니다. Classroom 계약은 Funding의 ticket 오류를 그대로 전달하며 자체 ErrorCode를 추가하지 않습니다. AI 오류는 구현하지 않습니다. HTTP status는 전송 수준, ErrorCode는 앱/관리자 화면 분기 수준입니다.

---

## 8. 이용권 기능 상세 설계

### 상품/기간

| 상품 | 가격 | 클래스별 정원 |
|---|---:|---:|
| 5인 | 9,900원 | 최대 5명 |
| 15인 | 19,900원 | 최대 15명 |
| 30인 | 29,900원 | 최대 30명 |

- 펀딩 결제 확정 후 1년 이내 코드 등록.
- 등록 확인 성공 시 6개월 활성화.
- 활성 이용권 하나로 클래스 수 제한 없이 생성.
- 상품 정원은 클래스별로 재적용되며 좌석은 이용권 전체에서 차감하지 않습니다.
- 인앱결제, 구매 복원, 앱 결제 SDK는 구현 범위가 아닙니다.
- 월/주별 생성 제한을 두지 않습니다.
- 코드 전달과 코드 등록은 별도 상태입니다.

### 코드 종류

| 종류 | 용도 | 정책 |
|---|---|---|
| TicketCode | 유료 펀딩 리워드를 계정 이용권으로 연결 | 충분한 난수, 해시 저장, 로그인 사용자만 등록 |
클래스 참여 코드(ParticipationCode)는 별도 Classroom 담당 범위입니다. Funding은 TicketCode만 발급·조회·등록하며 참여 코드 API/Entity를 추가하지 않습니다.

### 관리자 수동 발급 MVP

1. 관리자가 공식 펀딩 명단을 대조해 구매 주문과 결제 확정 여부를 기록합니다.
2. 상품 리워드 ID를 정원 5/15/30으로 매핑합니다.
3. ADMIN이 이용권과 등록 코드를 발급합니다.
4. 관리자 화면에서 발급 코드를 일회 확인해 사용자가 수동 전달하거나, 전달 채널/완료 상태만 기록합니다.
5. 결제 실패·취소·환불은 발급 보류 또는 폐기합니다.
6. 미사용 코드 재발급은 기존 코드를 폐기하고 registerBy를 유지합니다. 이미 등록된 이용권은 코드 재발급 대신 계정 복구로 처리합니다.

### 상태

- FundingOrder: RESERVED → PAYMENT_PENDING → PAID_CONFIRMED 또는 PAYMENT_FAILED/CANCELED/REFUNDED
- Ticket: UNREGISTERED → ACTIVE → EXPIRED, 별도 REGISTER_EXPIRED/REVOKED
- TicketCode: ISSUED → DELIVERED → CLAIMED, 별도 REVOKED
- 코드 발급 또는 전달만으로 이용 기간을 시작하지 않습니다.
- 환불 요청과 환불 확정을 구분하고 확정된 환불만 REVOKED 처리합니다.
- 만료 후 30일 결과 읽기 유예와 이후 삭제는 PRD 구현 기본안이며 보관 정책 승인이 필요합니다.

---

## 9. 이용권 화면 분기

GET /api/v1/tickets/me 응답 예시:

~~~json
{
  "statusCode": 200,
  "data": {
    "state": "ACTIVE",
    "ticketId": 4102,
    "capacity": 30,
    "activatedAt": "2026-10-15T03:00:00Z",
    "expiresAt": "2027-04-15T03:00:00Z",
  }
}
~~~

| 응답 상태 | 앱 화면 |
|---|---|
| NONE | 이용권 코드 등록 안내 |
| ACTIVE | 클래스 이용권 상세, 클래스 생성 진입 |
| EXPIRED | 종료일·읽기 유예 정책 표시, 신규 생성/활동 잠금 |
| REVOKED | 환불/취소 관련 문의 안내, 신규 활동 잠금 |

UNREGISTERED는 아직 계정에 claim되지 않은 펀딩 주문 상태이므로 기본적으로 관리자 화면에만 표시합니다. 등록 코드가 계정에 사전 연결되는 정책을 채택하면 이 분기를 확장합니다. “이용권 추가등록하기” UI와 PRD의 캠페인별 1인 1개 제한 간 의미는 앱/기획 확인 항목입니다.

---

## 10. 텀블벅 연동 방안 조사

확인일은 2026-10-15입니다. 텀블벅 공식 도움말에서 창작자는 후원자 관리의 데이터를 Excel로 메일 발송 받을 수 있고, 디지털 선물 전달에서는 템플릿에 후원번호를 적은 xlsx 업로드가 가능한 것을 확인했습니다. 공식 공개 자료에서 창작자용 후원자/결제 조회 API의 endpoint, 인증 방식, webhook, rate limit은 확인되지 않았습니다. 따라서 API 제공을 단정하지 않고, 텀블벅 지원/계약을 통해 API 이용 가능성과 범위를 별도 확인해야 합니다.

- [후원자 명단 다운로드 공식 안내](https://help.tumblbug.com/ko/categories/%EC%86%8C%ED%86%B5%ED%95%98%EA%B3%A0-%EC%A0%84%EB%8B%AC%ED%95%98%EA%B8%B0-92f62bf3)
- [디지털 선물 Excel 전달 공식 안내](https://help.tumblbug.com/ko/articles/7717bf63)

| 방식 | 현재 결정 | 운영 방식 |
|---|---|---|
| 텀블벅 API | 보류 | 공식 공개 자료에서 창작자 후원·결제 조회 API를 확인하지 못했습니다. 접근 권한과 데이터 범위를 공식 제휴 문의로 확인하기 전에는 연동을 전제하지 않습니다. |
| 명단 Excel | 사람 검토 + 서버 파싱 | 운영자가 텀블벅에서 직접 요청·다운로드합니다. 관리자는 리워드 시트만 선택하고 서버가 헤더 행과 화면의 9개 열을 자동 판별합니다. 미리보기에서 결제 상태·중복·수량을 검토합니다. |
| 픽케 주문 등록·발급 | 서버 처리, 관리자 승인 | 확정 작업에서 유효 행만 주문·티켓·코드로 생성하고, 후원번호와 코드 텍스트를 연결한 XLSX를 반환합니다. 원문 코드는 DB에 저장하지 않습니다. |
| 텀블벅 전달·상태 동기화 | 사람 처리 | 운영자가 텀블벅 디지털 선물 관리에 텍스트 파일을 업로드하고, 업로드 결과를 서버에 후원번호 목록으로 기록합니다. 실시간 API 동기화는 없습니다. |

현재 운영 흐름은 `텀블벅 Excel 다운로드 → 서버 시트 선택·열 자동 판별 및 미리보기 → 관리자 확인 → 유효 후원 행 저장·결제 완료 행 코드 발급 및 텀블벅 업로드 파일 다운로드 → 텀블벅 디지털 선물 업로드 → 서버에 전달 결과 기록`입니다. 플랫폼과 서버 사이의 API 자동 동기화는 포함하지 않습니다.

주의: 공식 도움말의 디지털 선물 Excel 기능은 텀블벅 내부에서 선물을 전달하는 기능입니다. 픽케 자체 구매자/코드 DB와 자동 동기화되는 API로 취급하지 않습니다.

---

## 11. 관리자 대시보드 설계

### 화면에서 확인된 기능

| 기능 | 확인 결과 |
|---|---|
| 클래스 목록/검색/상태·상품·배틀 유형 필터 | 있음 (별도 Classroom 구현 범위) |
| 클래스 운영 지표 카드 | 있음 (별도 Classroom 구현 범위) |
| 클래스 CSV 내보내기 | 있음 (별도 Classroom 구현 범위) |
| 이용권/등록자 목록·상태 카드 | 있음 |
| 이용권 코드 발급 | 있음 |
| 펀딩 구매자 등록/검색 | 있음 |
| 구매자 연락처·주문·상품·전달 상태 | 있음 |
| 클래스 상세와 기본 정보/멤버·참여/결과·AI/변경 이력 탭 | 있음 (별도 Classroom/AI 범위) |
| 별도 사용자 전체 목록 화면 | 첨부 이미지에는 없음 |
| 코드 상세 화면/수정/비활성 UI | 별도 이미지에 없음 |
| 텀블벅 Excel 처리 | 백엔드 API 있음, 첨부 시안에는 UI 없음 |
| 구매자 편집/삭제 | 없음 |
| 코드 재발급 버튼 | 없음 |
| 사용자 서비스 내부 ADMIN 로그인 화면 | 없음; 헤더만 표시 |

화면에 없는 운영 요구는 기본 CRUD로 추가하지 않습니다. 결제 환불/재발급/명단 재처리가 꼭 필요하면 API는 설계에 표시하되 UI에 **운영상 필요 가능성 있음 - 추가 논의 필요**로 둡니다.

위 표의 클래스 화면은 UI 분석 결과이며 이번 Funding 기능에서 구현하지 않습니다. 이번 관리자 구현은 이용권/등록자 목록, 구매자 등록·검색, 코드 발급 및 전달 상태에 한정합니다.

### 관리자 목록 처리 기준

- server-side paging/filtering 권장. 요청은 page=0, size 기본 20, sort는 allowlist.
- 클래스 검색: name 또는 owner display name/tag, status, capacity, LIBRARY/CUSTOM.
- 이용권 검색: code 일부/구매자/등록자, capacity, ticket 상태, registered date range.
- 펀딩 구매자 검색: 이름 및 연락처는 암호화/검색용 별도 설계를 거친 뒤 최소 필드만 검색.
- 날짜 범위는 Asia/Seoul 입력, 서버 UTC 저장. 조회 범위 종료일은 화면과 백엔드에서 inclusive/exclusive 중 하나로 합의.
- CSV는 ADMIN만 다운로드, 현재 검색 필터와 동일한 조건 적용, PII 최소화/마스킹.
- 코드 원문은 목록에서 기본 마스킹하고 발급 때만 텀블벅 업로드용 다운로드 파일에 담습니다. 파일 응답은 캐시하지 않습니다. 화면에서 개별 코드 발급 시에는 응답에 한 번 표시합니다.

---

## 12. 관리자 API

관리자 화면 구현에 필요한 API는 §4의 [신규 제안] endpoint를 사용합니다.

| 화면 | 필요한 API | 검색/페이지 조건 |
|---|---|---|
| 이용권·등록자 | GET /admin/tickets, POST /admin/tickets/{ticketId}/codes | keyword, capacity, status, registeredFrom, registeredTo, page, size |
| 펀딩 구매자 | GET/POST /admin/funding-orders, Tumblbug inspect/preview/digital-gifts, PATCH 전달 결과 | keyword, capacity, deliveryStatus, claimStatus, page, size |
| 환불·재발급 운영 | POST /admin/tickets/{ticketId}/refund, POST /admin/tickets/{ticketId}/reissue | reason 필수, 외부 확인 기록 |

모든 실제 URL은 /api/v1 접두어를 붙입니다. 관리자 화면 검색 결과의 구매자 연락처는 전체값을 기본 응답에 담지 않습니다. 관리자 클래스 목록/상세/CSV API는 별도 Classroom 담당 범위입니다.

---

## 13. 관리자 인증/인가

- 현재 UserRole에 ADMIN이 있고 SecurityConfig에서 /api/v1/admin/**는 hasRole("ADMIN")입니다.
- 신규 관리자 API도 해당 prefix에 놓고 메서드 단위 @PreAuthorize("hasRole('ADMIN')")를 병행합니다.
- 일반 로그인 API는 기존 OAuth/JWT를 사용합니다. AdminPickeController의 GET /api/v1/admin/login은 현재 외부 admin-web 로그인 페이지로 redirect하는 경로이며, 백엔드 로그인 API로 표시하지 않습니다.
- 클래스 운영자는 서비스 내 클래스 owner 권한으로 본인 클래스만 관리합니다. ROLE_ADMIN이 클래스 소유 권한을 대체하지 않습니다.
- 개인정보 응답은 관리자에서도 필요한 컬럼만 제공합니다. 열람/코드 발급/환불/재발급은 AdminAuditLog에 관리자 id, action, target, reason, 시각을 기록합니다.

---

## 14. 앱 개발자와 논의할 사항

### 화면/UI

- 고정 텍스트/FAQ/이미지는 App Asset으로 관리합니다. 이용권 상태·정원·날짜·코드 결과는 서버 응답을 씁니다.
- 버튼 활성 조건: 코드 입력 non-empty, claim 요청 진행 중 중복 제출 방지.
- 이용권 상태와 “이용권 추가등록하기” 의미(캠페인별 복수 보유 허용 여부).
- 날짜 표시 KST, 서버 ISO-8601/UTC, 달력 기준 1년/6개월 계산과 말일 경계.
- Enum→한국어 라벨 매핑은 App에서 하고 API message를 UI 분기로 쓰지 않습니다.
- Empty/Loading/Error/Network Error/Unauthorized/Forbidden 상태에서 입력 보존과 재시도 UX.
- 이용권 코드 형식, FAQ 문구, 화면에서 쓸 이용권 상태/코드 명칭.

### API/Auth

- 공통 성공 응답의 HTTP status와 body statusCode 일치 방식.
- ErrorCode만 내려줄지, message를 앱에서 매핑할지.
- nullable 필드와 이용권 상태 enum 표시.
- Refresh Token은 현재 X-Refresh-Token 헤더 사용.
- Version prefix는 기존 /api/v1을 유지.
- Pagination: 요청 0-based/응답 1-based인 현재 혼합 계약을 신규 API에서 어떻게 통일할지.
- claim용 Idempotency-Key 보존 기간과 앱의 네트워크 복구 후 재전송 방식.
- 클래스 참여 코드/게스트 입장, AI 작업 UX는 각각 Classroom/AI 담당 범위에서 논의합니다.

---

## 15. 관리자 프론트엔드 개발자와 논의할 사항

- 서버 pagination/filter/sort 채택 및 size 상한, 기본 정렬.
- 검색 버튼 기반 조회인지 debounce 검색인지. 현 시안은 조회 버튼을 보이므로 명시적 조회를 기본안으로 둡니다.
- 날짜 검색의 시간대/끝 날짜 포함 여부.
- 이용권 상태/결제 상태/전달 상태 Enum 표시명.
- 구매자 연락처·이용권 코드 마스킹 및 상세 열람 감사.
- “구매자 등록” 입력 필드와 결제 완료 증빙 방식.
- 코드 발급 직후 원문을 일회 표시·복사하는 모달.
- 전달 실패 기록과 재전달/재발급을 구별.
- 취소/환불은 물리 삭제 대신 REVOKED 상태로 보존.
- CSV 필터 적용, 다운로드 인가, 포함 컬럼/PII.
- 명단 Excel은 운영자가 텀블벅에서 직접 내려받아 관리자 기능에 업로드합니다. 시트를 선택하면 서버가 9개 열을 자동 판별하고, 운영자는 미리보기에서 결제 상태를 확인한 뒤 저장·코드 발급을 진행합니다.
- 클래스 상세/운영 화면은 별도 Classroom 담당 범위입니다.

---

## 16. 화면 상태 정의

| 상태 | 백엔드 표현 | 화면 처리 |
|---|---|---|
| Loading | Funding API 응답 전 | 진행 표시; AI job 상태는 구현 범위 밖 |
| Empty | 목록 API content=[] 또는 ticket state=NONE | App/관리자 Empty 안내 |
| Success | HTTP 2xx + data | 결과 렌더링 |
| Error | HTTP 4xx/5xx + ErrorCode | 업무 오류별 매핑 |
| Network Error | 서버 응답 없음 | 앱/브라우저 자체 감지, 입력 유지 |
| Unauthorized | 401 AUTH_UNAUTHORIZED/토큰 만료 | 재로그인/refresh |
| Forbidden | 403 AUTH_FORBIDDEN/범위 권한 없음 | 접근 거절, 민감 데이터 미반환 |
| Classroom 만료 | 별도 Classroom 도메인에서 처리 | Funding 구현은 ticket 만료 상태와 조회만 제공 |

서버가 업무 상태와 오류 코드를 구분해서 주고, Loading·네트워크 단절·토스트·정적 안내 문구는 각 프론트엔드에서 처리합니다.

---

## 17. UI 문구 수정사항

오류 화면에 표시된 구 문구는 사용하지 않습니다. 문서와 API 예시에 아래 문구만 사용합니다.

> 코드가 올바르지 않아요.  
> 입력한 코드를 다시 확인해 주세요.

권장 API는 안정적인 Funding ErrorCode를 반환하고 App이 문구를 매핑하는 방식입니다. 현재 공통 오류 응답은 message도 포함하므로, server message를 표시할지 App 번역 상수로 관리할지는 앱 개발자 협의 항목입니다.

---

## 18. 전체 API 목록

이번 구현 API 목록은 Funding 범위와 현재 구현된 공통 API만 포함합니다. Classroom 화면에서 보인 클래스 API와 PRD의 AI API는 분석 참고사항이며 신규 제안/구현 대상이 아닙니다. Classroom 연동은 내부 `FundingTicketPolicy` 호출이므로 HTTP API를 추가하지 않습니다.

### 사용자

| Type | Domain | Method | Endpoint | 상태 | 인증 |
|---|---|---|---|---|---|
| USER | Auth | POST | /api/v1/auth/login/{provider} | 존재 | 없음 |
| USER | Auth | POST | /api/v1/auth/refresh | 존재 | Refresh Token |
| USER | Auth | POST | /api/v1/auth/logout | 존재 | USER |
| USER | Mypage | GET | /api/v1/me/mypage | 존재 | USER |
| USER | Battle Catalog | GET | /api/v1/battles | 부분 존재, 일반 목록 | 기존 정책 |
| USER | Battle Catalog | GET | /api/v1/battles/{battleId} | 부분 존재, 일반 상세 | 기존 정책 |
| USER | Fundings | GET | /api/v1/tickets/me | 구현 완료 | USER |
| USER | Fundings | POST | /api/v1/ticket-codes/preview | 구현 완료 | USER |
| USER | Fundings | POST | /api/v1/ticket-codes/claim | 구현 완료 | USER |

### 관리자

| Type | Domain | Method | Endpoint | 상태 | 인증 |
|---|---|---|---|---|---|
| ADMIN | Users | GET | /api/v1/admin/users/search | 존재 | ADMIN |
| ADMIN | Dashboard | GET | /api/v1/admin/dashboard/summary | 존재 | ADMIN |
| ADMIN | Funding | GET | /api/v1/admin/tickets | 구현 완료 | ADMIN |
| ADMIN | Funding | GET | /api/v1/admin/funding-orders | 구현 완료 | ADMIN |
| ADMIN | Funding | POST | /api/v1/admin/funding-orders | 구현 완료, 수동 P0 | ADMIN |
| ADMIN | Funding | POST | /api/v1/admin/tickets/{ticketId}/codes | 구현 완료 | ADMIN |
| ADMIN | Funding | PATCH | /api/v1/admin/deliveries/{id} | 구현 완료 | ADMIN |
| ADMIN | Funding | POST | /api/v1/admin/tickets/{ticketId}/reissue | 구현 완료 | ADMIN |
| ADMIN | Funding | POST | /api/v1/admin/tickets/{ticketId}/refund | 구현 완료 | ADMIN |

일반 관리자 대시보드의 기존 통계 endpoint는 클래스 지표를 제공하지 않습니다. Funding 범위에 포함되지 않은 클래스 화면 API는 구현 완료 또는 신규 Funding API로 계산하지 않습니다.

---

## 19. 최종 Entity 목록

- User (기존): 로그인 사용자 및 시스템 역할.
- FundingCampaign: 펀딩 플랫폼 캠페인과 상품 매핑.
- FundingOrder: 결제 확정 주문/리워드 행과 최소 구매자 정보.
- Ticket: 정원·등록 기한·계정 귀속·활성/만료 권한.
- TicketCode: 이용권 claim용 비밀 코드의 해시와 상태.
- DeliveryAttempt: 코드 전달 시도 및 실패 결과.
- AdminAuditLog: Funding 관련 민감한 관리자 변경 이력.

Classroom은 별도 개발자가 소유합니다. Funding 구현은 별도 클래스 Entity를 만들지 않고, 기존 Classroom에 `ticketId` 참조와 생성 시 Funding 유효성 확인만 연결합니다. AI 관련 Entity는 만들지 않습니다.

---

## 20. 최종 아키텍처 요약

### 핵심 도메인

Funding, Classroom 최소 연동, 기존 User/Auth/Admin 공통 기반.

### 핵심 Entity

User(기존), FundingCampaign, FundingOrder, Ticket, TicketCode, DeliveryAttempt, AdminAuditLog. Classroom은 별도 담당의 기존 Entity에 `ticketId` 연결만 합의합니다.

### 핵심 Service

FundingTicketService, FundingAdminService, FundingTicketPolicy (내부 연동 계약).

### 핵심 API

이용권: GET /api/v1/tickets/me, POST /api/v1/ticket-codes/preview, POST /api/v1/ticket-codes/claim.  
클래스 연동: 클래스 담당자가 생성 시 `FundingTicketPolicy`를 내부 호출합니다 (HTTP endpoint 추가 없음).  
관리자: /api/v1/admin/tickets, /funding-orders, 텀블벅 엑셀 미리보기·코드파일 생성과 전달 기록.

### 외부 연동

현재 운영은 관리자가 텀블벅 엑셀을 직접 내려받아 확인하고, 서버에 업로드해 결제 완료 행의 코드와 텀블벅 디지털 선물 업로드 파일을 생성하는 방식입니다. 텀블벅 API를 이용한 자동 동기화는 포함하지 않습니다.

### 관리자 기능

펀딩 구매자 수동 등록, 이용권 코드 발급과 전달 상태, 이용권/등록자 조회와 CSV export. 클래스 대시보드는 별도 구현 범위입니다.

### 먼저 합의할 사항

- 앱: “추가 등록”과 1인 1개 제한, 코드 오류 문구 전달 방식, 기간 계산/시간대.
- Classroom 담당자: `ticketId` FK 소유권, 클래스 생성 시 Funding 정책 호출 지점과 환불 상태 재검증 방식.
- 관리자 프론트엔드: 코드 원문 노출/마스킹, 구매자 연락처 접근, 전달 실패/재발급 흐름, CSV 필드.
- 기획/운영: 플랫폼 선택, 결제 확정 증빙, 환불 정책, 구매자 데이터 보유 기간, Excel 시점.

### 구현 우선순위

- **P0**: 구매자/주문 수동 등록, 결제 상태 확인, 이용권·코드 발급과 claim, 전달 상태, 관리자 Funding 조회, Classroom 유효성 정책 계약 및 `ticketId` 연결.
- **P1**: Funding 운영 알림·만료 자동화, 재발급/환불 보조 흐름, 개인정보 보관·파기 자동화.
- **보류**: 텀블벅 공식 플랫폼 API 연동/자동 동기화와 자동 전달. 명단 파일 기반의 사람 검토형 가져오기/코드 파일 생성 API는 구현 대상에 포함합니다.

## 21. 구현 완료 및 미포함 항목

### 구현 완료

- 사용자 API: 이용권 상태 조회, 등록 코드 미리보기, 코드 등록.
- 관리자 API: 단건 주문 등록, 텀블벅 XLSX 시트 확인/미리보기/후원번호 중복 검사, 결제 완료 행 코드 발급 및 디지털 선물 업로드 파일 생성, 후원번호별 전달 결과 기록, 주문/이용권 검색, 코드 발급·재발급, 환불 확정 처리.
- 저장소 계층: 캠페인, 주문, 이용권, 코드, 전달 시도, 관리자 변경 로그 Entity/Repository 및 DB migration SQL.
- 동시성/보안: 코드와 사용자 행 잠금 기반 claim, 고유 주문 키, 코드 원문 미저장, 구매자 연락처 AES-GCM 암호화·HMAC 검색, 응답 마스킹.
- API 표에서 `[존재]`로 표시된 로그인·마이페이지·일반 관리자 API는 원래 있던 API입니다. `[구현 완료]`는 이번 변경으로 추가된 Funding API입니다.
- Swagger: 사용자 Funding API는 사용자 API 그룹 화이트리스트에 등록했고, 사용자/관리자 API 모두 요약·설명·인증·주요 응답 코드를 명시했습니다.
- API별 Method, URL, 요청 옵션, 저장 필드와 주요 오류는 [Funding API 빠른 참조](FUNDING_API.md)에 정리했습니다. Funding 스키마 변경은 단일 파일 [V20261015_01__create_fundings_domain.sql](src/main/resources/db/migration/V20261015_01__create_fundings_domain.sql)에 통합했습니다.

### 이 변경에 포함하지 않음

- `classroom`에는 Funding의 유효 이용권 조회 계약만 추가했습니다. 클래스 Entity, `ticketId` 필드, 클래스 API/화면은 클래스 담당자가 연결해야 합니다.
- AI 도메인, AI API 및 AI Provider 연동은 구현하지 않았습니다.
- 텀블벅 명단과 텍스트 선물 Excel은 운영자가 텀블벅에서 직접 내려받고 업로드합니다. 서버는 운영자가 선택한 시트의 화면 9개 열을 자동 판별하고 유효한 행을 저장하며, 결제 완료 행만 코드화합니다. 텀블벅 사이트 업로드와 완료 결과 입력은 운영자가 수행하며 플랫폼 API 자동 동기화·이메일/SMS 자동 발송은 없습니다.
- 새 코드 형식은 `PICKE-` 뒤에 O 제외 A-Z와 1-9에서 뽑은 28개 난수를 4자씩 고정 그룹으로 출력합니다. 접두부는 엔트로피에 포함되지 않으며 난수 공간은 약 142비트입니다. 기존 PK 코드는 하위 호환을 위해 claim 허용합니다.
- 구매자 연락처 암호화 기능은 배포 환경에 `FUNDING_CONTACT_ENCRYPTION_KEY`를 Base64 형식의 32바이트 키로 설정해야 주문 등록과 연락처 조회가 작동합니다.

Classroom 구현과 AI 도메인은 이번 범위에 포함하지 않습니다. Funding의 중복·권한·정원·기간·코드 claim 보장은 DB 제약과 트랜잭션을 기준으로 합니다.

