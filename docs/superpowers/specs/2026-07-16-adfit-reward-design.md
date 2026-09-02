# 애드핏 리워드 크레딧 설계

작성일: 2026-07-16
대상 브랜치: `dev`
상태: 설계 확정 대기

## 1. 배경

현재 서버는 AdMob 리워드 광고 + SSV(서버 사이드 검증)로 크레딧을 지급한다. 유저가 리워드 광고를 끝까지 보면 구글이 서버로 서명된 콜백을 보내고, 서버가 Tink `RewardedAdsVerifier`로 검증한 뒤 `FREE_CHARGE`(20 크레딧)를 적립한다.

구현 자체는 정상 동작하나 **AdMob 계정 승인이 거부되어 광고를 받을 수 없다.** 앱은 스토어에 출시되어 있고 심사도 통과한 상태다. 승인 사유 규명 대신 광고 네트워크를 교체하기로 결정했다.

### 대체 네트워크 조사 결과

| 네트워크 | 리워드 상품 | S2S 검증 | 결론 |
|---|---|---|---|
| 네이버 GFA | 없음 (광고주 전용 플랫폼) | 해당 없음 | 불가 — 매체용 SDK 자체가 없음 |
| 카카오 애드핏 | **없음** (배너/네이티브/비즈보드/앱전환/앱종료) | **없음** | 리워드 상품은 없으나 정책상 매체 자체 구현을 상정 |
| Unity LevelPlay | 있음 | 있음 (MD5 공유 시크릿) | 가능하나 승인 수 주 소요, 지연 사례 다수 |
| AppLovin MAX | 있음 | 있음 (SHA1 공유 시크릿) | 가능하나 인디 거절 보고 다수 |

국내 네트워크는 리워드 + S2S를 제공하지 않는다. 이는 글로벌 네트워크의 영역이다.

**애드핏을 선택했다.** 개인 자격으로 사업자등록 없이 등록 가능하고 승인 문턱이 낮아, 승인이 수 주 걸리고 결과도 불확실한 글로벌 네트워크보다 빠르게 수익화를 재개할 수 있다.

### 애드핏 리워드의 정책적 근거

