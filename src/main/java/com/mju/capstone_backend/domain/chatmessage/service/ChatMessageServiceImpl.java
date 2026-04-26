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
            // TODO: FastAPI SSE 연동 구현 — fastApiChatClient.stream() 호출
            //
            // 1. MemoryPayload 구성 (chatRoom.getAiSummary(), chatRoom.getPreferences())
            //    chatRoom.getPreferences()는 JSON String → Map<String,Object> 역직렬화 필요
            //
            // 2. fastApiChatClient.stream(roomId, content, memoryPayload)
            //    .concatMap(event -> switch(event) { ... })
            //
            // [Chunk 이벤트] ChatStreamEvent.Chunk
            //   → SSE event="chunk", data=MessageChunkResponse(content) 중계
            //
            // [Done 이벤트] ChatStreamEvent.Done
            //   Mono.fromCallable(() -> { ... }).subscribeOn(dbScheduler) 블록 내:
            //   (a) user / assistant ChatMessage 저장 (chatMessageRepository.save)
            //   (b) embedding 저장: updateEmbedding(msgId, embedding.toString())
            //   (c) memory 갱신 (done.memory != null 이면 type 무관하게 처리)
            //       TODO: ChatRoom.updateMemory(aiSummary, preferencesJson) — 팀원 승인 후 구현
            //   (d) type 분기 (sealed interface pattern switch):
            //       - Chat        → 추가 처리 없음
            //       - Itinerary   → itinerary_logs 스냅샷 → dayPlans full replacement
            //                       (동일 time → 기존 status 유지, 신규 → "todo", time 오름차순)
            //       - Change      → itinerary_logs 스냅샷 → itineraries 기본 정보 갱신
            //                       (날짜 변경 시 day_plans 키 조정, destination 수정 불가)
            //       - Reservation → itineraryId 조회(roomId 경유) → reservations 저장
            //                       bookedBy="ai", status="confirmed"
            //       - Cancel      → reservations.status="cancelled", cancelled_at 저장
            //       - null type   → log.warn + Chat fallback
            //       - unknown     → FastApiDonePayload.Deserializer → IllegalArgumentException
            //                       → 502 Bad Gateway
            //   (e) SSE event="done", data=MessageDoneResponse 반환
            //
            // .onErrorMap(e -> !(e instanceof ResponseStatusException), e -> RSE(500))
            return Flux.error(new UnsupportedOperationException("AI integration pending"));
        });
    }
}
