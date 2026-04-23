package com.mju.capstone_backend.domain.reservation.dto;

import com.mju.capstone_backend.domain.reservation.entity.Reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GetReservationsResponse(List<ReservationDto> reservations) {

    public record ReservationDto(
            UUID reservationId,
            UUID itineraryId,
            String type,
            String status,
            String bookedBy,
            String bookingUrl,
            String externalRefId,
            Map<String, Object> detail,
            BigDecimal totalPrice,
            String currency,
            OffsetDateTime reservedAt,
            OffsetDateTime cancelledAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static ReservationDto from(Reservation reservation, Map<String, Object> detail) {
            return new ReservationDto(
                    reservation.getId(),
                    reservation.getItineraryId(),
                    reservation.getType(),
                    reservation.getStatus(),
                    reservation.getBookedBy(),
                    reservation.getBookingUrl(),
                    reservation.getExternalRefId(),
                    detail,
                    reservation.getTotalPrice(),
                    reservation.getCurrency(),
                    reservation.getReservedAt(),
                    reservation.getCancelledAt(),
                    reservation.getCreatedAt(),
                    reservation.getUpdatedAt()
            );
        }
    }
}
