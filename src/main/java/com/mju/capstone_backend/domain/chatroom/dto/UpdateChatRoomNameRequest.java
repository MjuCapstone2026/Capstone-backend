package com.mju.capstone_backend.domain.chatroom.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateChatRoomNameRequest(
        @NotBlank String name
) {
}
