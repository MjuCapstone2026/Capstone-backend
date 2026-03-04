package com.mju.capstone_backend.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
@DisplayName("Redis 연결 설정 테스트")
class RedisConfigTest {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Test
    @DisplayName("Redis(Upstash) 연결 및 동작 확인")
    void redisConnectionTest() {
        String key = "test:" + UUID.randomUUID();
        String value = "ok";

        // 1. 비동기로 데이터 저장 및 확인 (StepVerifier 사용)
        reactiveRedisTemplate.opsForValue().set(key, value)
                .then(reactiveRedisTemplate.opsForValue().get(key))
                .as(StepVerifier::create)
                .expectNext("ok")
                .verifyComplete();

        // 2. 테스트 데이터 삭제 (마무리)
        reactiveRedisTemplate.delete(key).block();
    }
}
