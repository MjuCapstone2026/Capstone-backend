package com.mju.capstone_backend.domain.chatmessage.controller;

import com.mju.capstone_backend.domain.chatmessage.dto.GetChatRoomMessagesResponse;
import com.mju.capstone_backend.domain.chatmessage.dto.SendMessageRequest;
import com.mju.capstone_backend.domain.chatmessage.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Tag(name = "ChatMessage API", description = "채팅 메시지 관련 API")
@RestController
@RequestMapping("/api/v1/chat-rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @Operation(summary = "메시지 히스토리 조회", description = "현재 로그인한 사용자가 소유한 특정 채팅방의 메시지 히스토리를 최신순으로 조회합니다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<GetChatRoomMessagesResponse> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime cursor,
            @RequestParam(defaultValue = "30") int limit,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return chatMessageService.getMessages(clerkId, roomId, cursor, limit);
    }

    @Operation(summary = "메시지 전송", description = "현재 로그인한 사용자가 소유한 특정 채팅방에 메시지를 전송하고 AI Agent 응답을 스트리밍으로 반환합니다.")
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> sendMessage(
            @PathVariable UUID roomId,
            @RequestBody @Valid SendMessageRequest request,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return chatMessageService.sendMessage(clerkId, roomId, request.content());
    }
}
