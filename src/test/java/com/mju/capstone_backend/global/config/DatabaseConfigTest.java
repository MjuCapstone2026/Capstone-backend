package com.mju.capstone_backend.global.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
@DisplayName("데이터베이스 연결 설정 테스트")
class DatabaseConfigTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("PostgreSQL(Supabase) 연결 확인")
    void postgresConnectionTest() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assertions.assertEquals(1, result);
    }
}
