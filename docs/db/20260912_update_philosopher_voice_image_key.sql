-- philosopher_voice.image_key 채우기
-- 관련 이슈: #475 (PR #476)
--
-- Railway Bucket images/philosophers/ 에 실제 업로드된 파일과 이름이 일치하는
-- 22명만 채운다. 나머지 36명(voice는 있지만 이미지 없음)은 image_key = NULL 유지 —
-- 프론트가 null 이면 기본 플레이스홀더 이미지로 대체해야 한다(philosopher-voice-api.md 참고).
--
-- images/philosophers/ 에는 이 22명 외에 6개 파일이 더 있다
-- (aquinas, choe_hangi, jeong_yakyong, xunzi, yi_hwang, yi_i) —
-- 이 6명은 philosopher_voice 에 row 자체가 없다(Fish 보이스 미정이라 별도 처리 필요).
--
-- 환경별(dev/prod) 1회 실행. 재실행해도 안전(멱등 UPDATE).

UPDATE philosopher_voice SET image_key = 'images/philosophers/aristotle.png',    updated_at = now() WHERE name = '아리스토텔레스';
UPDATE philosopher_voice SET image_key = 'images/philosophers/buddha.png',       updated_at = now() WHERE name = '석가모니';
UPDATE philosopher_voice SET image_key = 'images/philosophers/camus.png',        updated_at = now() WHERE name = '카뮈';
UPDATE philosopher_voice SET image_key = 'images/philosophers/confucius.png',    updated_at = now() WHERE name = '공자';
UPDATE philosopher_voice SET image_key = 'images/philosophers/descartes.png',    updated_at = now() WHERE name = '데카르트';
UPDATE philosopher_voice SET image_key = 'images/philosophers/epicurus.png',     updated_at = now() WHERE name = '에피쿠로스';
UPDATE philosopher_voice SET image_key = 'images/philosophers/fromm.png',        updated_at = now() WHERE name = '프롬';
UPDATE philosopher_voice SET image_key = 'images/philosophers/hobbes.png',       updated_at = now() WHERE name = '홉스';
UPDATE philosopher_voice SET image_key = 'images/philosophers/hume.png',         updated_at = now() WHERE name = '흄';
UPDATE philosopher_voice SET image_key = 'images/philosophers/jung.png',         updated_at = now() WHERE name = '융';
UPDATE philosopher_voice SET image_key = 'images/philosophers/kant.png',         updated_at = now() WHERE name = '칸트';
UPDATE philosopher_voice SET image_key = 'images/philosophers/laozi.png',        updated_at = now() WHERE name = '노자';
UPDATE philosopher_voice SET image_key = 'images/philosophers/leibniz.png',      updated_at = now() WHERE name = '라이프니츠';
UPDATE philosopher_voice SET image_key = 'images/philosophers/marx.png',         updated_at = now() WHERE name = '마르크스';
UPDATE philosopher_voice SET image_key = 'images/philosophers/mencius.png',      updated_at = now() WHERE name = '맹자';
UPDATE philosopher_voice SET image_key = 'images/philosophers/mill.png',         updated_at = now() WHERE name = '밀';
UPDATE philosopher_voice SET image_key = 'images/philosophers/nietzsche.png',    updated_at = now() WHERE name = '니체';
UPDATE philosopher_voice SET image_key = 'images/philosophers/plato.png',        updated_at = now() WHERE name = '플라톤';
UPDATE philosopher_voice SET image_key = 'images/philosophers/rawls.png',        updated_at = now() WHERE name = '롤스';
UPDATE philosopher_voice SET image_key = 'images/philosophers/sartre.png',       updated_at = now() WHERE name = '사르트르';
UPDATE philosopher_voice SET image_key = 'images/philosophers/schopenhauer.png', updated_at = now() WHERE name = '쇼펜하우어';
UPDATE philosopher_voice SET image_key = 'images/philosophers/socrates.png',     updated_at = now() WHERE name = '소크라테스';
