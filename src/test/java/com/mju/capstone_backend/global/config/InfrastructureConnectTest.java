package com.mju.capstone_backend.global.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env") // 로컬 .env 파일을 읽으라고 명시
@DisplayName("인프라 연결 통합 테스트")
class InfrastructureConnectTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("PostgreSQL(Supabase) 연결 확인")
    void postgresConnectionTest() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assertions.assertEquals(1, result);
    }

    @Test
    @DisplayName("Redis(Upstash) 연결 확인")
    void redisConnectionTest() {
        String key = "test:" + UUID.randomUUID();
        redisTemplate.opsForValue().set(key, "ok");
        Assertions.assertEquals("ok", redisTemplate.opsForValue().get(key));
        redisTemplate.delete(key);
    }
}