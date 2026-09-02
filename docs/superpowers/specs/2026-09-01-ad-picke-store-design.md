# ad.picke.store 제휴 광고 설계

- 작성일: 2026-09-01
- 상태: 승인됨 (구현 진행)
- 범위: 쿠팡 파트너스 + 애드픽 제휴 광고를 앱 지면에 노출하고, 클릭을 추적·집계한다.

## 1. 배경과 목표

앱 화면 중간중간에 제휴 광고 배너를 노출해 수익을 만든다. 배너는 **앱이 네이티브로 렌더**하고,
탭하면 **외부 브라우저로 제휴 링크에 다이렉트**된다.

매체는 두 곳이다.

| 매체 | 식별자 | 성격 |
| --- | --- | --- |
| 쿠팡 파트너스 | `AF6830373` | 커머스 CPS. 상품 구매 전환 |
| 애드픽 | 가입 예정 | 성과형 CPA/CPI. 앱 설치·이벤트 참여 |

네이버(쇼핑커넥트)는 **범위에서 제외**한다. 가입 단위가 블로그·인스타 같은 크리에이터 채널이라
앱을 매체로 등록하는 경로가 없고, 인증 채널 밖에 링크를 게시하면 약관 위반 소지가 있다.

### 목표가 아닌 것

- 쿠팡 파트너스 오픈API 연동. 파트너스 실적 요건 충족 후 승인제라 지금은 쓸 수 없다.
  소재 자동 수급은 `AdCreative` 생성 경로만 추가하면 되므로 나중에 얹는다.
- 사용자별 클릭 귀속. 3.4 참조.
- AdMob 리워드 광고 통합. 이미 `reward` 도메인에 별도로 존재한다.

## 2. 접근 방식

**어드민 수동 등록 + 서버 리다이렉트 트래킹.**

각 매체 콘솔에서 뽑은 완성형 제휴 링크를 어드민에 소재로 등록한다. 앱은 지면 코드로 소재를 조회해
네이티브로 그리고, 탭하면 우리 서버의 리다이렉트 엔드포인트를 거쳐 제휴 링크로 나간다.

두 매체를 하나의 파이프라인으로 처리할 수 있고 외부 API 의존이 없다. 대신 소재를 사람이 채워야 하고,
상품 가격·품절이 실시간 반영되지 않는다. 소재 수가 수십 개 규모라 감당 가능한 비용으로 본다.

### 기각한 대안

- **WebView 임베드**: 소재 교체가 앱 배포와 무관해지지만, 스크롤 중첩·렌더 지연·다크모드 불일치가 생긴다.
  "앱 UI에 네이티브로 보여야 한다"는 요구와 어긋난다.
- **애드픽 마이도메인**: 애드픽이 자체 도메인 트래킹 링크를 지원하나, 애드픽 링크만 커버한다.
  쿠팡까지 한곳에서 집계하고 클릭 로그를 우리 DB에 두려면 자체 리다이렉트가 맞다.

## 3. 설계

### 3.1 패키지 구조

```
domain/ad/
  controller/  AdController          앱 조회 API
               AdClickController     /c/{code} 302 리다이렉트
               AdLandingController   ad.picke.store 루트 공개 지면
  service/     AdQueryService  AdClickService
  link/        AffiliateLinkBuilder  CoupangLinkBuilder  AdpickLinkBuilder
  entity/      AdCreative  AdClickLog  AdImpressionDaily
  enums/       AdNetwork  AdSlotCode  AdStatus
  repository/  dto/
domain/admin/  AdminAdController + AdminAdService  (기존 어드민 관례를 따른다)
```

`reward` 도메인의 `AdRewardHistory`(AdMob)와 이름이 겹쳐 보이지만, 그쪽은 리워드 광고 시청 보상이고
이쪽은 제휴 광고다. `AdNetwork` enum으로 구분된다.

### 3.2 지면(slot)

`AdSlotCode` **enum으로 둔다. 테이블이 아니다.**

어드민에서 지면을 새로 만들어도 앱이 그 지면을 그릴 줄 모르면 아무 일도 일어나지 않는다.
지면 추가는 어차피 앱 배포와 묶이므로, 테이블로 빼면 실제로 쓸 수 없는 유연성만 생긴다.

