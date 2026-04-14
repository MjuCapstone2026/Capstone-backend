package com.mju.capstone_backend.domain.chatroom.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateChatRoomNameResponse(
        UUID roomId,
        String name,
        OffsetDateTime updatedAt
) {
}
