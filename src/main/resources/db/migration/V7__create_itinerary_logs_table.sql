-- V7__create_itinerary_logs_table.sql
-- 작성자: ChaeHyunLim
-- 설명: itinerary_logs 테이블 생성 (일정 수정 전 스냅샷 이력 관리)

CREATE TABLE itinerary_logs
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id   UUID             NOT NULL REFERENCES itineraries (id) ON DELETE CASCADE,
    destination    VARCHAR(255),
    budget         DECIMAL(12, 2),
    adult_count    INT,
    child_count    INT,
    child_ages     JSONB,
    total_days     INT,
    start_date     DATE,
    end_date       DATE,
    day_plans      JSONB            NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_itinerary_logs_itinerary_id ON itinerary_logs (itinerary_id);
