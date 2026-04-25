package com.mju.capstone_backend.domain.chatmessage.dto;

import java.util.UUID;

public record FastApiChatRequest(
        UUID roomId,
        String content
) {
}
