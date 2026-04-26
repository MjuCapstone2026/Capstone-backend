package com.mju.capstone_backend.domain.chatmessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatmessage.dto.ChatStreamEvent;
import com.mju.capstone_backend.domain.chatmessage.dto.FastApiChatRequest;
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

    @Value("${ai.internal-token:}")
    private String internalToken;

    public Flux<ChatStreamEvent> stream(UUID roomId, String content, FastApiChatRequest.MemoryPayload memory) {
        // TODO: POST /api/v1/ai-messages WebClient SSE 연동 구현
        //   - Header: X-Internal-Token: ${ai.internal-token}
        //   - Body: FastApiChatRequest(roomId, content, memory)
        //   - bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {}).timeout(60s)
        //   이벤트 분기:
        //     "chunk" → ChatStreamEvent.Chunk(content)
        //     "done"  → objectMapper.readValue(data, FastApiDonePayload.class)
        //               → ChatStreamEvent.Done(payload)
        //               IllegalArgumentException → 502 Bad Gateway
        //     unknown event type → log.warn + Mono.empty()
        //   에러 매핑:
        //     TimeoutException           → 504 Gateway Timeout
        //     WebClientRequestException  → 503 Service Unavailable
        //     WebClientResponseException → 502 Bad Gateway
        return Flux.error(new UnsupportedOperationException("AI integration pending"));
    }
}
