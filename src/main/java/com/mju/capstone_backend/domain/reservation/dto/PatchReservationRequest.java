package com.mju.capstone_backend.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Schema(example = """
        {
          "status": "cancelled",
          "cancelledAt": "2026-04-10T10:00:00+09:00"
        }
        """)
public record PatchReservationRequest(
        String status,
        Map<String, Object> detail,
        BigDecimal totalPrice,
        String currency,
        OffsetDateTime reservedAt,
        OffsetDateTime cancelledAt
) {
    public boolean isEmpty() {
        return status == null && detail == null && totalPrice == null
                && currency == null && reservedAt == null && cancelledAt == null;
    }
}
