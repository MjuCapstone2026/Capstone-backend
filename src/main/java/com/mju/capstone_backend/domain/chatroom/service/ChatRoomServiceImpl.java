package com.mju.capstone_backend.domain.chatroom.service;

import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomRequest;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomResponse;
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

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ItineraryRepository itineraryRepository;
    private final Scheduler dbScheduler;

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
}
