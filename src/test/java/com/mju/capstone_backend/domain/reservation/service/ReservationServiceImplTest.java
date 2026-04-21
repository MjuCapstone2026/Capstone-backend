package com.mju.capstone_backend.domain.reservation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.reservation.entity.Reservation;
import com.mju.capstone_backend.domain.reservation.repository.ReservationRepository;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationServiceImpl 단위 테스트")
class ReservationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private static final String CLERK_ID = "user_testClerkId";

    @BeforeEach
    void injectScheduler() throws Exception {
        var field = ReservationServiceImpl.class.getDeclaredField("dbScheduler");
        field.setAccessible(true);
        field.set(reservationService, Schedulers.immediate());
    }

    // ─── 사용자 검증 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 사용자 - 404 예외")
    void getReservations_userNotFound_throws404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(false);

        StepVerifier.create(reservationService.getReservations(CLERK_ID, null, null))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();

        verify(reservationRepository, never()).findByClerkIdWithFilters(any(), any(), any());
    }

    // ─── 파라미터 검증 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("잘못된 type 파라미터 - 400 예외")
    void getReservations_invalidType_throws400() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);

        StepVerifier.create(reservationService.getReservations(CLERK_ID, "invalid", null))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("잘못된 status 파라미터 - 400 예외")
    void getReservations_invalidStatus_throws400() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);

        StepVerifier.create(reservationService.getReservations(CLERK_ID, null, "invalid"))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    // ─── 정상 조회 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("필터 없이 조회 - 전체 예약 목록 반환")
    void getReservations_noFilters_returnsAll() throws Exception {
        Reservation mockReservation = buildMockReservation("flight", "confirmed");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(reservationRepository.findByClerkIdWithFilters(CLERK_ID, null, null))
                .thenReturn(List.of(mockReservation));
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of());

        StepVerifier.create(reservationService.getReservations(CLERK_ID, null, null))
                .assertNext(response -> {
                    assertThat(response.reservations()).hasSize(1);
                    assertThat(response.reservations().get(0).type()).isEqualTo("flight");
                    assertThat(response.reservations().get(0).status()).isEqualTo("confirmed");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("type 필터 적용 - 리포지토리에 type 전달")
    void getReservations_withTypeFilter_passedToRepository() throws Exception {
        Reservation mockReservation = buildMockReservation("accommodation", "confirmed");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(reservationRepository.findByClerkIdWithFilters(CLERK_ID, "accommodation", null))
                .thenReturn(List.of(mockReservation));
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of());

        StepVerifier.create(reservationService.getReservations(CLERK_ID, "accommodation", null))
                .assertNext(response -> assertThat(response.reservations()).hasSize(1))
                .verifyComplete();

        verify(reservationRepository).findByClerkIdWithFilters(CLERK_ID, "accommodation", null);
    }

    @Test
    @DisplayName("결과 없음 - 빈 목록 반환")
    void getReservations_noResults_returnsEmptyList() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(reservationRepository.findByClerkIdWithFilters(CLERK_ID, null, null))
                .thenReturn(List.of());

        StepVerifier.create(reservationService.getReservations(CLERK_ID, null, null))
                .assertNext(response -> assertThat(response.reservations()).isEmpty())
                .verifyComplete();
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private Reservation buildMockReservation(String type, String status) {
        Reservation r = mock(Reservation.class);
        when(r.getId()).thenReturn(UUID.randomUUID());
        when(r.getItineraryId()).thenReturn(UUID.randomUUID());
        when(r.getType()).thenReturn(type);
        when(r.getStatus()).thenReturn(status);
        when(r.getBookedBy()).thenReturn("ai");
        when(r.getBookingUrl()).thenReturn(null);
        when(r.getExternalRefId()).thenReturn("REF-001");
        when(r.getDetail()).thenReturn("{}");
        when(r.getTotalPrice()).thenReturn(BigDecimal.valueOf(350000));
        when(r.getCurrency()).thenReturn("KRW");
        when(r.getReservedAt()).thenReturn(OffsetDateTime.now());
        when(r.getCancelledAt()).thenReturn(null);
        when(r.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(r.getUpdatedAt()).thenReturn(OffsetDateTime.now());
        return r;
    }
}
