# 관리자 AdFit 광고와 ROI

- `GET /api/v1/admin/adfit?from=YYYY-MM-DD&to=YYYY-MM-DD`: ADMIN 전용. 최대 366일.
- `PUT /api/v1/admin/adfit/daily`: ADMIN 전용. `{date, unit, revenue, cost, costBasis}`.
- `unit`: NATIVE_WIDE(홈·큐레이션·마이페이지 공유), BANNER(탐색), APP_TRANSITION(앱 시작).
- `costBasis`: AD_OPERATIONS(광고 운영비), ACQUISITION(유입 광고비), SERVICE_OPERATIONS(서비스 운영비).
- KRW 금액, 소수 둘째 자리까지, 음수 불가. 같은 날짜·단위는 수정. 미래 날짜 입력 불가.
- source=MANUAL_CONSOLE: AdFit 콘솔에서 확인한 예상 수익을 관리자가 입력한다. 자동 연동·확정 정산액이 아니다.
- 광고 단위는 로컬 Picke-iOS의 SDK 연결 기준이다. 개별 사용자에게 SDK가 선택한 이미지·광고주 소재를 재현하거나 실시간 노출을 보증하지 않는다.
- 같은 단위를 여러 화면에서 사용하더라도 수익은 한 번만 합산한다. 비용도 단위별 배분액으로 입력하며 전체 운영비를 각 단위에 중복 입력하지 않는다.
- 수익과 비용은 입력된 날짜들의 합계다. reportedDays/expectedDays로 부분 입력을 표시한다. 미입력은 null이며 0원이 아니다.
- ROI = (수익 - 비용) / 비용 × 100. 모든 날짜가 입력되고 같은 비용 기준이며 비용이 양수일 때만 계산한다. 그 외 null.
- 수익 0, 비용 양수인 정상 입력은 ROI -100%다. 미입력과 구분한다.
- 공식 공개 보고서 REST API는 확인하지 못했으므로 비공개 API를 추측하거나 관리자 브라우저에 인증 정보를 저장하지 않는다.
- 공식 참고: https://adfit.kakao.com/ , https://adfit.github.io/
- DB: `docs/db/20260910_create_adfit_daily_reports.sql`. 현재 프로젝트는 Hibernate ddl-auto=update를 사용한다.
