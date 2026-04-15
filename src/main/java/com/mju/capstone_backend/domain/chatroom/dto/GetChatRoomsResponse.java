package com.mju.capstone_backend.domain.chatroom.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GetChatRoomsResponse(
        List<ChatRoomItem> rooms
) {
    public record ChatRoomItem(
            UUID roomId,
            String name,
            String clerkId,
            String aiSummary,
            Map<String, Object> preferences,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}
}
