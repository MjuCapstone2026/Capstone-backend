package com.mju.capstone_backend.domain.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
        {
          "date": "2026-05-01",
          "index": 0,
          "status": "done"
        }
        """)
public record PatchItemStatusRequest(
        String date,
        Integer index,
        String status
) {
}
