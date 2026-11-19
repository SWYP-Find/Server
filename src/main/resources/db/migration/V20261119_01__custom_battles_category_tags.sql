-- #647 기존 커스텀 배틀(creator_type = USER)에 주제 카테고리를 카테고리 태그로 붙인다.
-- 새로 만드는 커스텀 배틀은 서버가 생성 시 붙인다. 이 SQL 은 배포 전에 만든 커스텀 배틀용이다.
-- battles.category 는 enum 이름(SOCIETY 등)이고, 태그 이름은 한글(사회 등)이다.
-- 같은 이름의 카테고리 태그가 없으면 만든다. 이미 붙어 있으면 건너뛰므로 재실행해도 안전하다.

WITH category_names(category, name) AS (
    VALUES ('PHILOSOPHY', '철학'), ('LITERATURE', '문학'), ('ART', '예술'),
           ('SCIENCE', '과학'), ('SOCIETY', '사회'), ('HISTORY', '역사')
)
INSERT INTO tags (name, type, created_at, updated_at)
SELECT DISTINCT cn.name, 'CATEGORY', NOW(), NOW()
FROM battles b
JOIN category_names cn ON cn.category = b.category
WHERE b.creator_type = 'USER'
  AND NOT EXISTS (
      SELECT 1 FROM tags t WHERE t.name = cn.name AND t.type = 'CATEGORY' AND t.deleted_at IS NULL);

WITH category_names(category, name) AS (
    VALUES ('PHILOSOPHY', '철학'), ('LITERATURE', '문학'), ('ART', '예술'),
           ('SCIENCE', '과학'), ('SOCIETY', '사회'), ('HISTORY', '역사')
)
INSERT INTO battle_tags (battle_id, tag_id, created_at, updated_at)
SELECT b.id,
       (SELECT MIN(t.id) FROM tags t WHERE t.name = cn.name AND t.type = 'CATEGORY' AND t.deleted_at IS NULL),
       NOW(), NOW()
FROM battles b
JOIN category_names cn ON cn.category = b.category
WHERE b.creator_type = 'USER'
  AND NOT EXISTS (
      SELECT 1 FROM battle_tags bt JOIN tags t ON t.id = bt.tag_id
      WHERE bt.battle_id = b.id AND t.type = 'CATEGORY');
