package com.mju.capstone_backend.domain.itinerary.controller;

import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Itinerary API", description = "여행 일정 관련 API")
@RestController
@RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @Operation(summary = "내 여행 일정 목록 조회", description = "현재 로그인한 사용자의 여행 일정 목록을 반환합니다. draft 상태 우선, 동일 status 내 startDate 오름차순으로 정렬됩니다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<GetItinerariesResponse> getItineraries(JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return itineraryService.getItineraries(clerkId);
    }
}
