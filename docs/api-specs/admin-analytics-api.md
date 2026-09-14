# 관리자 지표 (Mixpanel · Sentry)

- `GET /api/v1/admin/analytics/mixpanel?from=YYYY-MM-DD&to=YYYY-MM-DD&events=A,B`: ADMIN 전용. 최대 366일.
- `GET /api/v1/admin/analytics/sentry?from=YYYY-MM-DD&to=YYYY-MM-DD`: ADMIN 전용. 최대 366일.
- 두 응답의 `status`: `NOT_CONFIGURED`, `CONNECTED`, `UNAVAILABLE`. 조회 실패를 0으로 대체하지 않는다.
- `NOT_CONFIGURED`는 오류가 아니다. 토큰을 넣기 전 상태이므로 화면은 지면을 비우고 안내만 띄운다.

## Mixpanel

집계 API가 아니라 **원본 이벤트를 내려받아 서버가 직접 센다.**

- `GET https://data.mixpanel.com/api/2.0/export?from_date&to_date`. 응답은 한 줄에 이벤트 하나인 NDJSON.
- 인증은 프로젝트 API 비밀을 Basic 사용자명 자리에 넣고 비밀번호를 비우는 레거시 방식이다. 이 방식에 `project_id`를 넣으면 400이 된다. 비밀이 이미 프로젝트를 특정한다.
- `events[].days[]`: `{date, count}`. 최신 날짜부터. 원본을 전부 받아 세므로 이벤트가 없던 날짜는 미집계가 아니라 **0**이다.
- `signUpDays[]`: 선택 기간 전체의 `sign_up` 일별 가입 수. 이벤트 선택값과 무관하게 집계하며 날짜 오름차순이다.
- `events`를 비우면 기간에 나타난 이벤트 전부를 발생 수 내림차순으로 준다. 이벤트 이름을 미리 설정해 둘 필요가 없다.
- 원본을 전부 받으므로 기간은 **31일까지**다(`MixpanelClient.MAX_DAYS`). 4일치가 약 2MB다.
- `properties.time`은 프로젝트 타임존 기준 epoch 초이고 `from_date`·`to_date` 경계도 같은 타임존을 따른다. 둘을 같은 타임존으로 묶어야 Mixpanel 화면 숫자와 맞는다. `picke.analytics.mixpanel.project-zone`(기본 `UTC`)로 맞춘다. 이 프로젝트는 UTC로 실측 확인했다.
- 경계 하루가 타임존 차이로 걸쳐 들어올 수 있어 요청 기간 밖 이벤트는 버린다.

### 왜 집계 API를 안 쓰는가

- **현재 Picke의 Mixpanel 플랜은 Query API를 허용하지 않는다.** 2026-09-14 실측: `/api/query/segmentation`·`/api/query/insights` 모두 `HTTP 402 Your plan does not allow API calls`. 인증은 통과하므로 자격 문제가 아니다.
- 같은 플랜에서 Raw Export는 **200으로 열려 있다.** 그래서 이쪽으로 붙였다. Mixpanel MCP나 다른 클라이언트를 붙여도 Query API를 호출하는 한 같은 402를 받는다.
- 서비스 계정 방식(`username:secret`)은 이 프로젝트에서 401이다. 프로젝트 토큰(`project_token`)은 이벤트 수집용이라 조회 인증에 쓰이지 않는다(401).
- 실측(2026-09-14, 09-10~09-13): 16종 이벤트, `screen_view` 644 · `network_request` 561 · `ui_action` 172 · `onboarding_step` 65 · `battle_step` 44 · `sign_up` 3 등.

### 환경변수

- `MIXPANEL_API_SECRET` 하나면 된다. 설정 키는 `picke.analytics.mixpanel.*`.
- EU·인도 데이터 거주 프로젝트는 호스트가 다르다. `picke.analytics.mixpanel.base-url`로 바꾼다.
- `ad_click` 이벤트에 `unit`·`placement`·`format` 속성이 붙어 온다. AdFit 클릭은 `unit` 없이 `format=popup`·`placement=app_start`로 들어온다. 광고 클릭을 매체별로 나누려면 이 속성을 쓴다.

## Sentry 사용자 이벤트