애드핏은 리워드 SDK 포맷을 제공하지 않지만, [서비스 운영정책](https://adfit.kakao.com/web/html/use_kakao.html) 5.3.2~5.3.3은 리워드 동영상 광고를 명시적으로 규율한다:

- 5.3.2 — 리워드 광고는 사용자의 명확한 행동(버튼 클릭 등)이 있는 경우에만 노출
- 5.3.3 — 보상 조건, 지급 여부/시점, **지급 제외 사유(시청 중단, 중복 시청, 부정 시청)** 를 유저에게 명확히 고지

지급 제외 사유를 매체가 안내하라는 조항은 **매체가 직접 리워드 로직을 구현하는 형태를 전제**한 것으로 읽힌다. 즉 애드핏은 리워드 포맷과 검증 콜백을 제공하지 않을 뿐, 매체가 광고를 노출하고 자체적으로 보상을 지급하는 것을 금지하지 않는 것으로 해석한다.

**이 해석은 공개 문서만으로는 확정할 수 없다.** 상품 카탈로그에 리워드가 없는데 정책에는 규율이 있는 모순이 존재한다. 리스크 항목(11절)을 참조.

## 2. 목표 / 비목표

### 목표

- "광고를 끝까지 보면 크레딧 20 지급" UX를 유지한다.
- AdMob 의존을 완전히 제거한다 (폴백 유지하지 않음).
- S2S 검증이 없는 환경에서 남용을 **억제**한다.
- 기존 크레딧 지급 파이프라인(`AdRewardHistory`, `CreditService`)을 그대로 재사용한다.

### 비목표

- 애드핏 배너/전면 광고 자체의 수익화 — 클라이언트 전용 작업이며 서버 변경 없음
- `CreditService` / `CreditType` / 배치 잡 변경
- 광고 노출 원격 스위치 (remote config)
- 글로벌 리워드 네트워크(LevelPlay/AppLovin) 연동 — 추후 필요 시 별도 설계
- 완전한 광고 시청 증명 — 애드핏이 S2S를 제공하지 않는 한 불가능

### 성공 기준

1. 인증된 유저가 애드핏 전면 광고를 본 뒤 크레딧 20을 정확히 1회 받는다.
2. 동일 티켓으로 두 번 청구하면 두 번째는 거부되고 크레딧이 증가하지 않는다.
3. 광고를 보지 않고 티켓 발급 즉시 청구하면 거부된다.
4. 하루 한도 초과 시 거부된다.
5. AdMob 관련 코드/설정/의존성이 저장소에 남아있지 않고 빌드가 통과한다.

## 3. 핵심 설계 결정

### 3.1 티켓 방식을 채택한다

애드핏은 S2S 콜백을 주지 않으므로 서버가 "광고를 봤다"를 증명할 수 없다. 클라이언트 신고를 받되, 자동화 난이도를 올려 남용을 억제한다.

```
1. POST /api/v1/reward/adfit/ticket   (JWT) → 티켓 발급 (UUID)
2. 앱: 애드핏 전면(앱전환) 광고 표시
3. 광고 닫힘 콜백 수신
4. POST /api/v1/reward/adfit/claim    (JWT) {ticketId} → 크레딧 20
```

티켓 없이 `/claim` 단일 엔드포인트로 만들면 광고와 무관한 "누르면 20크레딧" API가 된다. 티켓 + 최소 경과시간은 S2S 없이 취할 수 있는 최선의 억제책이다.

### 3.2 JWT 인증을 사용한다 — AdMob 대비 개선점

기존 AdMob SSV 엔드포인트는 구글이 호출해야 하므로 **비인증으로 열려 있고**(`SecurityConfig.java:49`), 유저를 `custom_data` 문자열로 식별했다. 이는 임의 유저 태그를 넣어 **사칭이 가능한 구조**다.

애드핏 방식은 우리 앱이 직접 호출하므로 JWT 인증을 태울 수 있다. 유저 식별은 토큰에서 추출하며 **사칭이 원천 차단된다.** 즉 "누구인가"의 신뢰도는 올라가고, "광고를 봤는가"의 신뢰도만 내려간다.

### 3.3 `provider` 컬럼을 두지 않는다

AdMob을 완전히 제거하므로 네트워크 구분이 불필요하다. 애드핏 티켓은 UUID라 기존 `ad_reward_history.transaction_id` unique 제약과 충돌하지 않는다.

추후 다른 네트워크를 추가할 때 `provider` 컬럼을 도입한다. 지금 넣는 것은 YAGNI다.

### 3.4 스키마 마이그레이션 스크립트가 필요 없다

`ad_reward_ticket`은 신규 테이블이므로 `ddl-auto: update`가 자동 생성한다. `ad_reward_history`는 변경하지 않는다.

단, 일일 한도 조회 성능을 위한 인덱스는 `ddl-auto`가 만들지 않으므로 선택적으로 `db/migration/` 컨벤션에 따라 추가한다(6.3절).

### 3.5 크레딧 금액은 서버 고정값을 쓴다

기존 정책을 유지한다. 클라이언트가 보낸 어떤 금액도 신뢰하지 않고 `CreditType.FREE_CHARGE.getDefaultAmount()`(20)만 지급한다.

## 4. 아키텍처

```
AdFitRewardController  ── JWT 인증 필수
        │
        ▼
AdFitRewardService (interface + Impl)   ← 기존 컨벤션(인터페이스 분리) 준수
        │
        ├── AdRewardTicketRepository     티켓 발급/검증/사용 처리
        ├── AdRewardHistoryRepository    중복 방지 + 이력 (기존 재사용)
        ├── UserService                  findCurrentUser() (기존 재사용)
        └── CreditService                크레딧 적립 (기존 재사용, 무변경)
```

기존 `AdMobRewardServiceImpl.processReward()`의 지급 로직(50~77행: 중복 방지 → 유저 조회 → 이력 저장 → 크레딧 적립)을 `AdFitRewardServiceImpl`로 이관한다. AdMob 고유의 서명 검증(38~48행)은 폐기하고, 그 자리에 티켓 검증이 들어간다.

`RewardGrantService` 같은 별도 공용 서비스는 만들지 않는다. 네트워크가 하나뿐이므로 추상화의 실익이 없다.

## 5. API 명세

### 5.1 티켓 발급

```
POST /api/v1/reward/adfit/ticket
Authorization: Bearer <JWT>
```

응답 200:
```json
{
  "isSuccess": true,
  "result": {
    "ticketId": "9f1c8e2a-...",
    "expiresInSeconds": 300
  }
}
```

- 유저당 미사용 티켓이 이미 있으면 기존 티켓을 재발급하지 않고 신규 발급한다. 미사용 티켓은 만료로 자연 정리된다.
- **일일 한도 초과 시 이 단계에서도 거부한다.** 광고를 보여준 뒤 청구에서 거부하면 유저가 광고만 보고 보상을 못 받는 최악의 UX가 된다. 단 이는 UX 목적의 사전 차단이며, **실제 한도 강제는 청구 시점에 이루어진다**(7절).

### 5.2 크레딧 청구

```
POST /api/v1/reward/adfit/claim
Authorization: Bearer <JWT>
Content-Type: application/json

{ "ticketId": "9f1c8e2a-..." }
```

응답 200:
```json
{
  "isSuccess": true,
  "result": { "rewardedAmount": 20, "totalCredit": 145 }
}
```

기존 `ApiResponse` 래퍼를 그대로 사용한다. (AdMob과 달리 외부 네트워크가 응답 포맷을 강제하지 않는다.)

## 6. 데이터 모델

### 6.1 `ad_reward_ticket` (신규)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `id` | bigint PK | `BaseEntity` 상속 |
| `ticket_id` | varchar unique, not null | UUID |
| `user_id` | bigint FK, not null | |
| `used_at` | timestamp nullable | 사용 시각. null이면 미사용 |
| `created_at` / `updated_at` | timestamp | `BaseEntity` 제공 |

발급 시각은 `BaseEntity.createdAt`을 그대로 쓴다. 별도 `issued_at`을 두지 않는다.

### 6.2 `ad_reward_history` (기존, 무변경)

`transaction_id`에 티켓 UUID를 저장한다. 기존 unique 제약이 중복 청구 방지의 최종 방어선으로 그대로 동작한다.

### 6.3 인덱스 (선택)

일일 한도 조회는 `ad_reward_history`를 `user_id` + `created_at` 범위로 집계한다. 트래픽이 늘면 복합 인덱스를 추가한다:

```sql
-- db/migration/V20260716_01__add_ad_reward_history_user_created_idx.sql
CREATE INDEX IF NOT EXISTS idx_ad_reward_history_user_created
    ON ad_reward_history (user_id, created_at);
```

초기 규모에서는 불필요할 수 있다. 도입 여부는 구현 시 판단한다.

## 7. 검증 및 남용 억제

`/claim` 처리 순서:

| 순서 | 검증 | 실패 시 |
|---|---|---|
| 1 | JWT에서 userId 추출 | 401 (기존 `JwtFilter`) |
| 2 | 티켓 존재 | `REWARD_TICKET_NOT_FOUND` |
| 3 | 티켓 소유자 == 요청자 | `REWARD_TICKET_NOT_FOUND` (존재 여부를 노출하지 않음) |
| 4 | 미사용 (`used_at IS NULL`) | `REWARD_TICKET_ALREADY_USED` |
| 5 | 만료 전 (발급 후 5분 이내) | `REWARD_TICKET_EXPIRED` |
| 6 | 발급~청구 간격 ≥ 5초 | `REWARD_TICKET_TOO_SOON` |
| 7 | 일일 한도 미초과 (**실제 강제 지점**) | `REWARD_DAILY_LIMIT_EXCEEDED` |
| 8 | `transaction_id` 중복 아님 | 멱등 응답 (재지급 없음) |

### 일일 한도는 발급과 청구 양쪽에서 검사한다

발급 시에만 검사하면 우회된다. 청구 0회 상태에서 티켓 20개를 연속 발급하면 매 발급이 한도 검사를 통과하고(청구 횟수가 0이므로), 이후 20개를 모두 청구해 한도의 2배를 받을 수 있다.

- **발급 시 검사** — UX 목적. 광고를 보여주기 전에 미리 차단해, 유저가 광고만 보고 보상을 못 받는 상황을 막는다.
- **청구 시 검사** — 보안 목적. 한도를 실제로 강제하는 지점. 미사용 티켓 재고와 무관하게 당일 청구 횟수를 기준으로 판단한다.

한도 산정 기준은 `ad_reward_history`의 당일 적립 건수이며, 티켓 발급 건수가 아니다.

### 파라미터 기본값

| 값 | 기본 | 근거 |
|---|---|---|
| 일일 한도 | **10회** | 20크레딧 × 10 = 200/일. 출석 5/일, 배틀 진입 −5인 경제에서 충분히 넉넉하며 남용 피해 상한을 고정한다. |
| 최소 경과시간 | **5초** | 전면 광고 로드+노출에 최소한 소요되는 시간. 즉시 청구 자동화를 차단한다. |
| 티켓 만료 | **5분** | 광고 로드 실패/유저 이탈 시 티켓이 무한정 남지 않게 한다. |

세 값 모두 `application.yml`의 `adfit.reward.*`로 외부화하여 코드 수정 없이 조정한다. **최소 경과시간은 실제 애드핏 전면 광고 길이를 앱에서 측정한 뒤 조정이 필요하다.**

### 동시성

같은 티켓으로 동시에 두 번 청구하는 경쟁 조건은 `ad_reward_history.transaction_id`의 unique 제약이 최종 방어한다. 두 요청이 모두 검증을 통과해도 `saveAndFlush` 시점에 한쪽이 `DataIntegrityViolationException`으로 실패하고 롤백되므로 **이중 지급은 발생하지 않는다.**

**이 예외를 잡지 않는다.** `@Transactional` 내부에서 잡아도 트랜잭션이 이미 rollback-only로 마킹되어 있어, 멱등 응답을 반환하려면 `REQUIRES_NEW` 분리 등의 복잡도가 필요하다. 얻는 것에 비해 비용이 크다:

- 안전성은 이미 unique 제약으로 확보되어 있다 (이중 지급 없음).
- 동일 티켓 동시 청구는 정상 클라이언트에서 발생하지 않는다. 광고 시청 후 1회 호출하는 흐름이기 때문이다.
- 즉 이 경로를 타는 것은 사실상 공격자뿐이며, 공격자가 500을 받는 것은 문제가 아니다.

순차적 중복 청구(네트워크 재시도 등 정상 케이스)는 티켓 `used_at`과 `existsByTransactionId` 검사가 먼저 잡아내므로 정상적인 에러/멱등 응답을 받는다.

참고로 `CreditService.addCredit`도 `(user, creditType, referenceId)` 중복 시 조용히 무시하는 멱등 구현이나(`CreditService.java:60`), `referenceId`로 매번 새로운 `history.getId()`를 넘기므로 이 방어선은 본 흐름에서 작동하지 않는다. 중복 방어는 `transaction_id` unique에 의존한다.

## 8. 에러 처리

`ErrorCode`에 추가:

```java
REWARD_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "REWARD_404_2", "유효하지 않은 티켓입니다."),
REWARD_TICKET_ALREADY_USED(HttpStatus.CONFLICT, "REWARD_409", "이미 사용된 티켓입니다."),
REWARD_TICKET_EXPIRED(HttpStatus.GONE, "REWARD_410", "만료된 티켓입니다."),
REWARD_TICKET_TOO_SOON(HttpStatus.BAD_REQUEST, "REWARD_400_2", "광고 시청이 완료되지 않았습니다."),
REWARD_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "REWARD_429", "오늘 받을 수 있는 광고 보상을 모두 받았습니다."),
```

제거: `REWARD_INVALID_SIGNATURE` (AdMob 전용)

AdMob SSV는 스펙상 실패해도 200을 반환해야 했으나(`AdMobRewardController.java:43-46`), 애드핏은 우리 앱이 호출하므로 **정상적인 HTTP 에러 코드를 반환한다.** 클라이언트가 사유별로 다른 안내를 띄울 수 있다.

## 9. AdMob 제거 범위

### 삭제

- `global/config/AdMobConfig.java`
- `domain/reward/controller/AdMobRewardController.java`
- `domain/reward/service/AdMobRewardService.java`, `AdMobRewardServiceImpl.java`
- `domain/reward/dto/request/AdMobRewardRequest.java`
- `domain/reward/dto/response/AdMobRewardResponse.java`
- `test/.../domain/reward/service/AdMobRewardServiceTest.java`

### 수정

| 파일 | 변경 |
|---|---|
| `build.gradle:51-53` | Tink 의존성 2줄 제거 |
| `application.yml:76-81` | `admob.*` 블록 제거 |
| `SecurityConfig.java:49` | `/api/v1/admob/reward/**` permitAll 제거. 애드핏 엔드포인트는 **permitAll에 추가하지 않는다** (인증 필수) |
| `JwtFilter.java:30` | `/api/v1/admob/reward` 제외 항목 제거 |
| `SwaggerConfig.java:88,97` | `/api/v1/admob/**` 2곳 제거 |
| `ErrorCode.java:110` | `REWARD_INVALID_SIGNATURE` 제거 |
| `docs/api-specs/reward-api.md` | 애드핏 API로 재작성 |
| `static/app-ads.txt` | AdMob 항목 → 애드핏 항목으로 교체 (운영 작업, 애드핏 승인 후) |

### 유지

`AdRewardHistory`, `AdRewardHistoryRepository`, `RewardItem`, `CreditService`, `CreditType`, `UserService`, `StaticTextFileController`(app-ads.txt 서빙 자체는 애드핏도 필요)

### 환경변수 정리

`ADMOB_APP_ID`, `ADMOB_REWARD_UNIT_ID_IOS`, `ADMOB_REWARD_UNIT_ID_ANDROID` — 배포 환경에서 제거

## 10. 테스트 전략

기존 `AdMobRewardServiceTest`의 구조(Mockito 단위 테스트)를 참고하되 대상은 `AdFitRewardServiceImpl`이다.

- 정상 흐름: 티켓 발급 → 5초 경과 → 청구 → 크레딧 20 적립, `used_at` 기록
- 중복 청구: 같은 티켓 2회 → 두 번째 거부, 크레딧 불변
- 타인 티켓: 다른 유저의 티켓 청구 → 거부
- 만료: 5분 초과 → 거부
- 조기 청구: 5초 미만 → 거부
- 일일 한도 (발급): 한도 도달 시 티켓 발급 거부
- 일일 한도 (청구, 우회 방지): 한도 미달 상태에서 티켓을 한도 이상 미리 발급받아 두고 전부 청구해도 한도까지만 지급
- 멱등: 이미 지급 이력이 있는 티켓 → 재지급 없이 응답
- 회귀: AdMob 제거 후 전체 빌드 및 기존 테스트 통과

동시성 경쟁 조건은 unique 제약에 의존하며 단위 테스트로 검증하지 않는다(7절 참조).

## 11. 한계 및 리스크

### 광고 시청을 증명할 수 없다 (수용)

애드핏이 S2S 콜백을 제공하지 않으므로 근본적으로 해결 불가능하다. 티켓/최소시간/일일한도는 **자동화 난이도를 올리는 억제책**이지 증명이 아니다. 앱을 리버스 엔지니어링하면 티켓 발급 후 5초 대기 후 청구하는 스크립트를 만들 수 있다.

**피해 규모는 제한적이다.** 크레딧은 현금화 경로가 없고(IAP 없음) 소비처가 배틀 진입(−5)과 주제 제안(−100)뿐이다. 위조의 이득은 "배틀을 더 하는 것"이며 금전적 손실이 아니다. 일일 한도가 피해 상한을 200크레딧/일/유저로 고정한다.

이 트레이드오프는 애드핏을 선택한 대가다. 리워드 + S2S가 필요하면 글로벌 네트워크(LevelPlay/AppLovin)로 가야 하며, 그 경우 승인에 수 주가 걸리고 결과도 불확실하다.

### 애드핏 정책 해석이 확정적이지 않다 (미해결)

1절의 해석 — 애드핏이 매체 자체 리워드 구현을 허용한다 — 은 운영정책 조항으로부터의 추론이며 공식 확인이 아니다. 상품 카탈로그에 리워드가 없는데 정책에 규율이 있는 모순이 존재한다.

**완화책:** 구현 착수 전 카카오 고객센터에 "리워드 형태(광고 시청 완료 시 앱 내 재화 지급)로 애드핏 전면 광고를 사용해도 되는지"를 문의한다. 답변에 따라:

- 허용 → 그대로 진행
- 불허 → 애드핏은 배너 전용으로 축소하고 리워드는 글로벌 네트워크로 재검토 (본 설계 폐기)

문의 없이 진행할 경우 최악의 시나리오는 애드핏 계정 정지이며, AdMob도 막힌 상태라 수익원이 0이 된다. **문의 비용이 며칠인 데 비해 리스크가 크므로 선행을 권장한다.**

### 최소 경과시간이 실측 기반이 아니다

5초는 추정치다. 애드핏 전면 광고가 이보다 짧으면 정상 유저가 거부당하고, 훨씬 길면 억제 효과가 약해진다. 앱 연동 후 실제 광고 길이를 측정해 조정한다.

## 12. 미결 사항

| 항목 | 결정 필요 시점 |
|---|---|
| 카카오 정책 문의 결과 | 구현 착수 전 (권장) |
| 최소 경과시간 실측값 | 앱 연동 후 |
| 일일 한도 10회 적정성 | 운영 데이터 확인 후 |
| `ad_reward_history` 복합 인덱스 도입 | 트래픽 증가 시 |
