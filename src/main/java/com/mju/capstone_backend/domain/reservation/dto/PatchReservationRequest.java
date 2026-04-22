package com.mju.capstone_backend.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Schema(example = """
        {
          "status": "changed",
          "detail": {
            "airline": "아시아나",
            "flight_no": "OZ108",
            "departure": {"airport": "ICN", "datetime": "2026-05-02T09:00:00"},
            "arrival": {"airport": "NRT", "datetime": "2026-05-02T11:30:00"},
            "seat_class": "economy",
            "passengers": [{"name": "홍길동", "passport": "M12345678"}]
          },
          "totalPrice": 290000.00,
          "currency": "KRW",
          "reservedAt": "2026-04-10T10:00:00+09:00"
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
