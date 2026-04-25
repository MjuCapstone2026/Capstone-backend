package com.mju.capstone_backend.domain.chatmessage.service;

import com.mju.capstone_backend.domain.chatmessage.dto.GetChatRoomMessagesResponse;
import com.mju.capstone_backend.domain.chatmessage.entity.ChatMessage;
import com.mju.capstone_backend.domain.chatmessage.repository.ChatMessageRepository;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final FastApiChatClient fastApiChatClient;
    private final Scheduler dbScheduler;

    @Override
    public Mono<GetChatRoomMessagesResponse> getMessages(String clerkId, UUID roomId, OffsetDateTime cursor, int limit) {
        return Mono.fromCallable(() -> {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found."));

            if (!chatRoom.getClerkId().equals(clerkId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this chat room.");
            }

            if (limit < 1 || limit > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "limit must be between 1 and 100.");
            }

            PageRequest pageRequest = PageRequest.of(0, limit + 1);
            List<ChatMessage> fetched = cursor == null
                    ? chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageRequest)
                    : chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, cursor, pageRequest);

            boolean hasMore = fetched.size() > limit;
            List<ChatMessage> page = hasMore ? fetched.subList(0, limit) : fetched;

            List<GetChatRoomMessagesResponse.MessageItem> items = page.stream()
                    .map(msg -> new GetChatRoomMessagesResponse.MessageItem(
                            msg.getId(),
                            msg.getRole(),
                            msg.getContent(),
                            msg.getCreatedAt()
                    ))
                    .toList();

            OffsetDateTime nextCursor = hasMore ? page.get(page.size() - 1).getCreatedAt() : null;

            return new GetChatRoomMessagesResponse(roomId, items, nextCursor, hasMore);
        }).subscribeOn(dbScheduler)
                .onErrorMap(
                        e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch messages.")
                );
    }

    @Override
    public Flux<ServerSentEvent<Object>> sendMessage(String clerkId, UUID roomId, String content) {
        return Mono.fromCallable(() -> {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found."));
            if (!chatRoom.getClerkId().equals(clerkId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this chat room.");
            return chatRoom;
        }).subscribeOn(dbScheduler)
        .flatMapMany(chatRoom -> {
            // TODO: fastApiChatClient.stream(roomId, content) 연동 후 아래 구현 예정
            //
            // [Chunk 이벤트]
            //   → SSE "chunk" { content } 중계
            //
            // [Done 이벤트]
            //   1. user / assistant ChatMessage 저장
            //   2. embedding 저장 (updateEmbedding)
            //   3. memory != null → ChatRoom ai_summary / preferences 업데이트
            //      (TODO: ChatRoom 메서드 추가 후 구현)
            //   4. type 분기:
            //      - "chat"         → 추가 처리 없음
            //      - "itinerary"    → dayPlans full replacement + log snapshot
            //      - "change"       → basicInfo 업데이트 + log snapshot
            //      - "reservation"  → Reservation 저장 (팀원 승인 후)
            //      - "cancel"       → Reservation 취소 (팀원 승인 후)
            //      - null           → log.warn + chat fallback
            //      - unknown        → log.error + 502
            //   5. SSE "done" { userMessage, assistantMessage, itinerary? } 반환
            return Flux.error(new UnsupportedOperationException("AI integration pending"));
        });
    }
}