- iOS 분석 모듈은 Mixpanel에 보내는 모든 이벤트를 Sentry에도 info 이벤트로 전송하고 `analytics_event` 태그를 붙인다.
- `projects[].analyticsEvents.events[]`는 이 태그를 기준으로 이벤트명·기간 발생 수·고유 사용자·최초/마지막 발생·일별 발생 수/고유 사용자를 제공한다.
- 이벤트 이름은 서버에 하드코딩하지 않는다. 선택 기간에 실제 수집된 태그 값을 전부 조회하므로 `sign_up`, `ui_action` 등 새 액션도 자동으로 노출된다.
- Sentry Explore의 한 요청당 상위 시계열 10개 제한 때문에 이벤트명을 먼저 조회한 뒤 10개씩 나누어 일별 추이를 조회한다.
- `analyticsEvents.status`가 `UNAVAILABLE`이어도 오류·세션·릴리즈 등 다른 Sentry 데이터는 가능한 범위에서 유지한다.

## Sentry 오류

- `GET /api/0/projects/{org}/{project}/issues/?query=is:unresolved&sort=freq`. 조직 인증 토큰 `Authorization: Bearer`.
- 절대 기간을 쓰려면 `statsPeriod`를 빈 값으로 함께 보낸다. 생략하면 Sentry 기본 기간이 적용된다.
- iOS·Android를 각각 호출해 `projects[]`로 나눠 준다.
- 한 프로젝트가 막혀도 다른 프로젝트는 살린다. 대신 최상위 `status`는 `UNAVAILABLE`, 최상위 `totalEvents`는 `null`이다. 일부만 더한 값을 전체 합계처럼 보여주지 않는다.
- `projects[].totalEvents`는 stats API의 선택 기간 전체 수신 이벤트 합계다. `issues[]`는 미해결 상위 20건이다.
- `sort=freq`는 절대 기간에서 이벤트 수 내림차순을 보장하지 않는다(실측 확인). 응답 순서를 믿지 않고 서버가 다시 정렬한다.
- `issues[].events`는 Sentry가 문자열로 주기도 한다. 숫자·문자열 모두 읽는다.
- `recentEvents[]`는 `/events/?full=true`의 최근 오류 이벤트 최대 10건이다. 자주 쓰는 필드를 정규화하고 `details`에 사용자·브레드크럼·컨텍스트·예외·스택트레이스 등 전체 JSON을 보존한다.
- `datasets[]`는 Picke-iOS에서 활성화한 `errors`, `logs`, `spans`, `profile_functions`, `tracemetrics` 데이터셋의 일별 발생량이다. 각 데이터셋 실패는 프로젝트 핵심 오류/이슈 조회를 실패시키지 않는다.
- `metricCatalog`는 커스텀 메트릭의 이름·타입·단위·건수·마지막 수집 시각과 컨텍스트를 원본 형태로 준다.
- `sessionHealth`는 자동 세션 추적의 `healthy`, `errored`, `crashed` 등 상태별 합계와 일별 시리즈를 준다.
- `releases`는 프로젝트 최근 릴리즈 최대 20개의 버전·빌드·커밋·배포·상태 원본을 준다.
- iOS 설정에서 Session Replay는 샘플 비율이 0으로 꺼져 있으므로 Replay 녹화 데이터는 조회하지 않는다.
- 환경변수: `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`(기본 `picke`), `SENTRY_PROJECTS`(기본 `picke-ios,picke-android`). 설정 키는 `picke.analytics.sentry.*`.
- Explore·메트릭·세션 API는 `org:read`, 오류 이벤트는 `event:read`, 프로젝트·릴리즈 조회는 `project:read` 또는 해당 상위 권한이 필요하다.
- 앱은 이미 같은 org·project로 Sentry에 리포트한다. 토큰만 서버에 넣으면 같은 데이터를 읽는다.
- 실측(2026-09-14, 최근 30일): `picke-ios` 미해결 2건·1,765 이벤트, `picke-android` 9건·181 이벤트.

## 공통

- 외부 호출 실패가 관리자 화면 전체를 500으로 만들지 않는다. 타임아웃은 연결 3초, 요청 10초다.
- 토큰은 응답에 담지 않는다. 서버 설정에만 둔다.
