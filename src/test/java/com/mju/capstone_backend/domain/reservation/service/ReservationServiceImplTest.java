package com.mju.capstone_backend.domain.reservation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.reservation.dto.CreateReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationRequest;
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
import java.util.Optional;
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
    private ItineraryRepository itineraryRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private static final String CLERK_ID = "user_testClerkId";
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID ITINERARY_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();

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

    // ─── createReservation: 사용자/파라미터 검증 ──────────────────────────────

    @Test
    @DisplayName("예약 생성 - 존재하지 않는 사용자 - 404 예외")
    void createReservation_userNotFound_throws404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(false);

        StepVerifier.create(reservationService.createReservation(CLERK_ID, buildCreateRequest("flight", "ai")))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();

        verify(itineraryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("예약 생성 - 잘못된 type - 400 예외")
    void createReservation_invalidType_throws400() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);

        StepVerifier.create(reservationService.createReservation(CLERK_ID, buildCreateRequest("train", "ai")))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST
                        && rse.getReason().equals("type must be one of: flight, accommodation, car_rental."))
                .verify();
    }

    @Test
    @DisplayName("예약 생성 - 잘못된 bookedBy - 400 예외")
    void createReservation_invalidBookedBy_throws400() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);

        StepVerifier.create(reservationService.createReservation(CLERK_ID, buildCreateRequest("flight", "bot")))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST
                        && rse.getReason().equals("bookedBy must be one of: user, ai."))
                .verify();
    }

    // ─── createReservation: 소유권 검증 ──────────────────────────────────────

    @Test
    @DisplayName("예약 생성 - 존재하지 않는 itinerary - 404 예외")
    void createReservation_itineraryNotFound_throws404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.empty());

        StepVerifier.create(reservationService.createReservation(CLERK_ID, buildCreateRequest("flight", "ai")))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND
                        && rse.getReason().equals("Itinerary not found."))
                .verify();
    }

    @Test
    @DisplayName("예약 생성 - 다른 사용자의 itinerary - 403 예외")
    void createReservation_notOwner_throws403() {
        Itinerary mockItinerary = mock(Itinerary.class);
        when(mockItinerary.getRoomId()).thenReturn(ROOM_ID);
        ChatRoom otherChatRoom = mock(ChatRoom.class);
        when(otherChatRoom.getClerkId()).thenReturn("user_otherClerkId");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(mockItinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(otherChatRoom));

        StepVerifier.create(reservationService.createReservation(CLERK_ID, buildCreateRequest("flight", "ai")))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    // ─── createReservation: 정상 생성 ────────────────────────────────────────

    @Test
    @DisplayName("예약 생성 - 정상 요청 - 생성된 예약 반환")
    void createReservation_success_returnsResponse() throws Exception {
        Itinerary mockItinerary = mock(Itinerary.class);
        when(mockItinerary.getRoomId()).thenReturn(ROOM_ID);
        ChatRoom mockChatRoom = mock(ChatRoom.class);
        when(mockChatRoom.getClerkId()).thenReturn(CLERK_ID);
        Reservation mockReservation = mock(Reservation.class);
        when(mockReservation.getId()).thenReturn(UUID.randomUUID());
        when(mockReservation.getItineraryId()).thenReturn(ITINERARY_ID);
        when(mockReservation.getType()).thenReturn("flight");
        when(mockReservation.getStatus()).thenReturn("confirmed");
        when(mockReservation.getCreatedAt()).thenReturn(OffsetDateTime.now());

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(mockItinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(mockChatRoom));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"airline\":\"대한항공\"}");
        when(reservationRepository.save(any())).thenReturn(mockReservation);

        StepVerifier.create(reservationService.createReservation(CLERK_ID, buildCreateRequest("flight", "ai")))
                .assertNext(response -> {
                    assertThat(response.reservationId()).isNotNull();
                    assertThat(response.type()).isEqualTo("flight");
                    assertThat(response.status()).isEqualTo("confirmed");
                })
                .verifyComplete();

        verify(reservationRepository).save(any());
    }

    // ─── updateReservation: 파라미터 검증 ────────────────────────────────────

    @Test
    @DisplayName("예약 수정 - 빈 body - 400 예외")
    void updateReservation_emptyBody_throws400() {
        PatchReservationRequest emptyRequest = new PatchReservationRequest(null, null, null, null, null, null);

        StepVerifier.create(reservationService.updateReservation(CLERK_ID, RESERVATION_ID, emptyRequest))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST
                        && rse.getReason().equals("At least one field must be provided."))
                .verify();
    }

    @Test
    @DisplayName("예약 수정 - 잘못된 status - 400 예외")
    void updateReservation_invalidStatus_throws400() {
        PatchReservationRequest request = new PatchReservationRequest("invalid", null, null, null, null, null);

        StepVerifier.create(reservationService.updateReservation(CLERK_ID, RESERVATION_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST
                        && rse.getReason().equals("status must be one of: confirmed, changed, cancelled."))
                .verify();
    }

    // ─── updateReservation: 소유권 검증 ──────────────────────────────────────

    @Test
    @DisplayName("예약 수정 - 존재하지 않는 예약 - 404 예외")
    void updateReservation_reservationNotFound_throws404() {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        StepVerifier.create(reservationService.updateReservation(CLERK_ID, RESERVATION_ID,
                        new PatchReservationRequest("cancelled", null, null, null, null, null)))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND
                        && rse.getReason().equals("Reservation not found."))
                .verify();
    }

    @Test
    @DisplayName("예약 수정 - 다른 사용자의 예약 - 403 예외")
    void updateReservation_notOwner_throws403() {
        Reservation mockReservation = mock(Reservation.class);
        when(mockReservation.getItineraryId()).thenReturn(ITINERARY_ID);
        Itinerary mockItinerary = mock(Itinerary.class);
        when(mockItinerary.getRoomId()).thenReturn(ROOM_ID);
        ChatRoom otherChatRoom = mock(ChatRoom.class);
        when(otherChatRoom.getClerkId()).thenReturn("user_otherClerkId");

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(mockReservation));
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(mockItinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(otherChatRoom));

        StepVerifier.create(reservationService.updateReservation(CLERK_ID, RESERVATION_ID,
                        new PatchReservationRequest("cancelled", null, null, null, null, null)))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    // ─── updateReservation: 정상 수정 ────────────────────────────────────────

    @Test
    @DisplayName("예약 수정 - 상태 변경 - 수정된 예약 반환")
    void updateReservation_statusChange_returnsResponse() {
        Reservation mockReservation = mock(Reservation.class);
        when(mockReservation.getItineraryId()).thenReturn(ITINERARY_ID);
        when(mockReservation.getId()).thenReturn(RESERVATION_ID);
        when(mockReservation.getStatus()).thenReturn("cancelled");
        when(mockReservation.getUpdatedAt()).thenReturn(OffsetDateTime.now());
        Itinerary mockItinerary = mock(Itinerary.class);
        when(mockItinerary.getRoomId()).thenReturn(ROOM_ID);
        ChatRoom mockChatRoom = mock(ChatRoom.class);
        when(mockChatRoom.getClerkId()).thenReturn(CLERK_ID);

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(mockReservation));
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(mockItinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(mockChatRoom));
        when(reservationRepository.save(any())).thenReturn(mockReservation);

        StepVerifier.create(reservationService.updateReservation(CLERK_ID, RESERVATION_ID,
                        new PatchReservationRequest("cancelled", null, null, null, null, OffsetDateTime.now())))
                .assertNext(response -> {
                    assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
                    assertThat(response.status()).isEqualTo("cancelled");
                    assertThat(response.updatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(reservationRepository).save(mockReservation);
    }

    // ─── deleteReservation ────────────────────────────────────────────────────

    @Test
    @DisplayName("예약 삭제 - 존재하지 않는 예약 - 404 예외")
    void deleteReservation_reservationNotFound_throws404() {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        StepVerifier.create(reservationService.deleteReservation(CLERK_ID, RESERVATION_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND
                        && rse.getReason().equals("Reservation not found."))
                .verify();
    }

    @Test
    @DisplayName("예약 삭제 - 다른 사용자의 예약 - 403 예외")
    void deleteReservation_notOwner_throws403() {
        Reservation mockReservation = mock(Reservation.class);
        when(mockReservation.getItineraryId()).thenReturn(ITINERARY_ID);
        Itinerary mockItinerary = mock(Itinerary.class);
        when(mockItinerary.getRoomId()).thenReturn(ROOM_ID);
        ChatRoom otherChatRoom = mock(ChatRoom.class);
        when(otherChatRoom.getClerkId()).thenReturn("user_otherClerkId");

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(mockReservation));
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(mockItinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(otherChatRoom));

        StepVerifier.create(reservationService.deleteReservation(CLERK_ID, RESERVATION_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();

        verify(reservationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("예약 삭제 - 정상 요청 - delete 호출 후 완료")
    void deleteReservation_success_completesAndCallsDelete() {
        Reservation mockReservation = mock(Reservation.class);
        when(mockReservation.getItineraryId()).thenReturn(ITINERARY_ID);
        Itinerary mockItinerary = mock(Itinerary.class);
        when(mockItinerary.getRoomId()).thenReturn(ROOM_ID);
        ChatRoom mockChatRoom = mock(ChatRoom.class);
        when(mockChatRoom.getClerkId()).thenReturn(CLERK_ID);

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(mockReservation));
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(mockItinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(mockChatRoom));

        StepVerifier.create(reservationService.deleteReservation(CLERK_ID, RESERVATION_ID))
                .verifyComplete();

        verify(reservationRepository).delete(mockReservation);
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private CreateReservationRequest buildCreateRequest(String type, String bookedBy) {
        return new CreateReservationRequest(
                ITINERARY_ID,
                type,
                bookedBy,
                "https://booking.example.com/flight/123",
                "KE12345678",
                Map.of("airline", "대한항공", "flight_no", "KE123"),
                new BigDecimal("320000"),
                "KRW",
                OffsetDateTime.now()
        );
    }

    private Itinerary buildMockItinerary() {
        Itinerary itinerary = mock(Itinerary.class);
        when(itinerary.getRoomId()).thenReturn(ROOM_ID);
        return itinerary;
    }

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
