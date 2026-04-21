package com.mju.capstone_backend.domain.reservation.controller;

import com.mju.capstone_backend.domain.reservation.dto.CreateReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.CreateReservationResponse;
import com.mju.capstone_backend.domain.reservation.dto.GetReservationsResponse;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationRequest;
import com.mju.capstone_backend.domain.reservation.dto.PatchReservationResponse;
import com.mju.capstone_backend.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Reservation API", description = "예약 관련 API")
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "내 예약 목록 조회", description = "현재 로그인한 사용자의 예약 목록을 조회합니다. type과 status로 필터링할 수 있습니다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<GetReservationsResponse> getReservations(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return reservationService.getReservations(clerkId, type, status);
    }

    @Operation(summary = "예약 생성", description = "AI Agent가 예약 링크 제공 시 예약 레코드를 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return reservationService.createReservation(clerkId, request);
    }

    @Operation(summary = "예약 수정", description = "예약 상태, detail, 가격 등을 수정합니다.")
    @PatchMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<PatchReservationResponse> updateReservation(
            @PathVariable UUID reservationId,
            @RequestBody PatchReservationRequest request,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return reservationService.updateReservation(clerkId, reservationId, request);
    }

    @Operation(summary = "예약 삭제", description = "예약 레코드를 완전히 삭제합니다.")
    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteReservation(
            @PathVariable UUID reservationId,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return reservationService.deleteReservation(clerkId, reservationId);
    }
}
