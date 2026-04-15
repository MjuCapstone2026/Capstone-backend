package com.mju.capstone_backend.domain.itinerary.controller;

import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.dto.GetItineraryResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
    private static final UUID ITINERARY_ID = UUID.randomUUID();

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

    // ─── GET /api/v1/itineraries/{itineraryId} ────────────────────────────────

    @Test
    @DisplayName("일정 상세 조회 - 유효한 JWT로 200 반환")
    void getItinerary_withValidJwt_returns200() {
        GetItineraryResponse response = new GetItineraryResponse(
                ITINERARY_ID, "서울 3박 4일 여행", "draft", "서울",
                BigDecimal.valueOf(500000), 2, 1, List.of(5),
                4, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4),
                Map.of("2026-05-01", List.of(
                        Map.of("index", 0, "time", "09:00 ~ 11:00", "plan_name", "경복궁",
                                "place", "경복궁", "note", "", "status", "done")
                )),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(itineraryService.getItinerary(CLERK_ID, ITINERARY_ID)).thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/itineraries/{itineraryId}", ITINERARY_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.itineraryId").isEqualTo(ITINERARY_ID.toString())
                .jsonPath("$.name").isEqualTo("서울 3박 4일 여행")
                .jsonPath("$.dayPlans['2026-05-01'][0].index").isEqualTo(0);

        verify(itineraryService).getItinerary(CLERK_ID, ITINERARY_ID);
    }

    @Test
    @DisplayName("일정 상세 조회 - JWT 없이 요청 시 401 반환")
    void getItinerary_withoutJwt_returns401() {
        webTestClient
                .get()
                .uri("/api/v1/itineraries/{itineraryId}", ITINERARY_ID)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
