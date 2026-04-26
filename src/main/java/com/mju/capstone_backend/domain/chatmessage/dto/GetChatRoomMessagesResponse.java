package com.mju.capstone_backend.domain.chatmessage.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GetChatRoomMessagesResponse(
        UUID roomId,
        List<MessageItem> messages,
        OffsetDateTime nextCursor,
        boolean hasMore
) {
    public record MessageItem(
            UUID messageId,
            String role,
            String content,
            OffsetDateTime createdAt
    ) {}
}
