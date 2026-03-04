package com.mju.capstone_backend.domain.test.controller; // 수정한 패키지 경로

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
}