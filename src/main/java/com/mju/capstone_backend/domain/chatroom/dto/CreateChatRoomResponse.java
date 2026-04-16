package com.mju.capstone_backend.domain.chatroom.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateChatRoomResponse(
        UUID roomId,
        String name,
        UUID itineraryId,
        String clerkId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
