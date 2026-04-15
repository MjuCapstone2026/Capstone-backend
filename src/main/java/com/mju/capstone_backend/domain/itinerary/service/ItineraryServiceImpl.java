package com.mju.capstone_backend.domain.itinerary.service;

import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository.Summary;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final UserRepository userRepository;
    private final ItineraryRepository itineraryRepository;
    private final Scheduler dbScheduler;

    @Override
    public Mono<GetItinerariesResponse> getItineraries(String clerkId) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please sign up first.");
            }

            List<GetItinerariesResponse.ItineraryItem> items = itineraryRepository
                    .findSummariesByClerkId(clerkId)
                    .stream()
                    .map(s -> new GetItinerariesResponse.ItineraryItem(
                            s.getId(),
                            s.getName(),
                            s.getStatus(),
                            s.getDestination(),
                            s.getTotalDays(),
                            s.getStartDate()
                    ))
                    .toList();

            return new GetItinerariesResponse(items);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch itineraries.")
                );
    }
}
