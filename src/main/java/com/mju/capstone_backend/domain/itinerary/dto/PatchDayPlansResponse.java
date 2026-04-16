package com.mju.capstone_backend.domain.itinerary.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PatchDayPlansResponse(
        UUID itineraryId,
        Map<String, List<Map<String, Object>>> dayPlans,
        OffsetDateTime updatedAt
) {
}
