package com.mju.capstone_backend.domain.itinerary.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.dto.GetItineraryLogsResponse;
import com.mju.capstone_backend.domain.itinerary.dto.GetItineraryResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchDayPlansRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchDayPlansResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItemStatusRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItemStatusResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItineraryRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItineraryResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchStatusRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchStatusResponse;
import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import com.mju.capstone_backend.domain.itinerary.entity.ItineraryLog;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryLogRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository.Summary;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private static final Pattern TIME_RANGE_PATTERN =
            Pattern.compile("^\\d{2}:\\d{2} ~ \\d{2}:\\d{2}$");

    private final UserRepository userRepository;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryLogRepository itineraryLogRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ObjectMapper objectMapper;
    private final Scheduler dbScheduler;
    private final TransactionTemplate transactionTemplate;

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
    public Mono<GetItineraryLogsResponse> getItineraryLogs(String clerkId, UUID itineraryId) {
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

            List<GetItineraryLogsResponse.LogItem> logItems = itineraryLogRepository
                    .findByItineraryIdOrderByCreatedAtDesc(itineraryId)
                    .stream()
                    .map(log -> new GetItineraryLogsResponse.LogItem(
                            log.getId(),
                            log.getDestination(),
                            log.getBudget(),
                            log.getAdultCount(),
                            log.getChildCount(),
                            log.getChildAges(),
                            log.getTotalDays(),
                            log.getStartDate(),
                            log.getEndDate(),
                            parseDayPlansRaw(log.getDayPlans()),
                            log.getCreatedAt()
                    ))
                    .toList();

            return new GetItineraryLogsResponse(itineraryId, logItems);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch itinerary logs.")
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

            boolean startDateUnchanged = request.startDate() == null || request.startDate().equals(itinerary.getStartDate());
            boolean endDateUnchanged = request.endDate() == null || request.endDate().equals(itinerary.getEndDate());
            boolean budgetUnchanged = request.budget() == null ||
                    (itinerary.getBudget() != null && request.budget().compareTo(itinerary.getBudget()) == 0);
            boolean adultCountUnchanged = request.adultCount() == null || request.adultCount() == itinerary.getAdultCount();
            boolean childInfoUnchanged = !hasChildCount ||
                    (request.childCount() == itinerary.getChildCount() &&
                     Objects.equals(request.childAges(), itinerary.getChildAges()));

            if (startDateUnchanged && endDateUnchanged && budgetUnchanged && adultCountUnchanged && childInfoUnchanged) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No changes detected. The submitted values are identical to the current data.");
            }

            boolean dateChanged = !effectiveStart.equals(itinerary.getStartDate())
                    || !effectiveEnd.equals(itinerary.getEndDate());

            String updatedDayPlans = dateChanged
                    ? adjustDayPlans(itinerary.getDayPlans(), effectiveStart, effectiveEnd)
                    : null;

            transactionTemplate.executeWithoutResult(status -> {
                itineraryLogRepository.save(ItineraryLog.of(itinerary));
                itinerary.updateBasicInfo(
                        request.startDate(), request.endDate(),
                        request.budget(),
                        request.adultCount(),
                        request.childCount(), request.childAges(),
                        updatedDayPlans);
                itineraryRepository.save(itinerary);
            });

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

    @Override
    public Mono<PatchDayPlansResponse> patchDayPlans(String clerkId, UUID itineraryId, PatchDayPlansRequest request) {
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

            validateDayPlansRequest(request.dayPlans(), itinerary.getStartDate(), itinerary.getEndDate());

            Map<String, List<Map<String, Object>>> existingDayPlans =
                    objectMapper.readValue(itinerary.getDayPlans(), new TypeReference<>() {});

            Map<String, List<Map<String, Object>>> merged = new LinkedHashMap<>(existingDayPlans);

            for (Map.Entry<String, List<Map<String, Object>>> entry : request.dayPlans().entrySet()) {
                List<Map<String, Object>> existingItems = existingDayPlans.getOrDefault(entry.getKey(), List.of());
                Map<String, String> existingStatusByTime = new LinkedHashMap<>();
                for (Map<String, Object> item : existingItems) {
                    existingStatusByTime.put((String) item.get("time"),
                            (String) item.getOrDefault("status", "todo"));
                }

                List<Map<String, Object>> sorted = entry.getValue().stream()
                        .map(item -> {
                            Map<String, Object> normalized = new LinkedHashMap<>();
                            normalized.put("plan_name", item.get("plan_name"));
                            normalized.put("time", item.get("time"));
                            normalized.put("place", item.get("place"));
                            normalized.put("note", item.getOrDefault("note", ""));
                            normalized.put("cost", item.get("cost"));
                            normalized.put("status", existingStatusByTime.getOrDefault(
                                    (String) item.get("time"), "todo"));
                            return normalized;
                        })
                        .sorted(Comparator.comparing(item ->
                                LocalTime.parse(((String) item.get("time")).split(" ~ ")[0])))
                        .toList();
                merged.put(entry.getKey(), sorted);
            }

            Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
            merged.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> result.put(e.getKey(), e.getValue()));

            boolean hasChanges = request.dayPlans().keySet().stream().anyMatch(date -> {
                List<Map<String, Object>> newItems = result.get(date);
                List<Map<String, Object>> currentItems = existingDayPlans.getOrDefault(date, List.of()).stream()
                        .map(item -> {
                            Map<String, Object> n = new LinkedHashMap<>(item);
                            n.putIfAbsent("note", "");
                            n.putIfAbsent("cost", null);
                            return n;
                        })
                        .sorted(Comparator.comparing(item ->
                                LocalTime.parse(((String) item.get("time")).split(" ~ ")[0])))
                        .toList();
                return !objectMapper.valueToTree(newItems).equals(objectMapper.valueToTree(currentItems));
            });

            if (!hasChanges) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No changes detected. The submitted day plans are identical to the current data.");
            }

            String resultJson = objectMapper.writeValueAsString(result);

            transactionTemplate.executeWithoutResult(status -> {
                itineraryLogRepository.save(ItineraryLog.of(itinerary));
                itinerary.updateDayPlans(resultJson);
                itineraryRepository.save(itinerary);
            });

            return new PatchDayPlansResponse(itinerary.getId(), result, itinerary.getUpdatedAt());
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update day plans.")
                );
    }

    private void validateDayPlansRequest(Map<String, List<Map<String, Object>>> dayPlans,
                                          LocalDate rangeStart, LocalDate rangeEnd) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : dayPlans.entrySet()) {
            String dateStr = entry.getKey();

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid date: '" + dateStr + "'. Use YYYY-MM-DD format (e.g. '2026-05-01').");
            }

            if (date.isBefore(rangeStart) || date.isAfter(rangeEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Date '" + dateStr + "' is out of the itinerary date range.");
            }

            for (Map<String, Object> item : entry.getValue()) {
                if (!item.containsKey("plan_name") || !item.containsKey("time")
                        || !item.containsKey("place")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Each item must include plan_name, time, and place.");
                }

                String time = String.valueOf(item.get("time"));
                if (!TIME_RANGE_PATTERN.matcher(time).matches()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid time format. Use 'HH:MM ~ HH:MM' (e.g. '09:00 ~ 12:00').");
                }
                try {
                    String[] parts = time.split(" ~ ");
                    LocalTime.parse(parts[0]);
                    LocalTime.parse(parts[1]);
                } catch (DateTimeParseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid time format. Use 'HH:MM ~ HH:MM' (e.g. '09:00 ~ 12:00').");
                }
            }

            validateNoTimeOverlap(entry.getValue());
        }
    }

    private void validateNoTimeOverlap(List<Map<String, Object>> items) {
        if (items.size() < 2) return;

        List<LocalTime[]> ranges = items.stream()
                .map(item -> {
                    String[] parts = String.valueOf(item.get("time")).split(" ~ ");
                    return new LocalTime[]{LocalTime.parse(parts[0]), LocalTime.parse(parts[1])};
                })
                .sorted(Comparator.comparing(r -> r[0]))
                .toList();

        for (int i = 0; i < ranges.size() - 1; i++) {
            if (ranges.get(i + 1)[0].isBefore(ranges.get(i)[1])) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Time ranges must not overlap within the same date.");
            }
        }
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

    @Override
    public Mono<PatchStatusResponse> patchStatus(String clerkId, UUID itineraryId, PatchStatusRequest request) {
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

            if (!"draft".equals(request.status()) && !"completed".equals(request.status())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "status must be one of: draft, completed.");
            }

            if (request.status().equals(itinerary.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No changes detected. The submitted status is identical to the current status.");
            }

            itinerary.updateStatus(request.status());
            itineraryRepository.save(itinerary);

            return new PatchStatusResponse(itinerary.getId(), itinerary.getStatus(), itinerary.getUpdatedAt());
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update itinerary status.")
                );
    }

    @Override
    public Mono<PatchItemStatusResponse> patchItemStatus(String clerkId, UUID itineraryId, PatchItemStatusRequest request) {
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

            if (!"todo".equals(request.status()) && !"done".equals(request.status())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid status value. Must be 'todo' or 'done'.");
            }

            Map<String, List<Map<String, Object>>> dayPlans =
                    objectMapper.readValue(itinerary.getDayPlans(), new TypeReference<>() {});

            List<Map<String, Object>> dateItems = dayPlans.get(request.date());
            if (dateItems == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found.");
            }

            List<Map<String, Object>> sorted = new ArrayList<>(dateItems);
            sorted.sort(Comparator.comparing(item ->
                    LocalTime.parse(((String) item.get("time")).split(" ~ ")[0])));

            if (request.index() < 0 || request.index() >= sorted.size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found.");
            }

            Map<String, Object> targetItem = sorted.get(request.index());

            if (request.status().equals(targetItem.get("status"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No changes detected. The submitted status is identical to the current status.");
            }

            Map<String, Object> updatedItem = new LinkedHashMap<>(targetItem);
            updatedItem.put("status", request.status());
            sorted.set(request.index(), updatedItem);

            dayPlans.put(request.date(), sorted);
            itinerary.updateDayPlans(objectMapper.writeValueAsString(dayPlans));
            itineraryRepository.save(itinerary);

            return new PatchItemStatusResponse(
                    itinerary.getId(),
                    request.date(),
                    request.index(),
                    request.status(),
                    itinerary.getUpdatedAt()
            );
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update item status.")
                );
    }

    private Map<String, Object> parseDayPlansRaw(String dayPlansJson) {
        try {
            return objectMapper.readValue(dayPlansJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
