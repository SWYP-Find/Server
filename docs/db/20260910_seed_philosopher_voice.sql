-- 철학자 → Fish Audio 보이스(reference_id) 초기 매핑 시드
-- 관련 설계: 배틀_발행_시나리오_현행_vs_개선.md 2.5
--
-- 테이블 자체는 spring.jpa.hibernate.ddl-auto=update 가 PhilosopherVoice 엔티티를 보고 생성한다.
-- 이 스크립트는 초기 데이터만 넣는다. 환경별(dev / prod / local)로 1회 실행.
-- ON CONFLICT DO NOTHING 이라 재실행해도 안전하고, 어드민에서 수정한 값을 덮지 않는다.
-- 이후 매핑 변경/추가는 어드민 CRUD(/api/v1/admin/philosopher-voices)로 한다.

INSERT INTO philosopher_voice (name, reference_id, voice_label, created_at, updated_at) VALUES
  ('소크라테스',   '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),
  ('스마일즈',     '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),
  ('플라톤',       '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),
  ('톨스토이',     '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),
  ('맹자',         '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),
  ('공자',         '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),
  ('칙센트미하이', '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),
  ('히포크라테스', '02f1c443c12b4f1fa0777983d07e64af', '정감 있는 이야기꾼(남자 노인)', now(), now()),

  ('아리스토텔레스', '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('벤담',         '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('한비자',       '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('피터싱어',     '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('볼테르',       '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('푸코',         '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('토크빌',       '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('베카리아',     '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('스피노자',     '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('애덤 그랜트',  '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('버크',         '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('쇼펜하우어',   '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('노자',         '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('보르헤스',     '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),
  ('월드런',       '87d3970929bf47e19cf38472eda5764d', '한국 성우', now(), now()),

  ('홉스',         '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('비트겐슈타인', '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('도스토옙스키', '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('니체',         '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('밀',           '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('뒤르켐',       '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('베이컨',       '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('존 로크',      '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('루소',         '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('에피쿠로스',   '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('사르트르',     '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('간디',         '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('사마천',       '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('프로이트',     '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('카뮈',         '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),
  ('데카르트',     '14da644e89dc4ab18a3c066788b3f2c5', '미호크 장정진', now(), now()),

  ('칸트',         '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('애들러',       '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('마키아벨리',   '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('엘리아데',     '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('프롬',         '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('융',           '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('롤스',         '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('왈처',         '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('암베드카르',   '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('라이프니츠',   '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('카너먼',       '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('도킨스',       '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('흄',           '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('보드리야르',   '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('몽테스키외',   '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('마이어스',     '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('마르크스',     '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),
  ('바흐',         '21c5fb9d1000425c9a74536691c46ce4', 'Meursault(남자 중저음)', now(), now()),

  ('아렌트',       '90597c459f7a4438801589a5268c1628', '차분하고 지적인(여성)', now(), now())
ON CONFLICT (name) DO NOTHING;
