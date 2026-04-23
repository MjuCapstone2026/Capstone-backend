package com.mju.capstone_backend.domain.reservation.service;

import com.mju.capstone_backend.domain.reservation.dto.CreateReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.CreateReservationResponse;
import com.mju.capstone_backend.domain.reservation.dto.DeleteReservationResponse;
import com.mju.capstone_backend.domain.reservation.dto.GetReservationsResponse;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReservationService {

    Mono<GetReservationsResponse> getReservations(String clerkId, String type, String status);

    Mono<CreateReservationResponse> createReservation(String clerkId, CreateReservationRequest request);

    Mono<PatchReservationResponse> updateReservation(String clerkId, UUID reservationId, PatchReservationRequest request);

    Mono<DeleteReservationResponse> deleteReservation(String clerkId, UUID reservationId);
}
