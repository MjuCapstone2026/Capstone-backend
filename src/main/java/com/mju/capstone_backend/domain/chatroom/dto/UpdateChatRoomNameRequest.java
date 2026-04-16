package com.mju.capstone_backend.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(example = """
        {
          "name": "오사카 3박 4일 여행"
        }
        """)
public record UpdateChatRoomNameRequest(
        @NotBlank String name
) {
}
