package com.mju.capstone_backend.domain.itinerary.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.dto.GetItineraryResponse;
import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository.Summary;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final UserRepository userRepository;
    private final ItineraryRepository itineraryRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ObjectMapper objectMapper;
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

    @Override
    public Mono<GetItineraryResponse> getItinerary(String clerkId, UUID itineraryId) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please sign up first.");
            }

            Itinerary itinerary = itineraryRepository.findById(itineraryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found."));

            ChatRoom chatRoom = chatRoomRepository.findById(itinerary.getRoomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found."));

            if (!chatRoom.getClerkId().equals(clerkId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this itinerary.");
            }

            return new GetItineraryResponse(
                    itinerary.getId(),
                    chatRoom.getName(),
                    itinerary.getStatus(),
                    itinerary.getDestination(),
                    itinerary.getBudget(),
                    itinerary.getAdultCount(),
                    itinerary.getChildCount(),
                    itinerary.getChildAges(),
                    itinerary.getTotalDays(),
                    itinerary.getStartDate(),
                    itinerary.getEndDate(),
                    parseDayPlansWithIndex(itinerary.getDayPlans()),
                    itinerary.getCreatedAt(),
                    itinerary.getUpdatedAt()
            );
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch itinerary.")
                );
    }

    private Map<String, List<Map<String, Object>>> parseDayPlansWithIndex(String dayPlansJson) {
        try {
            Map<String, List<Map<String, Object>>> raw =
                    objectMapper.readValue(dayPlansJson, new TypeReference<>() {});

            Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : raw.entrySet()) {
                List<Map<String, Object>> items = new ArrayList<>(entry.getValue());
                items.sort(Comparator.comparing(item -> {
                    String time = (String) item.get("time");
                    return LocalTime.parse(time.split(" ~ ")[0]);
                }));

                List<Map<String, Object>> indexed = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    Map<String, Object> withIndex = new LinkedHashMap<>();
                    withIndex.put("index", i);
                    withIndex.putAll(items.get(i));
                    indexed.add(withIndex);
                }
                result.put(entry.getKey(), indexed);
            }
            return result;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
