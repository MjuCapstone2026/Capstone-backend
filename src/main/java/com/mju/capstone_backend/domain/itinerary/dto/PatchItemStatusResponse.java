package com.mju.capstone_backend.domain.itinerary.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatchItemStatusResponse(
        UUID itineraryId,
        String date,
        Integer index,
        String status,
        OffsetDateTime updatedAt
) {
}
