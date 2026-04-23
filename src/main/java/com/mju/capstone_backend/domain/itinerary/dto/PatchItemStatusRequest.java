package com.mju.capstone_backend.domain.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(example = """
        {
          "date": "2026-05-01",
          "index": 0,
          "status": "done"
        }
        """)
public record PatchItemStatusRequest(
        @NotBlank String date,
        @NotNull Integer index,
        @NotBlank String status
) {
}
