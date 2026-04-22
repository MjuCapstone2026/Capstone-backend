package com.mju.capstone_backend.domain.reservation.dto;

import com.mju.capstone_backend.domain.reservation.entity.Reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChangeReservationResponse(
        UUID reservationId,
        String status,
        OffsetDateTime reservedAt,
        OffsetDateTime updatedAt
) implements PatchReservationResponse {
    public static ChangeReservationResponse from(Reservation reservation) {
        return new ChangeReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getUpdatedAt()
        );
    }
}
