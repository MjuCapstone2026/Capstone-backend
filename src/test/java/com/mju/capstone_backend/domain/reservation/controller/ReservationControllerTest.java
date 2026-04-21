package com.mju.capstone_backend.domain.reservation.controller;

import com.mju.capstone_backend.domain.reservation.dto.CreateReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.CreateReservationResponse;
import com.mju.capstone_backend.domain.reservation.dto.GetReservationsResponse;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationResponse;
import com.mju.capstone_backend.domain.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    // ─── POST /api/v1/reservations ────────────────────────────────────────────

    @Test
    @DisplayName("예약 생성 - 유효한 JWT + 정상 body - 201 반환 및 응답 확인")
    void createReservation_withValidJwt_returns201() {
        CreateReservationResponse response = new CreateReservationResponse(
                RESERVATION_ID, ITINERARY_ID, "flight", "confirmed", OffsetDateTime.now()
        );
        when(reservationService.createReservation(eq(CLERK_ID), any(CreateReservationRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.reservationId").isEqualTo(RESERVATION_ID.toString())
                .jsonPath("$.itineraryId").isEqualTo(ITINERARY_ID.toString())
                .jsonPath("$.type").isEqualTo("flight")
                .jsonPath("$.status").isEqualTo("confirmed");

        verify(reservationService).createReservation(eq(CLERK_ID), any(CreateReservationRequest.class));
    }

    @Test
    @DisplayName("예약 생성 - JWT 없이 요청 시 401 반환")
    void createReservation_withoutJwt_returns401() {
        webTestClient
                .post()
                .uri("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("예약 생성 - itineraryId 누락 시 400 반환")
    void createReservation_missingItineraryId_returns400() {
        Map<String, Object> body = Map.of(
                "type", "flight",
                "bookedBy", "ai",
                "detail", Map.of("airline", "대한항공")
        );

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("예약 생성 - 존재하지 않는 itinerary - 404 반환")
    void createReservation_itineraryNotFound_returns404() {
        when(reservationService.createReservation(eq(CLERK_ID), any(CreateReservationRequest.class)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Itinerary not found.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("예약 생성 - 다른 사용자의 itinerary - 403 반환")
    void createReservation_notOwner_returns403() {
        when(reservationService.createReservation(eq(CLERK_ID), any(CreateReservationRequest.class)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to create a reservation for this itinerary.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody())
                .exchange()
                .expectStatus().isForbidden();
    }

    // ─── PATCH /api/v1/reservations/{reservationId} ───────────────────────────

    @Test
    @DisplayName("예약 수정 - 유효한 JWT + 정상 body - 200 반환 및 응답 확인")
    void updateReservation_withValidJwt_returns200() {
        PatchReservationResponse response = new PatchReservationResponse(
                RESERVATION_ID, "cancelled", OffsetDateTime.now()
        );
        when(reservationService.updateReservation(eq(CLERK_ID), eq(RESERVATION_ID), any(PatchReservationRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .patch()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "cancelled", "cancelledAt", "2026-04-10T10:00:00+09:00"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reservationId").isEqualTo(RESERVATION_ID.toString())
                .jsonPath("$.status").isEqualTo("cancelled");

        verify(reservationService).updateReservation(eq(CLERK_ID), eq(RESERVATION_ID), any(PatchReservationRequest.class));
    }

    @Test
    @DisplayName("예약 수정 - JWT 없이 요청 시 401 반환")
    void updateReservation_withoutJwt_returns401() {
        webTestClient
                .patch()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "cancelled"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("예약 수정 - 존재하지 않는 예약 - 404 반환")
    void updateReservation_reservationNotFound_returns404() {
        when(reservationService.updateReservation(eq(CLERK_ID), eq(RESERVATION_ID), any(PatchReservationRequest.class)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reservation not found.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .patch()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "cancelled"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("예약 수정 - 다른 사용자의 예약 - 403 반환")
    void updateReservation_notOwner_returns403() {
        when(reservationService.updateReservation(eq(CLERK_ID), eq(RESERVATION_ID), any(PatchReservationRequest.class)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to update this reservation.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .patch()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "cancelled"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("예약 수정 - 빈 body - 400 반환")
    void updateReservation_emptyBody_returns400() {
        when(reservationService.updateReservation(eq(CLERK_ID), eq(RESERVATION_ID), any(PatchReservationRequest.class)))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "At least one field must be provided.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .patch()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ─── DELETE /api/v1/reservations/{reservationId} ─────────────────────────

    @Test
    @DisplayName("예약 삭제 - 유효한 JWT - 204 반환")
    void deleteReservation_withValidJwt_returns204() {
        when(reservationService.deleteReservation(CLERK_ID, RESERVATION_ID))
                .thenReturn(Mono.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .delete()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .exchange()
                .expectStatus().isNoContent();

        verify(reservationService).deleteReservation(CLERK_ID, RESERVATION_ID);
    }

    @Test
    @DisplayName("예약 삭제 - JWT 없이 요청 시 401 반환")
    void deleteReservation_withoutJwt_returns401() {
        webTestClient
                .delete()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("예약 삭제 - 존재하지 않는 예약 - 404 반환")
    void deleteReservation_reservationNotFound_returns404() {
        when(reservationService.deleteReservation(CLERK_ID, RESERVATION_ID))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reservation not found.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .delete()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("예약 삭제 - 다른 사용자의 예약 - 403 반환")
    void deleteReservation_notOwner_returns403() {
        when(reservationService.deleteReservation(CLERK_ID, RESERVATION_ID))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to delete this reservation.")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .delete()
                .uri("/api/v1/reservations/" + RESERVATION_ID)
                .exchange()
                .expectStatus().isForbidden();
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private Map<String, Object> buildRequestBody() {
        return Map.of(
                "itineraryId", ITINERARY_ID.toString(),
                "type", "flight",
                "bookedBy", "ai",
                "bookingUrl", "https://booking.example.com/flight/123",
                "externalRefId", "KE12345678",
                "detail", Map.of(
                        "airline", "대한항공",
                        "flight_no", "KE123",
                        "departure", Map.of("airport", "ICN", "datetime", "2026-05-01T09:00:00"),
                        "arrival", Map.of("airport", "NRT", "datetime", "2026-05-01T11:30:00"),
                        "seat_class", "economy",
                        "passengers", List.of(Map.of("name", "홍길동", "passport", "M12345678"))
                ),
                "totalPrice", 320000.00,
                "currency", "KRW"
        );
    }
}
