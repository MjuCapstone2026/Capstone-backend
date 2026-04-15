package com.mju.capstone_backend.domain.chatroom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomRequest;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.DeleteChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomsResponse;
import com.mju.capstone_backend.domain.chatroom.dto.UpdateChatRoomNameResponse;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.entity.Itinerary;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.chatroom.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.reservation.repository.ReservationRepository;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ItineraryRepository itineraryRepository;
    private final ReservationRepository reservationRepository;
    private final Scheduler dbScheduler;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<CreateChatRoomResponse> createChatRoom(String clerkId, CreateChatRoomRequest request) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please sign up first.");
            }

            if (request.startDate().isAfter(request.endDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "startDate must not be later than endDate.");
            }

            if (request.childAges().size() != request.childCount()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "childAges length must match childCount.");
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
                            request.childCount(),
                            request.childAges()
                    )
            );

            return new CreateChatRoomResponse(
                    chatRoom.getId(),
                    itinerary.getId(),
                    chatRoom.getClerkId(),
                    chatRoom.getCreatedAt(),
                    chatRoom.getUpdatedAt()
            );
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create chat room.")
                );
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
                            room.getClerkId(),
                            room.getAiSummary(),
                            parsePreferences(room.getPreferences()),
                            room.getCreatedAt(),
                            room.getUpdatedAt()
                    ))
                    .toList();

            return new GetChatRoomsResponse(items);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch chat rooms.")
                );
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
                    chatRoom.getClerkId(),
                    chatRoom.getAiSummary(),
                    parsePreferences(chatRoom.getPreferences()),
                    itineraryId,
                    chatRoom.getCreatedAt(),
                    chatRoom.getUpdatedAt()
            );
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch chat room.")
                );
    }

    @Override
    public Mono<DeleteChatRoomResponse> deleteChatRoom(String clerkId, UUID roomId) {
        return Mono.fromCallable(() -> {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found."));

            if (!chatRoom.getClerkId().equals(clerkId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to delete this chat room.");
            }

            itineraryRepository.findByRoomId(roomId).ifPresent(itinerary -> {
                if (reservationRepository.existsByItineraryId(itinerary.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cannot delete chat room with existing reservations.");
                }
            });

            chatRoomRepository.deleteById(roomId);

            return new DeleteChatRoomResponse(roomId, true);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete chat room.")
                );
    }

    @Override
    public Mono<UpdateChatRoomNameResponse> updateChatRoomName(String clerkId, UUID roomId, String name) {
        return Mono.fromCallable(() -> {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found."));

            if (!chatRoom.getClerkId().equals(clerkId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to update this chat room.");
            }

            chatRoom.updateName(name);
            ChatRoom saved = chatRoomRepository.save(chatRoom);

            return new UpdateChatRoomNameResponse(saved.getId(), saved.getName(), saved.getUpdatedAt());
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update chat room name.")
                );
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
