-- #647 V20261119_01 되돌리기. 커스텀 배틀(creator_type = USER)에 붙은 카테고리 태그 연결을 지운다.
-- 배포 전 커스텀 배틀에는 태그가 없었으므로 USER 배틀의 카테고리 태그 연결만 지운다. 태그 자체(tags)는 남긴다.

DELETE FROM battle_tags bt
USING battles b, tags t
WHERE bt.battle_id = b.id
  AND bt.tag_id = t.id
  AND b.creator_type = 'USER'
  AND t.type = 'CATEGORY';
