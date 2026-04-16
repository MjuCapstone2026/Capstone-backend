package com.mju.capstone_backend.domain.itinerary.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.dto.GetItineraryResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItineraryRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItineraryResponse;
import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import com.mju.capstone_backend.domain.itinerary.entity.ItineraryLog;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryLogRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository.Summary;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;
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
    private final ItineraryLogRepository itineraryLogRepository;
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

    @Override
    public Mono<PatchItineraryResponse> patchItinerary(String clerkId, UUID itineraryId, PatchItineraryRequest request) {
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
                        "You do not have permission to update this itinerary.");
            }

            LocalDate effectiveStart = request.startDate() != null ? request.startDate() : itinerary.getStartDate();
            LocalDate effectiveEnd = request.endDate() != null ? request.endDate() : itinerary.getEndDate();

            if (effectiveStart.isAfter(effectiveEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "startDate must not be later than endDate.");
            }
            if (request.adultCount() != null && request.adultCount() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "adultCount must be at least 1.");
            }
            boolean hasChildCount = request.childCount() != null;
            boolean hasChildAges = request.childAges() != null;
            if (hasChildCount != hasChildAges) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "childCount and childAges must be provided together.");
            }
            if (hasChildCount && request.childAges().size() != request.childCount()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "childAges length must match childCount.");
            }

            itineraryLogRepository.save(ItineraryLog.of(itinerary));

            boolean dateChanged = !effectiveStart.equals(itinerary.getStartDate())
                    || !effectiveEnd.equals(itinerary.getEndDate());

            String updatedDayPlans = dateChanged
                    ? adjustDayPlans(itinerary.getDayPlans(), effectiveStart, effectiveEnd)
                    : null;

            itinerary.updateBasicInfo(
                    request.startDate(), request.endDate(),
                    request.budget(),
                    request.adultCount(),
                    request.childCount(), request.childAges(),
                    updatedDayPlans);

            itineraryRepository.save(itinerary);

            return new PatchItineraryResponse(
                    itinerary.getId(),
                    itinerary.getDestination(),
                    itinerary.getStartDate(),
                    itinerary.getEndDate(),
                    itinerary.getTotalDays(),
                    itinerary.getBudget(),
                    itinerary.getAdultCount(),
                    itinerary.getChildCount(),
                    itinerary.getChildAges(),
                    itinerary.getUpdatedAt());
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update itinerary.")
                );
    }

    private String adjustDayPlans(String currentDayPlansJson, LocalDate newStart, LocalDate newEnd) {
        try {
            Map<String, Object> dayPlans = objectMapper.readValue(currentDayPlansJson, new TypeReference<>() {});

            dayPlans.keySet().removeIf(key -> {
                LocalDate date = LocalDate.parse(key);
                return date.isBefore(newStart) || date.isAfter(newEnd);
            });

            LocalDate cursor = newStart;
            while (!cursor.isAfter(newEnd)) {
                dayPlans.putIfAbsent(cursor.toString(), List.of());
                cursor = cursor.plusDays(1);
            }

            Map<String, Object> sorted = new LinkedHashMap<>();
            dayPlans.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sorted.put(e.getKey(), e.getValue()));

            return objectMapper.writeValueAsString(sorted);
        } catch (Exception e) {
            return currentDayPlansJson;
        }
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
