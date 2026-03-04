package com.mju.capstone_backend.global.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
@DisplayName("WebClient 설정 테스트")
class WebClientConfigTest {

    @Autowired
    private WebClient aiWebClient;

    @Test
    @DisplayName("aiWebClient 빈 생성 확인")
    void aiWebClientBeanTest() {
        Assertions.assertNotNull(aiWebClient);
    }
}
