package com.mju.capstone_backend.global.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env") // 로컬 .env 파일을 읽으라고 명시
@DisplayName("인프라 연결 통합 테스트")
class InfrastructureConnectTest {

    @Autowired
    private JdbcTemplate jdbcTemplate; // 동기 DB 주입

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate; // 비동기 레디스 주입

    @Test
    @DisplayName("PostgreSQL(Supabase) 연결 확인")
    void postgresConnectionTest() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assertions.assertEquals(1, result);
    }

    @Test
    @DisplayName("Redis(Upstash) 연결 및 동작 확인")
    void redisConnectionTest() {
        String key = "test:" + UUID.randomUUID();
        String value = "ok";

        // 1. 비동기로 데이터 저장 및 확인 (StepVerifier 사용)
        reactiveRedisTemplate.opsForValue().set(key, value) // Mono<Boolean> 반환
                .then(reactiveRedisTemplate.opsForValue().get(key)) // 저장 후 바로 조회 Mono<String>
                .as(StepVerifier::create) // 비동기 스트림 검증 시작
                .expectNext("ok") // "ok"가 올 것을 기대함
                .verifyComplete(); // 스트림이 정상 종료되는지 확인

        // 2. 테스트 데이터 삭제 (마무리)
        reactiveRedisTemplate.delete(key).block(); // 테스트 종료를 위해 여기서만 살짝 block 허용
    }
}