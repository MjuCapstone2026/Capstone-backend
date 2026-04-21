package com.mju.capstone_backend.domain.reservation.service;

import com.mju.capstone_backend.domain.reservation.dto.CreateReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.CreateReservationResponse;
import com.mju.capstone_backend.domain.reservation.dto.GetReservationsResponse;
import reactor.core.publisher.Mono;

public interface ReservationService {

    Mono<GetReservationsResponse> getReservations(String clerkId, String type, String status);

    Mono<CreateReservationResponse> createReservation(String clerkId, CreateReservationRequest request);
}
