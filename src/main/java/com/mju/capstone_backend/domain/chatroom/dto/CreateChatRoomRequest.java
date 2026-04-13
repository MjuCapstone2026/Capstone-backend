package com.mju.capstone_backend.domain.chatroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateChatRoomRequest(
        @NotBlank String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        BigDecimal budget,
        @NotNull @Min(1) Integer adultCount,
        Integer childCount,
        List<Integer> childAges
) {
    public int resolvedChildCount() {
        return childCount != null ? childCount : 0;
    }
}
