-- V6__add_not_null_to_itineraries_dates.sql
-- 설명: itineraries.start_date, end_date에 NOT NULL 제약 추가
--       채팅방 생성 시 날짜가 항상 필수값으로 설정되므로 DB 레벨에서도 강제

ALTER TABLE itineraries
    ALTER COLUMN start_date SET NOT NULL;

ALTER TABLE itineraries
    ALTER COLUMN end_date SET NOT NULL;
