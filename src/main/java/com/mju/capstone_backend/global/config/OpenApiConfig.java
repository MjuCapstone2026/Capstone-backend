package com.mju.capstone_backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("MJU AI Agent API Document")
                .version("v1.0.0")
                .description("명지대학교 캡스톤 디자인 백엔드 API 명세서입니다.");

        // 로컬 서버 정보 추가
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Server");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}