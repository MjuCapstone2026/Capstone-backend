package com.mju.capstone_backend.global.dev;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 개발용 JWT 발급 엔드포인트.
 *
 * clerkId → Clerk 로그인 → JWT 반환 흐름:
 *   1. Backend API  POST /v1/sign_in_tokens           → 로그인용 ticket 발급
 *   2. Frontend API POST /v1/client/sign_ins          → ticket으로 세션 생성, session_id 획득
 *   3. Backend API  POST /v1/sessions/{id}/tokens     → 신선한 JWT 발급
 *
 * @Profile("dev") — 운영 환경에서는 빈 자체가 등록되지 않음.
 */
@Slf4j
@Tag(name = "[DEV] Auth API")
@RestController
@RequestMapping("/dev/auth")
@Profile("dev")
public class DevAuthController {

    record TokenRequest(String clerkId) {}

    @Value("${clerk.secret-key}")
    private String clerkSecretKey;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String clerkFrontendApiUrl;

    private final WebClient backendApiClient = WebClient.create("https://api.clerk.com");

    @Operation(
            summary = "[DEV] Clerk JWT 발급",
            description = "clerkId를 받아 Clerk에 로그인하고 JWT를 반환한다."
    )
    @PostMapping("/token")
    public Mono<Map<String, String>> getDevToken(@RequestBody TokenRequest request) {
        String userId = request.clerkId();

        // Step 1: Backend API — 로그인용 ticket 발급
        return backendApiClient.post()
                .uri("/v1/sign_in_tokens")
                .header("Authorization", "Bearer " + clerkSecretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("user_id", userId, "expires_in_seconds", 300))
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(res -> log.info("[Step1] sign_in_tokens 응답: {}", res))
                .flatMap(signInTokenResponse -> {
                    String ticket = (String) signInTokenResponse.get("token");

                    // Step 2: Frontend API — ticket으로 세션 생성, session_id 획득
                    String frontendApiUrl = clerkFrontendApiUrl.stripTrailing().replaceAll("/$", "");
                    return WebClient.create(frontendApiUrl)
                            .post()
                            .uri("/v1/client/sign_ins")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(BodyInserters.fromFormData("strategy", "ticket")
                                    .with("ticket", ticket))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .doOnNext(res -> log.info("[Step2] sign_ins 응답: {}", res));
                })
                .flatMap(signInResponse -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = (Map<String, Object>) signInResponse.get("response");
                    String sessionId = (String) response.get("created_session_id");
                    log.info("[Step3] 발급할 sessionId: {}", sessionId);

                    // Step 3: Backend API — 해당 세션으로 신선한 JWT 발급
                    return backendApiClient.post()
                            .uri("/v1/sessions/{sessionId}/tokens", sessionId)
                            .header("Authorization", "Bearer " + clerkSecretKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .doOnNext(res -> log.info("[Step3] tokens 응답 수신 완료"));
                })
                .map(tokenResponse -> Map.of("token", (String) tokenResponse.get("jwt")))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("[Clerk API 오류] status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.just(Map.of(
                            "error", "Clerk API 오류: " + e.getStatusCode(),
                            "detail", e.getResponseBodyAsString()
                    ));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("[오류] {}", e.getMessage(), e);
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }
}
