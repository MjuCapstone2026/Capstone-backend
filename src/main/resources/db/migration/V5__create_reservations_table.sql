-- V5__create_reservations_table.sql
-- 작성자: gkstmf
-- 설명: reservations 테이블 생성

CREATE TABLE reservations
(
    id              UUID PRIMARY KEY          DEFAULT gen_random_uuid(),
    itinerary_id    UUID         NOT NULL REFERENCES itineraries (id) ON DELETE RESTRICT,
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('flight', 'accommodation', 'car_rental')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'confirmed' CHECK (status IN ('confirmed', 'changed', 'cancelled')),
    booked_by       VARCHAR(10)  NOT NULL DEFAULT 'user' CHECK (booked_by IN ('user', 'ai')),
    booking_url     TEXT,
    external_ref_id VARCHAR(255),
    detail          JSONB        NOT NULL DEFAULT '{}',
    total_price     DECIMAL(12, 2),
    currency        VARCHAR(3)            DEFAULT 'KRW',
    reserved_at     TIMESTAMP WITH TIME ZONE,
    cancelled_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_reservations_itinerary_id ON reservations (itinerary_id);
CREATE INDEX idx_reservations_status ON reservations (status);
CREATE INDEX idx_reservations_detail ON reservations USING gin (detail);
