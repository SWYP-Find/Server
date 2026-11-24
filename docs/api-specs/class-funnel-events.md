# 클래스 퍼널 Mixpanel 이벤트 (서버 전송, #689)

클래스 API 가 성공하면 서버가 Mixpanel Import API 로 보낸다. 앱 수정 없이 iOS·Android 를 같은 기준으로 집계한다.

## 켜기

| env | 설명 |
|---|---|
| `MIXPANEL_TRACKING_ENABLED=true` | 전송 켜기. **기본 false**. dev·prod 서버가 같은 Mixpanel 프로젝트를 쓰므로 운영에만 켠다 |
| `MIXPANEL_API_SECRET` | 레거시 인증 (2027-03-03 제거 예정) |
| `MIXPANEL_SERVICE_ACCOUNT_USERNAME` · `_SECRET` · `MIXPANEL_PROJECT_ID` | 서비스 계정 인증 (모두 있으면 우선) |

- 호출한 트랜잭션이 **커밋된 뒤 비동기**로 보낸다. 롤백되면 안 보내고, 전송 실패는 WARN 로그만 남긴다 (재시도 없음)
- `distinct_id` = `userTag` (앱이 로그인 때 identify 하는 값) → 앱 이벤트와 같은 사용자로 합쳐진다
- `$insert_id` 로 중복 전송은 Mixpanel 이 합친다

## 공통 속성

| 속성 | 값 |
|---|---|
| `class_id` | 클래스 ID (개설 시작은 없음) |
| `battle_id` | 클래스 배틀 ID |
| `battle_type` | `EXISTING`(기존 콘텐츠) / `CUSTOM` |
| `role` | `OWNER` / `MEMBER` |
| `capacity` | 이용권 정원 |
| `owner_participates` | 운영자 참여 여부 |
| `source` | `server` |

## 이벤트

| 이벤트 | 시점 (API 성공) | 추가 속성 |
|---|---|---|
| `class_create_start` | `GET /class-battle-recommendations` (기존) · `POST /class-topic-drafts` (커스텀) | `battle_type`, `entry`(recommendation / topic_draft) |
| `class_create_complete` | `POST /classes` | |
| `class_join_preview` | `GET /classes/join/preview` (코드 확인) | |
| `class_join_complete` | `POST /classes/join` | |
| `class_vote_pre` | `POST /classes/{id}/battle/votes/pre` | |
| `class_tts_complete` | `POST .../votes/tts-complete` | |
| `class_vote_mid` | `POST .../votes/mid` | |
| `class_debate_start` | `POST .../battle/debate` (AI 첫 주장) | |
| `class_debate_reply` | `POST .../battle/debate/replies` (반박 제출) | `round` |
| `class_vote_post` | `POST .../votes/post` | |
| `class_opinion_write` | `POST .../battle/perspectives` | |
| `class_comment_write` | 클래스 의견에 댓글 작성 | |
| `class_result_view` | `GET /classes/{id}/result` | |
| `class_report_view` | 완성된(SUCCEEDED/LIMITED) 리포트 조회: 참가자 `GET .../result/my-report`, 운영자 `GET .../result/insight` | `report_type`(MEMBER / INSIGHT), `report_state`, `first_view` |

## 리포트 열람 기록

`class_report_views` (class_id, report_id, user_id, first_viewed_at, last_viewed_at, view_count). 사용자·리포트당 1행. 클래스 원문 파기 때 함께 지운다.

## 퍼널 예 (어드민 `/admin/analytics/mixpanel/funnel`)

- P0 클래스 퍼널 (`groupBy=class_id`): `class_create_complete → class_join_complete → class_vote_post → class_report_view`
- 방식별: 각 단계에 `:battle_type=CUSTOM` / `:battle_type=EXISTING`
- 커스텀 구간: `class_join_complete → class_vote_pre → class_debate_start → class_debate_reply:round=1 → class_debate_reply:round=2 → class_debate_reply:round=3 → class_vote_post → class_report_view:report_type=MEMBER`
- 기존 콘텐츠: `class_join_complete → class_vote_pre → class_tts_complete → class_vote_post → class_opinion_write → class_report_view:report_type=MEMBER`
