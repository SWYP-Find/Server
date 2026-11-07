-- #610 기획 확정 배틀별 수준 반영. #571 에서 임시로 초급으로 채운 기존 배틀의 수준을 확정값으로 바꾼다.
-- 기획 목록 115개와, 기획과 제목이 다르게 등록된 배틀의 이전 제목 26개로 DB에 있는 배틀만 찾는다. 목록에 없거나 DB에 없는 배틀은 그대로 둔다.
-- 제목은 공백·문장부호를 빼고 비교한다. 사용자가 만든 배틀(USER)과 삭제된 배틀은 제외한다.
-- 카테고리는 같은 제목이 사회 초급·역사 중급에 있는 "살기 위해 반역의 왕위, 받아들일까?"에만 비교한다. 한 배틀에 기획 행이 둘 이상 맞으면 바꾸지 않는다.
-- 멱등하게 작성되어 재실행해도 안전하다. 한 트랜잭션으로 실행한다.

BEGIN;

DROP TABLE IF EXISTS battle_difficulty_match;
DROP TABLE IF EXISTS battle_difficulty_plan;
-- category 는 같은 제목이 두 카테고리에 있는 행에만 채운다. 나머지는 제목만으로 찾는다
CREATE TEMP TABLE battle_difficulty_plan (plan_no VARCHAR(40), title TEXT, category VARCHAR(30), difficulty VARCHAR(20));
INSERT INTO battle_difficulty_plan (plan_no, title, category, difficulty) VALUES
    ('철학 001', '인간은 본래 선한가, 악한가?', NULL, 'BEGINNER'),
    ('철학 002', '불편한 진실보다 편안한 무지가 나을까?', NULL, 'INTERMEDIATE'),
    ('철학 003', '무지는 죄일까?', NULL, 'ADVANCED'),
    ('철학 004', '탈옥은 인간의 권리인가, 범죄인가?', NULL, 'ADVANCED'),
    ('철학 005', '트롤리 딜레마, 다수 위한 희생은 옳을까?', NULL, 'INTERMEDIATE'),
    ('철학 006', '자기계발서, 정말 삶을 바꿀 수 있을까?', NULL, 'INTERMEDIATE'),
    ('철학 007', '행복은 이루는 것인가, 누리는 것인가?', NULL, 'BEGINNER'),
    ('철학 008', '선의의 거짓말은 정당한가?', NULL, 'BEGINNER'),
    ('철학 009', '비폭력은 언제나 옳은가?', NULL, 'ADVANCED'),
    ('철학 010', '''0''은 발견인가, 발명인가?', NULL, 'ADVANCED'),
    ('철학 011', 'AI 판사는 도입해야 하는가?', NULL, 'ADVANCED'),
    ('철학 013', '사라질 위기의 언어 교육은 의무인가, 선택인가?', NULL, 'INTERMEDIATE'),
    ('철학 014', '도덕은 이성에서 오는가, 감정에서 오는가?', NULL, 'ADVANCED'),
    ('철학 015', '사후세계, 존재할까 인간이 만든 허구일까?', NULL, 'BEGINNER'),
    ('철학 016', '꿈속 상징은 정말 의미가 있을까?', NULL, 'INTERMEDIATE'),
    ('철학 018', '모든 일은 운명일까, 우연일 뿐일까?', NULL, 'BEGINNER'),
    ('철학 019', '매트릭스, 가상세계 속 행복은 진짜일까?', NULL, 'INTERMEDIATE'),
    ('철학 021', '피노키오를 죽이는 건 살인인가?', NULL, 'INTERMEDIATE'),
    ('철학 023', '죽음을 앞둔 사람에게 진실을 말할까, 감출까?', NULL, 'ADVANCED'),
    ('철학 024', '복수는 되갚는 것일까, 용서하는 것일까?', NULL, 'INTERMEDIATE'),
    ('철학 025', '애인이 내 친구의 깻잎을 떼어주는 건 배려인가, 무례인가?', NULL, 'BEGINNER'),
    ('철학 026', '일은 좋아하는 것을 택해야 하는가, 잘하는 것을 택해야 하는가?', NULL, 'BEGINNER'),
    ('사회 001', '언어가 생각을 지배하는가?', NULL, 'INTERMEDIATE'),
    ('사회 002', '망각은 축복인가, 저주인가?', NULL, 'BEGINNER'),
    ('사회 003', '원가보다 훨씬 비싼 명품은 사기일까?', NULL, 'INTERMEDIATE'),
    ('사회 004', '범죄자에 대한 즉결처분은 정당한가?', NULL, 'ADVANCED'),
    ('사회 005', '광복절 특별사면, 폐지해야 할까?', NULL, 'ADVANCED'),
    ('사회 006', '경범죄 모범수, 조기 석방 허용해야 할까?', NULL, 'INTERMEDIATE'),
    ('사회 007', '혐오표현은 표현의 자유인가, 폭력인가?', NULL, 'ADVANCED'),
    ('사회 008', '노키즈존은 영업의 자유인가, 아동 차별인가?', NULL, 'BEGINNER'),
    ('사회 009', '살기 위해 반역의 왕위, 받아들일까?', 'SOCIETY', 'BEGINNER'),
    ('사회 010', '회사에서 쓰는 가면은 위선인가, 처세인가?', NULL, 'INTERMEDIATE'),
    ('사회 011', '종교와 사이비, 차이는 성공인가 본질인가?', NULL, 'ADVANCED'),
    ('사회 012', 'MBTI, 과학일까 현대판 사주일까?', NULL, 'INTERMEDIATE'),
    ('사회 013', '사회는 공정한 계약인가, 교묘한 감시인가?', NULL, 'ADVANCED'),
    ('사회 014', '외모도 실력인가, 겉모습에 불과한가?', NULL, 'BEGINNER'),
    ('사회 015', '모든 시민의 공공 노동 의무는 정당한가?', NULL, 'ADVANCED'),
    ('역사 001', '신숙주는 변절자인가, 킹메이커인가?', NULL, 'INTERMEDIATE'),
    ('역사 002', '오펜하이머: 그는 영웅인가, 악마인가?', NULL, 'ADVANCED'),
    ('역사 003', '종교는 인류를 더 많이 죽였는가, 살렸는가?', NULL, 'ADVANCED'),
    ('역사 004', '자국민 죽이면 폭군, 타국민 죽이면 성군인가?', NULL, 'ADVANCED'),
    ('역사 005', '이방원은 냉혹한 폭군인가, 불가피한 혁명가인가?', NULL, 'INTERMEDIATE'),
    ('역사 006', '콜럼버스, 위대한 개척자인가 학살범인가?', NULL, 'INTERMEDIATE'),
    ('역사 007', '간디, 위대한 성자인가 모순된 위선자인가?', NULL, 'ADVANCED'),
    ('역사 008', '논란이 된 인물의 동상, 철거해야 할까?', NULL, 'INTERMEDIATE'),
    ('역사 009', '살기 위해 반역의 왕위, 받아들일까?', 'HISTORY', 'INTERMEDIATE'),
    ('과학 001', '아틀란티스는 허구라고 단언할 수 있는가?', NULL, 'INTERMEDIATE'),
    ('과학 002', 'AI와 사랑에 빠지는 것, 진짜 사랑인가?', NULL, 'INTERMEDIATE'),
    ('과학 003', '나쁜 기억을 지우는 약, 먹어도 될까?', NULL, 'INTERMEDIATE'),
    ('과학 004', '배양육, 진짜 고기일까 맛만 흉내 낸 걸까?', NULL, 'INTERMEDIATE'),
    ('과학 005', '평행우주 속의 나는 진정한 나인가?', NULL, 'ADVANCED'),
    ('과학 006', '삼겹살은 먹어도 되고, 강아지는 소중할까?', NULL, 'BEGINNER'),
    ('과학 007', '복제인간은 나인가, 나를 닮은 타인인가?', NULL, 'INTERMEDIATE'),
    ('과학 008', '인공 자궁은 축복인가?', NULL, 'ADVANCED'),
    ('과학 009', '가짜 약도 효과만 있다면 괜찮을까?', NULL, 'ADVANCED'),
    ('과학 010', '기억을 잃은 사람은 여전히 같은 사람일까?', NULL, 'ADVANCED'),
    ('과학 011', '안락사, 허용해도 되는가?', NULL, 'ADVANCED'),
    ('문학 001', '역사 왜곡은 모함인가, 창작의 자유인가?', NULL, 'INTERMEDIATE'),
    ('문학 002', '웹소설은 문학인가, 상품인가?', NULL, 'ADVANCED'),
    ('문학 003', '새드 엔딩 과몰입은 힐링인가, 자학인가?', NULL, 'INTERMEDIATE'),
    ('문학 004', 'PC주의, 원작 파괴인가 다양성 존중인가?', NULL, 'ADVANCED'),
    ('문학 005', '5분 요약으로 영화를 다 봤다고 해도 될까, 안 될까?', NULL, 'BEGINNER'),
    ('문학 006', '로미오와 줄리엣: 숭고한 로맨스인가, 참사인가?', NULL, 'INTERMEDIATE'),
    ('문학 007', '햄릿: 비극의 천재인가, 무능한 리더인가?', NULL, 'ADVANCED'),
    ('문학 008', '베니스의 상인: 샤일록의 요구는 정당한가?', NULL, 'ADVANCED'),
    ('문학 009', '이방인, 공감하지 못하는 것도 죄인가?', NULL, 'ADVANCED'),
    ('예술 001', 'AI가 만든 작품, 예술인가 모방인가?', NULL, 'INTERMEDIATE'),
    ('예술 002', '라이언 일병 구하려 8명이 목숨 걸어도 될까?', NULL, 'ADVANCED'),
    ('예술 003', '클래식은 대중음악보다 고상한가?', NULL, 'BEGINNER'),
    ('예술 004', '이해 못 한 예술은 실패작일까?', NULL, 'INTERMEDIATE'),
    ('예술 005', '예술은 세상을 바꾸기 위해 존재하는가?', NULL, 'ADVANCED'),
    ('예술 006', '예술은 반드시 아름다워야 하는가?', NULL, 'INTERMEDIATE'),
    ('예술 007', '뒤샹의 변기, 예술인가 도발인가?', NULL, 'ADVANCED'),
    ('예술 008', '수억 원의 바나나, 거품일까 가치 있을까?', NULL, 'INTERMEDIATE'),
    ('바이럴 001', '가해국 유명인, 피해국에서 활동해도 될까?', NULL, 'ADVANCED'),
    ('바이럴 002', '조상의 죄는 후손도 책임이 있을까?', NULL, 'ADVANCED'),
    ('바이럴 003', '범죄 예술가의 작품, 불매해야 할까?', NULL, 'ADVANCED'),
    ('바이럴 004', '아이의 종교적 이름, 부모의 자유일까?', NULL, 'INTERMEDIATE'),
    ('바이럴 005', '놀이공원 프리패스 제도는 정당한가?', NULL, 'BEGINNER'),
    ('바이럴 006', '남녀 사이에 진짜 친구가 가능할까?', NULL, 'INTERMEDIATE'),
    ('바이럴 007', '탕수육은 부먹이 진리인가, 찍먹이 진리인가?', NULL, 'BEGINNER'),
    ('바이럴 008', '첫 데이트 국밥, 무죄인가 유죄인가?', NULL, 'BEGINNER'),
    ('바이럴 009', '썸에도 독점권이 있을까?', NULL, 'INTERMEDIATE'),
    ('바이럴 010', '9시 출근, 10분 전에 도착해야 할까?', NULL, 'INTERMEDIATE'),
    ('바이럴 011', '기념일, 백일마다 챙겨야 할까?', NULL, 'BEGINNER'),
    ('바이럴 012', '주말에 온 업무 카톡, 무시해도 될까?', NULL, 'INTERMEDIATE'),
    ('바이럴 013', '단톡방 생일, 꼭 다 같이 축하해야 할까?', NULL, 'BEGINNER'),
    ('바이럴 014', '축의금, 친한 만큼 낼까 여유만큼 낼까?', NULL, 'INTERMEDIATE'),
    ('바이럴 015', '정신적 바람과 육체적 바람, 더 나쁜 건?', NULL, 'ADVANCED'),
    ('바이럴 016', '에어팟 끼고 일하는 건 근무 태만인가?', NULL, 'INTERMEDIATE'),
    ('바이럴 017', '데이트비는 정확히 반 나눠야 할까?', NULL, 'INTERMEDIATE'),
    ('바이럴 018', '나만 없는 친구들 단톡방, 그럴 수 있나?', NULL, 'BEGINNER'),
    ('바이럴 019', '동료 차 뒷좌석에 타는 건 개인의 자유인가, 비매너인가?', NULL, 'INTERMEDIATE'),
    ('바이럴 020', '혼자 비싼 메뉴를 시켜도 n빵할까, 따로 계산할까?', NULL, 'BEGINNER'),
    ('바이럴 021', '부모님께 드리는 용돈은 자식의 도리일까?', NULL, 'INTERMEDIATE'),
    ('바이럴 022', '연애에서는 센스가 중요한가, 매너가 중요한가?', NULL, 'BEGINNER'),
    ('바이럴 023', '피해자의 복수는 정당한가, 또 다른 가해인가?', NULL, 'INTERMEDIATE'),
    ('바이럴 024', '명문대 입시 위한 부모의 개입, 사랑인가 착취인가?', NULL, 'INTERMEDIATE'),
    ('바이럴 025', '단톡방 읽씹은 무례인가, 개인의 자유인가?', NULL, 'BEGINNER'),
    ('바이럴 026', '에렌은 영웅인가, 학살자인가?', NULL, 'ADVANCED'),
    ('바이럴 027', '절대적 자기확신은 용기인가, 무모함인가?', NULL, 'BEGINNER'),
    ('바이럴 028', '먼저 공격하고 보복에 맞서는 건 위선인가?', NULL, 'ADVANCED'),
    ('바이럴 029', '연예인의 악행 비판은 정당한가, 과도한가?', NULL, 'INTERMEDIATE'),
    ('바이럴 030', '불매운동은 소비자 권리인가?', NULL, 'INTERMEDIATE'),
    ('바이럴 031', '촉법소년 연령, 낮춰야 할까?', NULL, 'ADVANCED'),
    ('바이럴 032', '맛없는 음식 리뷰, 솔직하게 써야 할까?', NULL, 'BEGINNER'),
    ('바이럴 033', '변신, 가족이라면 끝까지 돌봐야 할까?', NULL, 'INTERMEDIATE'),
    ('바이럴 034', '죄와 벌, 세상을 위한 범죄는 정당한가?', NULL, 'ADVANCED'),
    ('바이럴 035', '작품에 드러난 정치색, 정당한가 강요인가?', NULL, 'ADVANCED'),
    ('바이럴 036', '운동선수 군면제, 계속 유지해야 할까?', NULL, 'ADVANCED'),
    ('바이럴 037', '주운 지갑, 사례비 먼저 챙겨도 될까?', NULL, 'BEGINNER'),
    ('바이럴 038', '쉬었음 청년, 세금으로 지원해야 할까?', NULL, 'ADVANCED'),
    ('바이럴 039', '천만 원 받고 5억년 살기 버튼, 누를까?', NULL, 'INTERMEDIATE'),
    ('바이럴 040', '아이돌의 연애, 제한해야 할까?', NULL, 'INTERMEDIATE'),
    ('바이럴 041', '영화 속 불륜과 범죄, 미화해도 될까?', NULL, 'ADVANCED'),
    ('철학 002 (이전 제목)', '불편한 진실과 편안한 무지, 어느 쪽을 택하겠는가?', NULL, 'INTERMEDIATE'),
    ('사회 001 (이전 제목)', '언어가 사람을 지배하는가? 안하는가?', NULL, 'INTERMEDIATE'),
    ('사회 007 (이전 제목)', '혐오표현도 표현의 자유인가?', NULL, 'ADVANCED'),
    ('역사 006 (이전 제목)', '콜럼버스의 대항해 위대한 개척자인가, 학살의 주범인가?', NULL, 'INTERMEDIATE'),
    ('과학 006 (이전 제목)', '삼겹살은 맛있고 강아지는 소중하다?', NULL, 'BEGINNER'),
    ('문학 001 (이전 제목)', '역사 왜곡인가, 창작의 자유인가?', NULL, 'INTERMEDIATE'),
    ('예술 001 (이전 제목)', 'AI는 예술을 창작할 수 있는가?', NULL, 'INTERMEDIATE'),
    ('예술 003 (이전 제목)', '클래식은 고상하고 대중음악은 가벼운가?', NULL, 'BEGINNER'),
    ('바이럴 004 (이전 제목)', '부모가 아이 이름에 종교적 의미를 담는 것은', NULL, 'INTERMEDIATE'),
    ('역사 002 (이전 제목)', '오펜하이머, 영웅인가 악마인가?', NULL, 'ADVANCED'),
    ('문학 001 (이전 제목)', '역사 왜곡, 창작의 자유일까?', NULL, 'INTERMEDIATE'),
    ('문학 006 (이전 제목)', '로미오와 줄리엣, 숭고한 로맨스인가?', NULL, 'INTERMEDIATE'),
    ('사회 014 (이전 제목)', '외모도 실력일까?', NULL, 'BEGINNER'),
    ('철학 023 (이전 제목)', '죽음을 앞둔 사람에게 진실을 말해야 할까?', NULL, 'ADVANCED'),
    ('역사 005 (이전 제목)', '이방원, 폭군인가 혁명가인가?', NULL, 'INTERMEDIATE'),
    ('철학 026 (이전 제목)', '일은 좋아하는 걸 택할까, 잘하는 걸 택할까?', NULL, 'BEGINNER'),
    ('철학 025 (이전 제목)', '애인이 내 친구의 깻잎 떼줘도 괜찮을까?', NULL, 'BEGINNER'),
    ('철학 013 (이전 제목)', '사라질 위기의 언어는 의무 교육해야 할까?', NULL, 'INTERMEDIATE'),
    ('사회 008 (이전 제목)', '노키즈존은 영업상의 자유일까?', NULL, 'BEGINNER'),
    ('바이럴 007 (이전 제목)', '탕수육, 부먹인가 찍먹인가?', NULL, 'BEGINNER'),
    ('바이럴 014 (이전 제목)', '축의금, 친한 만큼 낼까 형편만큼 낼까?', NULL, 'INTERMEDIATE'),
    ('바이럴 019 (이전 제목)', '직장 동료 차 뒷좌석에 타는 건 비매너일까?', NULL, 'INTERMEDIATE'),
    ('문학 005 (이전 제목)', '5분 요약으로 영화를 봤다고 해도 될까?', NULL, 'BEGINNER'),
    ('바이럴 030 (이전 제목)', '불매운동은 소비자의 권리인가?', NULL, 'INTERMEDIATE'),
    ('바이럴 026 (이전 제목)', '진격의 거인, 에렌은 영웅인가 학살자인가?', NULL, 'ADVANCED'),
    ('바이럴 024 (이전 제목)', '부모의 극단적 개입, 사랑인가 착취인가?', NULL, 'INTERMEDIATE');

-- 제목은 공백·문장부호를 빼고 비교한다
CREATE TEMP TABLE battle_difficulty_match AS
SELECT b.id AS battle_id, b.title AS battle_title, b.category AS battle_category, b.difficulty AS current_difficulty,
       p.plan_no, p.difficulty AS plan_difficulty
FROM battles b
JOIN battle_difficulty_plan p
  ON regexp_replace(lower(b.title), '[[:space:][:punct:]‘’“”·…「」『』]', '', 'g') = regexp_replace(lower(p.title), '[[:space:][:punct:]‘’“”·…「」『』]', '', 'g')
 AND (p.category IS NULL OR b.category = p.category)
WHERE b.deleted_at IS NULL AND b.creator_type <> 'USER';

-- 반영 전 확인용: 맞은 배틀과 바뀔 수준
-- SELECT * FROM battle_difficulty_match ORDER BY plan_no;
-- 확인용: DB에서 찾지 못한 기획 행
-- SELECT p.* FROM battle_difficulty_plan p WHERE NOT EXISTS (SELECT 1 FROM battle_difficulty_match m WHERE m.plan_no = p.plan_no) ORDER BY p.plan_no;

UPDATE battles b
SET difficulty = m.plan_difficulty
FROM battle_difficulty_match m
WHERE b.id = m.battle_id
  AND m.battle_id IN (SELECT battle_id FROM battle_difficulty_match GROUP BY battle_id HAVING COUNT(*) = 1)
  AND b.difficulty IS DISTINCT FROM m.plan_difficulty;

DROP TABLE battle_difficulty_match;
DROP TABLE battle_difficulty_plan;

COMMIT;
