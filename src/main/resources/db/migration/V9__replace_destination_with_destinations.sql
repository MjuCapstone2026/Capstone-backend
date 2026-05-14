-- V9__replace_destination_with_destinations.sql
-- 설명: itineraries.destination(단일 문자열) → destinations(JSONB 배열, 도시별 날짜 범위)로 교체
--       itinerary_logs 동일 변경

ALTER TABLE itineraries DROP COLUMN IF EXISTS destination;
ALTER TABLE itineraries ADD COLUMN IF NOT EXISTS destinations JSONB NOT NULL DEFAULT '[]';

ALTER TABLE itinerary_logs DROP COLUMN IF EXISTS destination;
ALTER TABLE itinerary_logs ADD COLUMN IF NOT EXISTS destinations JSONB;