지면 목록은 iOS Presentation 모듈(Home/Battle/Chat/Profile)의 실제 화면을 기준으로 잡았다.
앱팀 확정 전이므로, 실제로 붙이는 지면에만 소재를 등록하면 된다. 소재가 없는 지면은 빈 배열을 주고
앱은 지면 자체를 숨기므로 미사용 지면이 남아 있어도 부작용이 없다.

| 지면 | 화면 | CPI 허용 |
| --- | --- | --- |
| `HOME_FEED` | 홈 피드 인라인 | 아니오 |
| `BATTLE_RESULT_BOTTOM` | 배틀 결과 하단 | 예 |
| `CHAT_ROOM_INLINE` | 관점 목록 인라인 | 아니오 |
| `ATTENDANCE_COMPLETE` | 출석 완료 후 | 예 |
| `PROFILE_BOTTOM` | 프로필 하단 | 예 |

`cpiFriendly`는 앱 설치형(CPI) 광고를 놓아도 되는 지면인지를 뜻한다. 5장 트레이드오프 참조.

### 3.3 데이터 모델

**`ad_creatives`** — 소재. `BaseEntity` 상속(id, created_at, updated_at).

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `code` | varchar(16) unique | 공개 클릭 URL용 짧은 코드. PK 노출 방지 |
| `network` | varchar | `COUPANG` \| `ADPICK` |
| `slot` | varchar | `AdSlotCode` |
| `title` | varchar(100) | 배너 주 문구 |
| `subtitle` | varchar(200) nullable | 보조 문구 |
| `image_url` | varchar(500) | 소재 이미지 |
| `cta_text` | varchar(30) | "구매하러 가기" / "설치하고 받기" 등 |
| `landing_url` | varchar(1000) | 콘솔에서 뽑은 원본 제휴 링크 |
| `status` | varchar | `DRAFT` \| `ACTIVE` \| `PAUSED` |
| `weight` | int | 가중 로테이션. 기본 1 |
| `starts_at` / `ends_at` | timestamp nullable | 게재 기간. null이면 무제한 |

`cta_text`를 매체별 하드코딩이 아니라 소재 단위로 두는 이유는, 쿠팡은 상품 구매이고 애드픽은
앱 설치·이벤트 참여라 문구 성격이 다르기 때문이다.

**`ad_click_logs`** — 클릭 원장. creative_id, slot, ip_hash, user_agent, clicked_at.
제휴사 리포트와 대조하는 용도다.

**`ad_impression_daily`** — 노출 집계. (creative_id, slot, stat_date) 유니크 + impressions 카운터.

노출은 raw 로그로 쌓지 않는다. 배너가 스크롤에 걸릴 때마다 행이 생기면 금방 수천만 건이 된다.
일별 upsert 카운터로 CTR을 뽑는 데 충분하다.

### 3.4 클릭 로그는 익명이다

`/c/{code}`는 **외부 브라우저에서 열린다.** Authorization 헤더가 없다.

사용자를 붙이려면 클릭 URL에 사용자 식별자를 실어야 하는데, 공개 URL에 그걸 넣으면 열거 공격과
프라이버시 문제가 생긴다. 서명된 단기 토큰을 발급하는 방법도 있지만 v1에 그만한 값어치가 없다.

지면별 CTR과 정산 대조에는 userId가 필요 없으므로 **v1은 익명(ip_hash + user_agent)으로 간다.**
본인 클릭 어뷰징 탐지가 필요해지면 그때 추가한다.

### 3.5 API

모든 앱/어드민 API는 `/api/v1/` 아래에 둔다.

**앱**

- `GET /api/v1/ads?slot={AdSlotCode}` → `ApiResponse<List<AdResponse>>`
  `{ code, network, title, subtitle, imageUrl, ctaText, clickUrl, label }`
  `clickUrl` = `https://ad.picke.store/c/{code}`
  `label`은 `"광고"` 고정. 표시광고법 대응이므로 앱이 반드시 렌더해야 한다.
- `POST /api/v1/ads/impressions` — `{ codes: [...] }` 묶음 전송

조회 시점에 노출을 집계하면 엔드포인트 하나를 아끼지만 **조회 ≠ 실제 노출**이라 CTR이 왜곡된다.
지면 성과로 배치를 정할 것이므로 분리한다.

**클릭 리다이렉트**

- `GET /c/{code}` → 302 Location: 제휴 링크

