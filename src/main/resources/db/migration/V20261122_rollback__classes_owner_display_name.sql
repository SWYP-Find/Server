-- #669 V20261122_01 되돌리기. 미참여 운영자가 바꾼 이름은 사라지고 계정 닉네임으로 돌아간다.

ALTER TABLE classes DROP COLUMN IF EXISTS owner_display_name;
