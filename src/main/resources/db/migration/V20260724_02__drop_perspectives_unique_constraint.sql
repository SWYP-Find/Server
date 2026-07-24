-- 1인 1관점 제한 해제: perspectives(battle_id, user_id) 유니크 제약 제거
-- Hibernate가 이름을 자동 생성해 환경마다 다를 수 있으므로 이름에 의존하지 않고 동적으로 찾아 드롭한다.
DO $$
DECLARE
    found_constraint_name text;
BEGIN
    SELECT tc.constraint_name INTO found_constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
     AND tc.table_name = kcu.table_name
    WHERE tc.table_name = 'perspectives'
      AND tc.constraint_type = 'UNIQUE'
    GROUP BY tc.constraint_name
    HAVING array_agg(kcu.column_name ORDER BY kcu.column_name) = ARRAY['battle_id', 'user_id'];

    IF found_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE perspectives DROP CONSTRAINT %I', found_constraint_name);
    END IF;
END $$;