`/api/v1` 밑에 두지 않는다. 공개 숏링크라 짧아야 하고, JSON API가 아니라 브라우저 진입점이다.

`landing_url`에 매체별 추적 파라미터를 **병합**한다. 원본 링크에 이미 쿼리스트링이 있으므로
단순 문자열 결합이 아니다. 클릭 로그는 비동기로 적재해 리다이렉트를 DB 쓰기가 붙잡지 않게 한다.

코드가 없거나 만료면 404 대신 랜딩 페이지로 302한다. 사용자에게 실패를 보이지 않는다.

**랜딩** — `GET /` (Host: `ad.picke.store`) → Thymeleaf `ad/landing`

ACTIVE 소재를 카드로 나열하고 하단에 쿠팡 파트너스 수수료 고지 문구를 넣는다.
쿠팡 파트너스 매체 심사에서 URL 접속 확인을 하므로, 빈 페이지면 반려된다.

Host가 광고 도메인이 아니면 최소 응답만 돌려준다. API 도메인 루트에 광고 페이지가 뜨면 안 된다.

**어드민** — `/api/v1/admin/ads` CRUD, `/api/v1/admin/ads/stats?from=&to=`
이미지 업로드는 기존 S3 presigned 경로를 재사용한다.

### 3.6 매체별 링크 빌더

`AffiliateLinkBuilder` 인터페이스 하나에 매체별 구현체를 둔다.

- `CoupangLinkBuilder` — `subId={slot}_{code}` 병합. 파트너스 리포트에서 지면별 실매출이 갈린다.
- `AdpickLinkBuilder` — 파라미터명을 `picke.ad.adpick.sub-id-param` 설정값으로 둔다.
  서브아이디 규격을 아직 확인하지 못해 기본값은 비어 있고, 그동안은 pass-through로 원본 링크를 넘긴다.
  파트너센터 링크생성 화면에서 규격이 확인되면 **배포 없이 환경변수만 채우면** 쿠팡과 같은 방식으로 붙는다.
  그전까지 애드픽은 지면별 성과 분리가 안 될 뿐, 노출·클릭·리다이렉트는 정상 동작한다.

### 3.5.1 애드픽 캠페인 자동 수급

애드픽은 캠페인 리스트 JSON API(`offers.php`)를 인증 없이 공개한다. 쿠팡 오픈API와 달리 승인 절차가 없어
애드픽 소재는 사람이 등록하지 않고 주기 동기화로 채운다.

수집한 캠페인은 **같은 `ad_creatives` 테이블에 `source = ADPICK_API`로 저장한다.** 별도 테이블을 두지 않으므로
로테이션·노출 집계·클릭 추적 경로를 그대로 탄다.

응답에서 쓰는 필드는 `apOffer`(캠페인 ID), `apAppTitle`, `apHeadline`/`apAppPromoText`, `apImages.icon`,
`apTrackingLink`, `apOS`, `apRemain`이다.

호출 제약이 있다. 애드픽 가이드가 **최대 1분에 1회 이하 호출과 저장 후 사용**을 요구하고, 실제로 짧은 간격으로
연달아 호출하면 403을 돌려준다. 그래서 요청 때마다 부르지 않고 스케줄러로만 부른다.

동기화 규칙은 이렇다.

- `apRemain`이 0이면 게재하지 않는다. 잔여가 없는 캠페인은 클릭해도 전환이 잡히지 않는다.
- 피드에서 사라진 캠페인은 지우지 않고 내린다. 쌓인 노출·클릭 집계가 어느 소재의 것인지 계속 읽혀야 한다.
- **`PAUSED`는 동기화가 되돌리지 않는다.** 별도 플래그 없이 어드민의 끄기 스위치로 쓴다.
- 동기화가 내용을 덮어쓰므로 어드민에서 수정·삭제는 막고, 게재 상태만 바꾸게 한다.

`affId`(애드픽 회원 아이디)가 비어 있으면 동기화를 건너뛴다. 가입 전에도 나머지 기능은 그대로 돈다.

### 3.5.2 OS 타깃팅

애드픽 앱 설치형 캠페인은 `apOS`로 OS가 갈린다. iOS 사용자에게 Android 캠페인을 보여주면 클릭해도
전환이 일어나지 않으므로, 소재에 `target_os`를 두고 조회 시 요청 OS와 맞는 것만 준다.

