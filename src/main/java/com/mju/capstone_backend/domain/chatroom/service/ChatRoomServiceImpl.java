package com.mju.capstone_backend.domain.chatroom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomRequest;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomsResponse;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.entity.Itinerary;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.chatroom.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ItineraryRepository itineraryRepository;
    private final Scheduler dbScheduler;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<CreateChatRoomResponse> createChatRoom(String clerkId, CreateChatRoomRequest request) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please sign up first.");
            }

            int childCount = request.resolvedChildCount();
            List<Integer> childAges = request.childAges();

            if (childCount > 0 && (childAges == null || childAges.size() != childCount)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "childAges must be provided when childCount > 0.");
            }

            long totalDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
            String name = (totalDays - 1) + "박 " + totalDays + "일 " + request.destination() + " 여행";
            ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.of(clerkId, name));

            Itinerary itinerary = itineraryRepository.save(
                    Itinerary.of(
                            chatRoom.getId(),
                            request.destination(),
                            request.startDate(),
                            request.endDate(),
                            request.budget(),
                            request.adultCount(),
                            childCount,
                            childAges != null ? childAges : Collections.emptyList()
                    )
            );

            return new CreateChatRoomResponse(
                    chatRoom.getId(),
                    itinerary.getId(),
                    chatRoom.getCreatedAt(),
                    chatRoom.getUpdatedAt()
            );
        }).subscribeOn(dbScheduler);
    }

    @Override
    public Mono<GetChatRoomsResponse> getChatRooms(String clerkId) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please sign up first.");
            }

            List<GetChatRoomsResponse.ChatRoomItem> items = chatRoomRepository
                    .findByClerkIdOrderByUpdatedAtDesc(clerkId)
                    .stream()
                    .map(room -> new GetChatRoomsResponse.ChatRoomItem(
                            room.getId(),
                            room.getAiSummary(),
                            parsePreferences(room.getPreferences()),
                            room.getCreatedAt(),
                            room.getUpdatedAt()
                    ))
                    .toList();

            return new GetChatRoomsResponse(items);
        }).subscribeOn(dbScheduler);
    }

    @Override
    public Mono<GetChatRoomResponse> getChatRoom(String clerkId, UUID roomId) {
        return Mono.fromCallable(() -> {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found."));

            if (!chatRoom.getClerkId().equals(clerkId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this chat room.");
            }

            UUID itineraryId = itineraryRepository.findByRoomId(roomId)
                    .map(Itinerary::getId)
                    .orElse(null);

            return new GetChatRoomResponse(
                    chatRoom.getId(),
                    chatRoom.getAiSummary(),
                    parsePreferences(chatRoom.getPreferences()),
                    itineraryId,
                    chatRoom.getCreatedAt(),
                    chatRoom.getUpdatedAt()
            );
        }).subscribeOn(dbScheduler);
    }

    private Map<String, Object> parsePreferences(String preferences) {
        if (preferences == null) return null;
        try {
            return objectMapper.readValue(preferences, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
