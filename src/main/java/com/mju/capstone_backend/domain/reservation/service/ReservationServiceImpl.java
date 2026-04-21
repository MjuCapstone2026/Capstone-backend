package com.mju.capstone_backend.domain.reservation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.reservation.dto.GetReservationsResponse;
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

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final Set<String> VALID_TYPES = Set.of("flight", "accommodation", "car_rental");
    private static final Set<String> VALID_STATUSES = Set.of("confirmed", "changed", "cancelled");

    private final UserRepository userRepository;
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

    private Map<String, Object> parseDetail(Reservation reservation) {
        try {
            return objectMapper.readValue(reservation.getDetail(), new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
