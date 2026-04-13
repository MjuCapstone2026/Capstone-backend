package com.mju.capstone_backend.domain.chatroom.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateChatRoomResponse(
        UUID roomId,
        UUID itineraryId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
