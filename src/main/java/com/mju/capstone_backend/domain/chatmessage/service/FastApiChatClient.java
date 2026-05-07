package com.mju.capstone_backend.domain.chatmessage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatmessage.dto.ChatStreamEvent;
import com.mju.capstone_backend.domain.chatmessage.dto.FastApiChatRequest;
import com.mju.capstone_backend.domain.chatmessage.dto.FastApiDonePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiChatClient {

    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.internal-token:}")
    private String internalToken;

    public Flux<ChatStreamEvent> stream(UUID roomId, String content, FastApiChatRequest.MemoryPayload memory) {
        return aiWebClient.post()
                .uri("/api/v1/ai-messages")
                .header("X-Internal-Token", internalToken)
                .bodyValue(new FastApiChatRequest(roomId, content, memory))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .timeout(Duration.ofSeconds(180))
                .concatMap(event -> {
                    String type = event.event();
                    String data = event.data();
                    if ("chunk".equals(type)) {
                        return parseChunk(data);
                    } else if ("done".equals(type)) {
                        return parseDone(data);
                    } else if ("error".equals(type)) {
                        log.error("AI server SSE error: {}", data);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                "AI server reported an error."));
                    } else {
                        log.warn("Unknown SSE event type from AI server: {}", type);
                        return Mono.empty();
                    }
                })
                .onErrorMap(TimeoutException.class, e ->
                        new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI server timed out."))
                .onErrorMap(WebClientRequestException.class, e ->
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI server is unavailable."))
                .onErrorMap(WebClientResponseException.class, e ->
                        new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI server returned an error response."))
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected error from AI server."));
    }

    private Mono<ChatStreamEvent> parseChunk(String data) {
        try {
            String content = objectMapper.readTree(data).path("content").asText();
            return Mono.just(new ChatStreamEvent.Chunk(content));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse chunk data: {}", data);
            return Mono.empty();
        }
    }

    private Mono<ChatStreamEvent> parseDone(String data) {
        try {
            FastApiDonePayload payload = objectMapper.readValue(data, FastApiDonePayload.class);
            return Mono.just(new ChatStreamEvent.Done(payload));
        } catch (IllegalArgumentException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unknown AI response type: " + e.getMessage()));
        } catch (JsonProcessingException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Invalid done payload format from AI server."));
        }
    }
}
