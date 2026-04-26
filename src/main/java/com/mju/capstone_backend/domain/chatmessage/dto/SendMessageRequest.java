package com.mju.capstone_backend.domain.chatmessage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(example = """
        {
          "content": "경복궁 대신 창덕궁으로 바꿔줘"
        }
        """)
public record SendMessageRequest(
        @NotBlank String content
) {
}
