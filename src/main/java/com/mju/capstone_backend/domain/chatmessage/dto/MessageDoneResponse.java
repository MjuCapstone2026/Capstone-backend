package com.mju.capstone_backend.domain.chatmessage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageDoneResponse(
        MessageItem userMessage,
        MessageItem assistantMessage,
        ItineraryResult itinerary    // null이면 JSON에서 필드 제외
) {
    public record MessageItem(
            UUID messageId,
            String role,
            String content,
            OffsetDateTime createdAt
    ) {
    }

    public record ItineraryResult(
            UUID itineraryId,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, List<Map<String, Object>>> dayPlans,
            OffsetDateTime updatedAt
    ) {
    }
}
