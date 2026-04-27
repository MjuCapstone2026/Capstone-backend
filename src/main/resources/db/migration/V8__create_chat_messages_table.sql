-- V8__create_chat_messages_table.sql
-- 설명: chat_messages 테이블 생성 (채팅방 메시지 히스토리 저장)

CREATE TABLE chat_messages
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID         NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    role       VARCHAR(20)  NOT NULL CHECK (role IN ('user', 'assistant', 'tool')),
    content    TEXT         NOT NULL,
    embedding  vector(1536),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_chat_messages_room_id ON chat_messages (room_id);
CREATE INDEX idx_chat_messages_embedding ON chat_messages USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
