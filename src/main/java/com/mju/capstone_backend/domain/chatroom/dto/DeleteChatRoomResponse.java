package com.mju.capstone_backend.domain.chatroom.dto;

import java.util.UUID;

public record DeleteChatRoomResponse(
        UUID roomId,
        boolean deleted
) {
}
