package com.mju.capstone_backend.domain.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
        {
          "status": "completed"
        }
        """)
public record PatchStatusRequest(
        String status
) {
}
