package com.mju.capstone_backend.domain.chatroom.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record GetChatRoomResponse(
        UUID roomId,
        String aiSummary,
        Map<String, Object> preferences,
        UUID itineraryId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
