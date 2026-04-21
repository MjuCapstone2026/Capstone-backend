package com.mju.capstone_backend.domain.reservation.dto;

import com.mju.capstone_backend.domain.reservation.entity.Reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PatchReservationResponse(
        UUID reservationId,
        String status,
        OffsetDateTime updatedAt
) {
    public static PatchReservationResponse from(Reservation reservation) {
        return new PatchReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getUpdatedAt()
        );
    }
}
