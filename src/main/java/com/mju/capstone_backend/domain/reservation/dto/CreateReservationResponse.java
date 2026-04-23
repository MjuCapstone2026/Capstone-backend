package com.mju.capstone_backend.domain.reservation.dto;

import com.mju.capstone_backend.domain.reservation.entity.Reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateReservationResponse(
        UUID reservationId,
        UUID itineraryId,
        String type,
        String status,
        OffsetDateTime reservedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CreateReservationResponse from(Reservation reservation) {
        return new CreateReservationResponse(
                reservation.getId(),
                reservation.getItineraryId(),
                reservation.getType(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
