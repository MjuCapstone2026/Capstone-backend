-- V4__add_cascade_delete_to_chat_rooms.sql
-- 작성자: gkstmf
-- 설명: itineraries.room_id FK에 ON DELETE CASCADE 추가 (채팅방 삭제 시 일정 자동 삭제)

ALTER TABLE itineraries
    DROP CONSTRAINT itineraries_room_id_fkey;

ALTER TABLE itineraries
    ADD CONSTRAINT itineraries_room_id_fkey
        FOREIGN KEY (room_id) REFERENCES chat_rooms (id) ON DELETE CASCADE;
