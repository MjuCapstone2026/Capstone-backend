package com.mju.capstone_backend.domain.itinerary.service;

import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import reactor.core.publisher.Mono;

public interface ItineraryService {

    Mono<GetItinerariesResponse> getItineraries(String clerkId);
}
