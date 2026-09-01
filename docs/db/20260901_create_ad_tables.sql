-- 제휴 광고(쿠팡 파트너스 / 애드픽) 소재·클릭·노출 테이블
-- 관련 설계: docs/superpowers/specs/2026-09-01-ad-picke-store-design.md
--
-- 이 파일은 참고용이다. 운영은 spring.jpa.hibernate.ddl-auto=update 라 배포 시 자동 생성된다.
-- 스키마를 손으로 관리하는 환경이나 사후 검증이 필요할 때 쓴다.

CREATE TABLE IF NOT EXISTS ad_creatives (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(16)  NOT NULL UNIQUE,
    network     VARCHAR(20)  NOT NULL,
    slot        VARCHAR(40)  NOT NULL,
    title       VARCHAR(100) NOT NULL,
    subtitle    VARCHAR(200),
    image_url   VARCHAR(500) NOT NULL,
    cta_text    VARCHAR(30)  NOT NULL,
    landing_url VARCHAR(1000) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    weight      INTEGER      NOT NULL DEFAULT 1,
    starts_at   TIMESTAMP,
    ends_at     TIMESTAMP,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

-- 지면 조회는 (slot, status)로만 들어온다.
CREATE INDEX IF NOT EXISTS idx_ad_creatives_slot_status ON ad_creatives (slot, status);

CREATE TABLE IF NOT EXISTS ad_click_logs (
    id          BIGSERIAL PRIMARY KEY,
    creative_id BIGINT      NOT NULL,
    slot        VARCHAR(40) NOT NULL,
    ip_hash     VARCHAR(64),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ad_click_logs_creative ON ad_click_logs (creative_id);
CREATE INDEX IF NOT EXISTS idx_ad_click_logs_created_at ON ad_click_logs (created_at);

-- 노출은 raw 로그로 쌓지 않는다. 배너가 스크롤에 걸릴 때마다 행이 생기면 금방 수천만 건이 된다.
CREATE TABLE IF NOT EXISTS ad_impression_daily (
    id          BIGSERIAL PRIMARY KEY,
    creative_id BIGINT      NOT NULL,
    slot        VARCHAR(40) NOT NULL,
    stat_date   DATE        NOT NULL,
    impressions BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uk_ad_impression_daily UNIQUE (creative_id, slot, stat_date)
);

CREATE INDEX IF NOT EXISTS idx_ad_impression_daily_date ON ad_impression_daily (stat_date);
