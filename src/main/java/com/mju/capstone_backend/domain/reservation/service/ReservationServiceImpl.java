package com.mju.capstone_backend.domain.reservation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.reservation.dto.CreateReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.CreateReservationResponse;
import com.mju.capstone_backend.domain.reservation.dto.GetReservationsResponse;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationResponse;
import com.mju.capstone_backend.domain.reservation.entity.Reservation;
import com.mju.capstone_backend.domain.reservation.repository.ReservationRepository;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final Set<String> VALID_TYPES = Set.of("flight", "accommodation", "car_rental");
    private static final Set<String> VALID_STATUSES = Set.of("confirmed", "changed", "cancelled");
    private static final Set<String> VALID_BOOKED_BY = Set.of("user", "ai");

    private final UserRepository userRepository;
    private final ItineraryRepository itineraryRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ReservationRepository reservationRepository;
    private final ObjectMapper objectMapper;
    private final Scheduler dbScheduler;

    @Override
    public Mono<GetReservationsResponse> getReservations(String clerkId, String type, String status) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please sign up first.");
            }

            if (type != null && !VALID_TYPES.contains(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "type must be one of: flight, accommodation, car_rental.");
            }

            if (status != null && !VALID_STATUSES.contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "status must be one of: confirmed, changed, cancelled.");
            }

            List<GetReservationsResponse.ReservationDto> dtos = reservationRepository
                    .findByClerkIdWithFilters(clerkId, type, status)
                    .stream()
                    .map(r -> GetReservationsResponse.ReservationDto.from(r, parseDetail(r)))
                    .toList();

            return new GetReservationsResponse(dtos);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch reservations.")
                );
    }

    @Override
    public Mono<CreateReservationResponse> createReservation(String clerkId, CreateReservationRequest request) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please sign up first.");
            }

            if (!VALID_TYPES.contains(request.type())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "type must be one of: flight, accommodation, car_rental.");
            }

            if (!VALID_BOOKED_BY.contains(request.bookedBy())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "bookedBy must be one of: user, ai.");
            }

            Itinerary itinerary = itineraryRepository.findById(request.itineraryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found."));

            var chatRoom = chatRoomRepository.findById(itinerary.getRoomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found."));

            if (!chatRoom.getClerkId().equals(clerkId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to create a reservation for this itinerary.");
            }

            String detailJson = objectMapper.writeValueAsString(request.detail());

            Reservation reservation = Reservation.of(
                    request.itineraryId(),
                    request.type(),
                    "confirmed",
                    request.bookedBy(),
                    request.bookingUrl(),
                    request.externalRefId(),
                    detailJson,
                    request.totalPrice(),
                    request.currency() != null ? request.currency() : "KRW",
                    request.reservedAt()
            );

            Reservation saved = reservationRepository.save(reservation);
            return CreateReservationResponse.from(saved);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create reservation.")
                );
    }

    @Override
    public Mono<PatchReservationResponse> updateReservation(String clerkId, UUID reservationId, PatchReservationRequest request) {
        return Mono.fromCallable(() -> {
            if (request.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided.");
            }

            if (request.status() != null && !VALID_STATUSES.contains(request.status())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "status must be one of: confirmed, changed, cancelled.");
            }

            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found."));

            Itinerary itinerary = itineraryRepository.findById(reservation.getItineraryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found."));

            var chatRoom = chatRoomRepository.findById(itinerary.getRoomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found."));

            if (!chatRoom.getClerkId().equals(clerkId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to update this reservation.");
            }

            String detailJson = request.detail() != null
                    ? objectMapper.writeValueAsString(request.detail())
                    : null;

            reservation.update(
                    request.status(),
                    detailJson,
                    request.totalPrice(),
                    request.currency(),
                    request.reservedAt(),
                    request.cancelledAt()
            );

            Reservation saved = reservationRepository.save(reservation);
            return PatchReservationResponse.from(saved);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update reservation.")
                );
    }

    private Map<String, Object> parseDetail(Reservation reservation) {
        try {
            return objectMapper.readValue(reservation.getDetail(), new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
