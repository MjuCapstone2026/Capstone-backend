package com.mju.capstone_backend.domain.chatmessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatmessage.dto.ChatStreamEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FastApiChatClient {

    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.internal-token:}")  // AI 연동 전까지 기본값 빈 문자열로 서버 기동 허용
    private String internalToken;

    public Flux<ChatStreamEvent> stream(UUID roomId, String content) {
        // TODO: WebClient로 POST /api/v1/chat SSE 연동 구현
        // TODO: X-Internal-Token 헤더 추가
        // TODO: bodyToFlux SSE 파싱 (chunk / done 이벤트)
        return Flux.error(new UnsupportedOperationException("AI integration pending"));
    }
}
