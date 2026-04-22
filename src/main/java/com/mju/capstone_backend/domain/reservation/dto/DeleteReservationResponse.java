package com.mju.capstone_backend.domain.reservation.dto;

import java.util.UUID;

public record DeleteReservationResponse(
        UUID reservationId,
        boolean deleted
) {
    public static DeleteReservationResponse of(UUID reservationId) {
        return new DeleteReservationResponse(reservationId, true);
    }
}
