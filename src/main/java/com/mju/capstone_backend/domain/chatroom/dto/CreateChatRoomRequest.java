package com.mju.capstone_backend.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(example = """
        {
          "destination": "제주도",
          "startDate": "2026-05-01",
          "endDate": "2026-05-03",
          "budget": 300000,
          "adultCount": 2,
          "childCount": 1,
          "childAges": [7]
        }
        """)
public record CreateChatRoomRequest(
        @NotBlank String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        BigDecimal budget,
        @NotNull @Min(1) Integer adultCount,
        @NotNull @Min(0) Integer childCount,
        @NotNull List<Integer> childAges
) {
}
