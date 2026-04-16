package com.mju.capstone_backend.domain.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(example = """
        {
          "startDate": "2026-05-01",
          "endDate": "2026-05-03",
          "budget": 300000,
          "adultCount": 2,
          "childCount": 1,
          "childAges": [7]
        }
        """)
public record PatchItineraryRequest(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        Integer adultCount,
        Integer childCount,
        List<Integer> childAges
) {
}
