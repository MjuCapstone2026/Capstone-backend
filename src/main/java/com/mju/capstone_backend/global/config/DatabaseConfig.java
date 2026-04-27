package com.mju.capstone_backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import java.util.concurrent.Executors;

@Configuration
public class DatabaseConfig {

    @Value("${DB_POOL_SIZE:20}") // .env에서 읽어오되 없으면 20 기본값
    private int dbPoolSize;

    @Bean
    public Scheduler dbScheduler() {
        // 일꾼 20명을 생성하여 properties에서 설정한 커넥션 20개와 1:1 대응시킴
        return Schedulers.fromExecutor(Executors.newFixedThreadPool(dbPoolSize));
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}