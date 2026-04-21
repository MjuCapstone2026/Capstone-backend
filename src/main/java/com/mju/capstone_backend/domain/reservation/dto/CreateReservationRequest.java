package com.mju.capstone_backend.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(example = """
        {
          "itineraryId": "be4d9d2d-1d84-4b1b-bf4d-1ac6b9cc7f22",
          "type": "flight",
          "bookedBy": "ai",
          "bookingUrl": "https://booking.example.com/flight/123",
          "externalRefId": "KE12345678",
          "detail": {
            "airline": "대한항공",
            "flight_no": "KE123",
            "departure": {"airport": "ICN", "datetime": "2026-05-01T09:00:00"},
            "arrival": {"airport": "NRT", "datetime": "2026-05-01T11:30:00"},
            "seat_class": "economy",
            "passengers": [{"name": "홍길동", "passport": "M12345678"}]
          },
          "totalPrice": 320000.00,
          "currency": "KRW",
          "reservedAt": "2026-04-03T21:30:00+09:00"
        }
        """)
public record CreateReservationRequest(
        @NotNull UUID itineraryId,
        @NotBlank String type,
        @NotBlank String bookedBy,
        String bookingUrl,
        String externalRefId,
        @NotNull Map<String, Object> detail,
        BigDecimal totalPrice,
        String currency,
        OffsetDateTime reservedAt
) {
}
