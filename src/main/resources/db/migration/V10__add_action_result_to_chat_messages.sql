-- V10__add_action_result_to_chat_messages.sql
-- 설명: chat_messages에 action_result 컬럼 추가 (assistant 메시지의 일정 스냅샷 저장용)

ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS action_result JSONB;
