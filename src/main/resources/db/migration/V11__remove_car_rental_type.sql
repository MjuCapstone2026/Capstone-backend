-- V11__remove_car_rental_type.sql
-- 설명: reservations.type에서 car_rental 제거 (flight, accommodation만 허용)

DELETE FROM reservations WHERE type = 'car_rental';

ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_type_check;
ALTER TABLE reservations ADD CONSTRAINT reservations_type_check
    CHECK (type IN ('flight', 'accommodation'));
