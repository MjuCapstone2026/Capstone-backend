package com.mju.capstone_backend.domain.chatmessage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.UUID;

public record FastApiChatRequest(
        UUID roomId,
        String content,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        MemoryPayload memory
) {
    public record MemoryPayload(
            String aiSummary,
            Map<String, Object> preferences
    ) {}
}
