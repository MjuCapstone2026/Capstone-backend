package com.mju.capstone_backend.domain.reservation.controller;

import com.mju.capstone_backend.domain.reservation.dto.GetReservationsResponse;
import com.mju.capstone_backend.domain.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
@DisplayName("ReservationController 슬라이스 테스트")
class ReservationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReservationService reservationService;

    private static final String CLERK_ID = "user_testClerkId";
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID ITINERARY_ID = UUID.randomUUID();

    // ─── GET /api/v1/reservations ─────────────────────────────────────────────

    @Test
    @DisplayName("예약 목록 조회 - 유효한 JWT로 200 반환 및 응답 데이터 확인")
    void getReservations_withValidJwt_returns200() {
        GetReservationsResponse response = new GetReservationsResponse(List.of(
                new GetReservationsResponse.ReservationDto(
                        RESERVATION_ID, ITINERARY_ID, "flight", "confirmed", "ai",
                        null, "KE-20260501-001", Map.of(), new BigDecimal("350000"), "KRW",
                        OffsetDateTime.now(), null, OffsetDateTime.now(), OffsetDateTime.now()
                )
        ));
        when(reservationService.getReservations(CLERK_ID, null, null))
                .thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/reservations")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reservations").isArray()
                .jsonPath("$.reservations[0].reservationId").isEqualTo(RESERVATION_ID.toString())
                .jsonPath("$.reservations[0].itineraryId").isEqualTo(ITINERARY_ID.toString())
                .jsonPath("$.reservations[0].type").isEqualTo("flight")
                .jsonPath("$.reservations[0].status").isEqualTo("confirmed");

        verify(reservationService).getReservations(CLERK_ID, null, null);
    }

    @Test
    @DisplayName("예약 목록 조회 - JWT 없이 요청 시 401 반환")
    void getReservations_withoutJwt_returns401() {
        webTestClient
                .get()
                .uri("/api/v1/reservations")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("예약 목록 조회 - type 필터 적용 시 서비스에 전달 및 응답 확인")
    void getReservations_withTypeFilter_passedToService() {
        GetReservationsResponse response = new GetReservationsResponse(List.of(
                new GetReservationsResponse.ReservationDto(
                        RESERVATION_ID, ITINERARY_ID, "flight", "confirmed", "ai",
                        null, "KE-20260501-001", Map.of(), new BigDecimal("350000"), "KRW",
                        OffsetDateTime.now(), null, OffsetDateTime.now(), OffsetDateTime.now()
                )
        ));
        when(reservationService.getReservations(CLERK_ID, "flight", null))
                .thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/reservations?type=flight")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reservations[0].type").isEqualTo("flight");

        verify(reservationService).getReservations(CLERK_ID, "flight", null);
    }

    @Test
    @DisplayName("예약 목록 조회 - status 필터 적용 시 서비스에 전달 및 응답 확인")
    void getReservations_withStatusFilter_passedToService() {
        GetReservationsResponse response = new GetReservationsResponse(List.of(
                new GetReservationsResponse.ReservationDto(
                        RESERVATION_ID, ITINERARY_ID, "accommodation", "confirmed", "user",
                        null, "LOTTE-20260501", Map.of(), new BigDecimal("180000"), "KRW",
                        OffsetDateTime.now(), null, OffsetDateTime.now(), OffsetDateTime.now()
                )
        ));
        when(reservationService.getReservations(CLERK_ID, null, "confirmed"))
                .thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/reservations?status=confirmed")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reservations[0].status").isEqualTo("confirmed");

        verify(reservationService).getReservations(CLERK_ID, null, "confirmed");
    }

    @Test
    @DisplayName("예약 목록 조회 - 잘못된 type 파라미터 시 400 반환")
    void getReservations_withInvalidType_returns400() {
        when(reservationService.getReservations(eq(CLERK_ID), eq("invalid"), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "type must be one of: flight, accommodation, car_rental.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/reservations?type=invalid")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("예약 목록 조회 - 잘못된 status 파라미터 시 400 반환")
    void getReservations_withInvalidStatus_returns400() {
        when(reservationService.getReservations(eq(CLERK_ID), any(), eq("invalid")))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "status must be one of: confirmed, changed, cancelled.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/reservations?status=invalid")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("예약 목록 조회 - 존재하지 않는 사용자 시 404 반환")
    void getReservations_userNotFound_returns404() {
        when(reservationService.getReservations(CLERK_ID, null, null))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found. Please sign up first.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/reservations")
                .exchange()
                .expectStatus().isNotFound();
    }
}
