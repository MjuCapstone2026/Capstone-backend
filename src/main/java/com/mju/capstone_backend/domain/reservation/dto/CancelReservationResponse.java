package com.mju.capstone_backend.domain.reservation.dto;

import com.mju.capstone_backend.domain.reservation.entity.Reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CancelReservationResponse(
        UUID reservationId,
        String status,
        OffsetDateTime cancelledAt,
        OffsetDateTime updatedAt
) implements PatchReservationResponse {
    public static CancelReservationResponse from(Reservation reservation) {
        return new CancelReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getCancelledAt(),
                reservation.getUpdatedAt()
        );
    }
}
