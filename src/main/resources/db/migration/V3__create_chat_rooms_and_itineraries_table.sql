-- V3__create_chat_rooms_and_itineraries_table.sql
-- 작성자: gkstmf
-- 설명: chat_rooms, itineraries 테이블 생성

CREATE TABLE chat_rooms
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    clerk_id    VARCHAR(255) NOT NULL REFERENCES users (clerk_id),
    name        VARCHAR(100) NOT NULL,
    ai_summary  TEXT,
    preferences JSONB,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_chat_rooms_clerk_id ON chat_rooms (clerk_id);

CREATE TABLE itineraries
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    room_id     UUID NOT NULL UNIQUE REFERENCES chat_rooms (id),
    destination VARCHAR(255) NOT NULL,
    budget      DECIMAL(12, 2),
    adult_count INT  NOT NULL DEFAULT 1 CHECK (adult_count >= 1),
    child_count INT  NOT NULL DEFAULT 0 CHECK (child_count >= 0),
    child_ages  JSONB        DEFAULT '[]',
    total_days  INT  NOT NULL CHECK (total_days >= 1),
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'draft',
    day_plans   JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_itineraries_day_plans ON itineraries USING gin (day_plans);
