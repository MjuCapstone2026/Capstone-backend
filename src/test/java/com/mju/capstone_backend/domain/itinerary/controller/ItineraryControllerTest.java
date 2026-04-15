package com.mju.capstone_backend.domain.itinerary.controller;

import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.service.ItineraryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
@DisplayName("ItineraryController 슬라이스 테스트")
class ItineraryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItineraryService itineraryService;

    private static final String CLERK_ID = "user_testClerkId";

    // ─── GET /api/v1/itineraries ──────────────────────────────────────────────

    @Test
    @DisplayName("일정 목록 조회 - 유효한 JWT로 200 반환")
    void getItineraries_withValidJwt_returns200() {
        GetItinerariesResponse response = new GetItinerariesResponse(List.of(
                new GetItinerariesResponse.ItineraryItem(
                        UUID.randomUUID(), "도쿄 3박 4일 여행", "draft", "도쿄", 4, LocalDate.of(2026, 5, 1)
                )
        ));
        when(itineraryService.getItineraries(CLERK_ID)).thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/itineraries")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.itineraries").isArray()
                .jsonPath("$.itineraries[0].status").isEqualTo("draft")
                .jsonPath("$.itineraries[0].destination").isEqualTo("도쿄");

        verify(itineraryService).getItineraries(CLERK_ID);
    }

    @Test
    @DisplayName("일정 목록 조회 - JWT 없이 요청 시 401 반환")
    void getItineraries_withoutJwt_returns401() {
        webTestClient
                .get()
                .uri("/api/v1/itineraries")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
