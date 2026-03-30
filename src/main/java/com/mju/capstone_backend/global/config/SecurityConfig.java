package com.mju.capstone_backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(corsSpec -> {}) // WebConfig의 CORS 설정 사용
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/test/**").permitAll()         // 테스트 엔드포인트 인증 제외
                        .pathMatchers("/v3/api-docs/**").permitAll()      // Swagger
                        .pathMatchers("/swagger-ui/**").permitAll()       // Swagger UI
                        .anyExchange().authenticated()                    // 나머지는 JWT 필요
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {}) // application.properties의 Clerk JWKS 설정 사용
                )
                .build();
    }
}
