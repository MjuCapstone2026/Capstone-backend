package com.mju.capstone_backend.domain.itinerary.service;

import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.dto.GetItineraryResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchDayPlansRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchDayPlansResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItineraryRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItineraryResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ItineraryService {

    Mono<GetItinerariesResponse> getItineraries(String clerkId);

    Mono<GetItineraryResponse> getItinerary(String clerkId, UUID itineraryId);

    Mono<PatchItineraryResponse> patchItinerary(String clerkId, UUID itineraryId, PatchItineraryRequest request);

    Mono<PatchDayPlansResponse> patchDayPlans(String clerkId, UUID itineraryId, PatchDayPlansRequest request);
}
