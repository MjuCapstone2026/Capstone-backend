package com.mju.capstone_backend.domain.chatmessage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageDoneResponse(
        MessageItem userMessage,
        MessageItem assistantMessage,
        ItineraryResult itinerary,
        ChangeResult change,
        ReservationResult reservation,
        CancelResult cancel
) {
    public record MessageItem(
            UUID messageId,
            String role,
            String content,
            OffsetDateTime createdAt
    ) {}

    public record ItineraryResult(
            UUID itineraryId,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, List<Map<String, Object>>> dayPlans,
            OffsetDateTime updatedAt
    ) {}

    public record ChangeResult(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budget,
            Integer adultCount,
            Integer childCount,
            List<Integer> childAges,
            OffsetDateTime updatedAt
    ) {}

    public record ReservationResult(
            UUID reservationId,
            String type,
            String bookingUrl,
            String status,
            BigDecimal totalPrice,
            String currency,
            OffsetDateTime reservedAt
    ) {}

    public record CancelResult(
            UUID reservationId,
            OffsetDateTime cancelledAt
    ) {}
}
