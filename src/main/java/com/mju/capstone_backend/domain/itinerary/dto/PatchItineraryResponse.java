package com.mju.capstone_backend.domain.itinerary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PatchItineraryResponse(
        UUID itineraryId,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        BigDecimal budget,
        int adultCount,
        int childCount,
        List<Integer> childAges,
        OffsetDateTime updatedAt
) {
}
