package com.mju.capstone_backend.domain.chatmessage.dto;

import com.mju.capstone_backend.domain.chatmessage.dto.FastApiDonePayload;

public sealed interface ChatStreamEvent
        permits ChatStreamEvent.Chunk, ChatStreamEvent.Done {

    record Chunk(String content) implements ChatStreamEvent {}
    record Done(FastApiDonePayload payload) implements ChatStreamEvent {}
}
