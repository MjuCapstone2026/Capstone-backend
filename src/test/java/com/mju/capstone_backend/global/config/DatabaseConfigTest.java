package com.mju.capstone_backend.global.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Scheduler;
import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
@DisplayName("데이터베이스 연결 설정 테스트")
class DatabaseConfigTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Scheduler dbScheduler;

    @Test
    @DisplayName("PostgreSQL(Supabase) 연결 확인")
    void postgresConnectionTest() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assertions.assertEquals(1, result);
    }

    @Test
    @DisplayName("HikariCP 커넥션 풀 사이즈가 .env 설정값(15)과 일치하는지 확인")
    void hikariPoolSizeTest() {
        // DataSource 인스턴스가 HikariDataSource인지 확인 후 형변환
        Assertions.assertTrue(dataSource instanceof HikariDataSource);
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

        // 열쇠(커넥션) 개수 검증
        Assertions.assertEquals(15, hikariDataSource.getMaximumPoolSize());
    }

    @Test
    @DisplayName("DB 전용 스케줄러(일꾼) 빈이 정상적으로 생성되었는지 확인")
    void dbSchedulerBeanTest() {
        // 일꾼 부대가 준비되었는지 검증
        Assertions.assertNotNull(dbScheduler);
    }
}
