package com.mju.capstone_backend.domain.test.controller; // 수정한 패키지 경로

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Tag(name = "Test API", description = "프론트-자바-파이썬 통신 확인을 위한 테스트용 API")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    @Value("${ai.agent.url}")
    private String aiAgentUrl; // 로그 찍기용 url 변수
    private final WebClient webClient;

    @Operation(summary = "서버 연결 테스트 (인증 없음)", description = "Spring Boot → FastAPI 통신 확인용")
    @GetMapping("/connect")
    public Mono<String> connectToPython() {
        String javaMessage = "✅ [Spring Boot 응답]: " + aiAgentUrl + " 로 토스합니다.\n";

        return webClient.get()
                .uri("/api/test/python-test")
                .retrieve()
                .bodyToMono(String.class)
                .map(pythonResponse -> javaMessage + "✅ [FastAPI 응답]: " + pythonResponse)
                .onErrorResume(e -> Mono.just(javaMessage + "⚠️ [Error]: 파이썬 서버 연결 실패!"));
    }

    @Operation(summary = "JWT 인증 확인 (인증 필요)", description = "현재 JWT에서 clerkId와 이름을 반환한다.")
    @GetMapping("/me")
    public Mono<String> me(JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        String name = authentication.getToken().getClaimAsString("name");
        return Mono.just("clerkId: " + clerkId + ", name: " + name);
    }

    @Operation(summary = "JWT 토큰 전달 테스트", description = "JWT에서 사용자 이름 출력 후 FastAPI에 토큰 전달하여 응답 확인")
    @GetMapping("/auth-connect")
    public Mono<String> authConnect(JwtAuthenticationToken authentication) {
        String name = authentication.getToken().getClaimAsString("name");
        String token = authentication.getToken().getTokenValue();

        String javaMessage = "✅ [Spring Boot 응답]: 사용자 이름 = " + name + ", " + aiAgentUrl + " 로 토스합니다.\n";

        return webClient.get()
                .uri("/api/test/auth-test")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .map(pythonResponse -> javaMessage + "✅ [FastAPI 응답]: " + pythonResponse)
                .onErrorResume(e -> Mono.just(javaMessage + "⚠️ [Error]: 파이썬 서버 연결 실패!"));
    }
}