`GET /api/v1/ads`에 `os` 파라미터가 필요하다. 값을 안 보내면 `ALL`로 보고 모든 소재를 후보로 둔다.

### 3.6.1 쿠팡 파트너스 아이디 대조

남의 파트너스 링크를 잘못 붙여넣으면 우리가 광고를 싣고 수수료는 남이 받는다.
소재 등록·수정 시 `landingUrl`의 `lptag`를 `coupang.partners.id`와 대조해 다르면 거부한다.

다만 `link.coupang.com` 단축 링크에는 `lptag`가 드러나지 않으므로 **파라미터가 있을 때만** 본다.
없다고 막으면 정상적인 단축 링크를 쓸 수 없다.

### 3.7 인증 우회 경로

`JwtFilter`가 SecurityConfig보다 먼저 돌면서 **토큰이 없으면 무조건 401**을 던진다.
따라서 `SecurityConfig.permitAll`만으로는 공개 엔드포인트가 뚫리지 않고, `JwtFilter.WHITELIST`에도 넣어야 한다.

그런데 `isWhitelisted`가 `startsWith` 매칭이라 `"/"`를 넣으면 전체 인증이 무력화된다.
**정확히 일치할 때만 통과하는 `EXACT_WHITELIST`를 분리해 `/`와 `/error`를 넣는다.**

`/error`가 빠져 있던 탓에 존재하지 않는 모든 경로가 404 대신 401로 나오고 있었다. 같이 고친다.

### 3.8 Swagger 분리

광고 API는 별도 그룹 `3. 광고 API`로 띄운다. `/api/v1/ads/**`, `/api/v1/admin/ads/**`를 매칭하고,
기존 사용자·관리자 그룹에서는 제외해 섞이지 않게 한다.

기존 `userApi` 그룹은 `FE_USED_OPERATIONS` 화이트리스트로 필터링되므로, 광고 API를 거기 넣으면
어차피 보이지 않는다. 별도 그룹이 구조적으로 맞다.

## 4. 운영 선행 작업

| 항목 | 상태 |
| --- | --- |
| `ad.picke.store` DNS + Railway 커스텀 도메인 + TLS | 완료 |
| 루트 공개 지면 배포 | 본 구현에 포함 |
| 광고 테이블 생성 | 별도 실행 불필요. `ddl-auto: update`라 배포 시 자동 생성된다 |
| 쿠팡 파트너스 가입·매체 등록 | ID 발급됨(`AF6830373`), 매체 등록 확인 필요 |
| 애드픽 파트너 가입 | 미착수. 계정이 필요해 코드로 대신할 수 없다 |
| 애드픽 `affId` 주입 | 가입 후 `ADPICK_AFF_ID` 설정. 비어 있으면 동기화를 건너뛴다 |
| 애드픽 서브아이디 파라미터 규격 확인 | 미확인. 확인되면 `ADPICK_SUB_ID_PARAM` 환경변수만 채우면 된다 |
| 애드픽 이용정책상 자체 앱 배너 노출 허용 여부 | 미확인 |
| 지면 목록 앱팀 확정 | 후보 5개 확정, 앱팀 확인 대기 |
| Play Console / App Store Connect "광고 포함" 신고 | 미착수 |
| 개인정보처리방침에 제휴 광고 문구 추가 | 미착수 |

## 5. 지면 배치 트레이드오프

애드픽 캠페인 상당수가 CPI(앱 설치형)다. 단가는 커머스보다 높지만 클릭하면 사용자가 스토어로 나가
다른 앱을 설치한다. 배틀 진행 중간 지면에 CPI를 깔면 이탈·리텐션에 직접 타격이 온다.

세션이 자연스럽게 끝나는 지점(배틀 결과 화면)에 CPI를 두고, 피드 중간 인라인은 이탈 부담이 적은
쿠팡 커머스로 채우는 배치를 권한다. 데이터가 쌓이면 지면별 CTR과 정산액을 보고 조정한다.

## 6. 테스트

실제로 깨지기 쉬운 지점에 집중한다.

- 쿼리스트링이 이미 붙은 제휴 링크에 `subId` 병합
- 기간·상태 필터링 (미시작/만료/PAUSED 소재가 노출되지 않을 것)
- 가중 로테이션 분포
- 없는 code / 만료 code 클릭 시 랜딩으로 302
- 노출 집계 upsert가 같은 날 중복 호출에 누적될 것
