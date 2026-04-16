package com.mju.capstone_backend.domain.itinerary.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatchStatusResponse(
        UUID itineraryId,
        String status,
        OffsetDateTime updatedAt
) {
}
