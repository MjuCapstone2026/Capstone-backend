package com.mju.capstone_backend.domain.chatmessage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GetChatRoomMessagesResponse(
        UUID roomId,
        List<MessageItem> messages,
        OffsetDateTime nextCursor,
        boolean hasMore
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageItem(
            UUID messageId,
            String role,
            String content,
            Object actionResult,
            OffsetDateTime createdAt
    ) {}
}
