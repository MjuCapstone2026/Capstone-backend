package com.mju.capstone_backend.domain.chatmessage.service;

import com.mju.capstone_backend.domain.chatmessage.dto.GetChatRoomMessagesResponse;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ChatMessageService {

    Mono<GetChatRoomMessagesResponse> getMessages(String clerkId, UUID roomId, OffsetDateTime cursor, int limit);

    Flux<ServerSentEvent<Object>> sendMessage(String clerkId, UUID roomId, String content);
}
