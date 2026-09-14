# 관리자 지표 (Mixpanel · Sentry)

- `GET /api/v1/admin/analytics/mixpanel?from=YYYY-MM-DD&to=YYYY-MM-DD&events=A,B`: ADMIN 전용. 최대 366일.
- `GET /api/v1/admin/analytics/sentry?from=YYYY-MM-DD&to=YYYY-MM-DD`: ADMIN 전용. 최대 366일.
- 두 응답의 `status`: `NOT_CONFIGURED`, `CONNECTED`, `UNAVAILABLE`. 조회 실패를 0으로 대체하지 않는다.
- `NOT_CONFIGURED`는 오류가 아니다. 토큰을 넣기 전 상태이므로 화면은 지면을 비우고 안내만 띄운다.

## Mixpanel

- Query API `GET /api/query/segmentation`. 서비스 계정 Basic 인증이며 모든 요청에 `project_id`가 필요하다.
- 이벤트 이름은 서버가 모른다. 앱이 무엇을 트래킹하는지에 달렸으므로 `events` 요청 파라미터 또는 `picke.analytics.mixpanel.default-events` 설정으로 받는다. 둘 다 비면 `NOT_CONFIGURED`.
- `events[].days[]`: `{date, count}`. 최신 날짜부터. Mixpanel이 값을 주지 않은 날짜는 `null`이며 0이 아니다.
- 이벤트 여러 개를 요청했을 때 하나라도 실패하면 전체가 `UNAVAILABLE`이다. 일부 합계를 전체처럼 보여주지 않는다.
- segmentation은 Mixpanel이 유지보수 모드로 둔 엔드포인트다. 저장된 리포트(bookmark)를 미리 만들 필요가 없어 날짜만 바꿔 조회하는 관리자 화면에 맞다. 막히면 Insights Query API로 옮긴다.
- 환경변수: `MIXPANEL_PROJECT_ID`, `MIXPANEL_SERVICE_ACCOUNT_USERNAME`, `MIXPANEL_SERVICE_ACCOUNT_SECRET`. 설정 키는 `picke.analytics.mixpanel.*`.
- EU·인도 데이터 거주 프로젝트는 호스트가 다르다. `picke.analytics.mixpanel.base-url`로 바꾼다.

## Sentry

- `GET /api/0/projects/{org}/{project}/issues/?query=is:unresolved&sort=freq`. 조직 인증 토큰 `Authorization: Bearer`.
- 절대 기간을 쓰려면 `statsPeriod`를 빈 값으로 함께 보낸다. 생략하면 Sentry 기본 기간이 적용된다.
- 이벤트 수 내림차순 최대 20건. `totalEvents`는 그 20건의 합계이며 프로젝트 전체 합계가 아니다.
- `issues[].events`는 Sentry가 문자열로 주기도 한다. 숫자·문자열 모두 읽는다.
- 환경변수: `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`(현재 `picke`), `SENTRY_PROJECT`(현재 `picke-ios`). 설정 키는 `picke.analytics.sentry.*`.
- iOS 앱은 이미 같은 org·project로 Sentry에 리포트한다. 토큰만 서버에 넣으면 같은 데이터를 읽는다.

## 공통

- 외부 호출 실패가 관리자 화면 전체를 500으로 만들지 않는다. 타임아웃은 연결 3초, 요청 10초다.
- 토큰은 응답에 담지 않는다. 서버 설정에만 둔다.
