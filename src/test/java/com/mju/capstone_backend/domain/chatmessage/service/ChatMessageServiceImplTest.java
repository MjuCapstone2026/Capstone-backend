package com.mju.capstone_backend.domain.chatmessage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatmessage.entity.ChatMessage;
import com.mju.capstone_backend.domain.chatmessage.repository.ChatMessageRepository;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryLogRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageServiceImpl 단위 테스트")
class ChatMessageServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private ItineraryLogRepository itineraryLogRepository;

    @Mock
    private FastApiChatClient fastApiChatClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    private static final String CLERK_ID = "user_testClerkId";
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID MSG_ID_1 = UUID.randomUUID();
    private static final UUID MSG_ID_2 = UUID.randomUUID();

    @BeforeEach
    void injectSchedulerAndMapper() throws Exception {
        var schedulerField = ChatMessageServiceImpl.class.getDeclaredField("dbScheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(chatMessageService, Schedulers.immediate());

        var mapperField = ChatMessageServiceImpl.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        mapperField.set(chatMessageService, new ObjectMapper());
    }

    // ─── 성공 케이스 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 조회 - cursor 없이 첫 페이지 조회")
    void getMessages_withoutCursor_returnsFirstPage() {
        // given
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID);
        OffsetDateTime now = OffsetDateTime.now();
        List<ChatMessage> messages = List.of(
                mockChatMessage(MSG_ID_1, ROOM_ID, "user", "안녕하세요", now),
                mockChatMessage(MSG_ID_2, ROOM_ID, "assistant", "네 안녕하세요", now.minusSeconds(30))
        );

        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(ROOM_ID), any()))
                .thenReturn(messages);

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, null, 30);

        // then
        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.roomId()).isEqualTo(ROOM_ID);
                    assertThat(res.messages()).hasSize(2);
                    assertThat(res.hasMore()).isFalse();
                    assertThat(res.nextCursor()).isNull();
                })
                .verifyComplete();

        verify(chatMessageRepository).findByRoomIdOrderByCreatedAtDesc(eq(ROOM_ID), any());
        verify(chatMessageRepository, never())
                .findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("메시지 조회 - cursor 기반 이전 메시지 조회")
    void getMessages_withCursor_returnsPreviousMessages() {
        // given
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID);
        OffsetDateTime cursor = OffsetDateTime.now().minusHours(1);
        List<ChatMessage> messages = List.of(
                mockChatMessage(MSG_ID_1, ROOM_ID, "user", "이전 메시지", cursor.minusSeconds(30))
        );

        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                eq(ROOM_ID), eq(cursor), any()))
                .thenReturn(messages);

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, cursor, 30);

        // then
        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.messages()).hasSize(1);
                    assertThat(res.hasMore()).isFalse();
                })
                .verifyComplete();

        verify(chatMessageRepository)
                .findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(eq(ROOM_ID), eq(cursor), any());
        verify(chatMessageRepository, never()).findByRoomIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("메시지 조회 - hasMore=true이면 nextCursor 반환")
    void getMessages_hasMore_true_nextCursorReturned() {
        // given
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID);
        OffsetDateTime t1 = OffsetDateTime.now();
        OffsetDateTime t2 = t1.minusSeconds(10);
        OffsetDateTime t3 = t1.minusSeconds(20);

        // limit=2, fetched=3(limit+1) → hasMore=true, page=[msg1, msg2], nextCursor=t2
        List<ChatMessage> fetched = List.of(
                mockChatMessage(MSG_ID_1, ROOM_ID, "user", "첫 번째", t1),
                mockChatMessage(MSG_ID_2, ROOM_ID, "assistant", "두 번째", t2),
                mockChatMessage(UUID.randomUUID(), ROOM_ID, "user", "세 번째(extra)", t3)
        );

        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(ROOM_ID), any()))
                .thenReturn(fetched);

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, null, 2);

        // then
        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.messages()).hasSize(2);
                    assertThat(res.hasMore()).isTrue();
                    assertThat(res.nextCursor()).isEqualTo(t2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("메시지 조회 - 마지막 페이지이면 nextCursor=null 반환")
    void getMessages_lastPage_nextCursorIsNull() {
        // given
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID);

        // limit=5, fetched=1(< limit) → hasMore=false
        List<ChatMessage> fetched = List.of(
                mockChatMessage(MSG_ID_1, ROOM_ID, "user", "마지막 메시지", OffsetDateTime.now())
        );

        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(ROOM_ID), any()))
                .thenReturn(fetched);

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, null, 5);

        // then
        StepVerifier.create(result)
                .assertNext(res -> {
                    assertThat(res.hasMore()).isFalse();
                    assertThat(res.nextCursor()).isNull();
                })
                .verifyComplete();
    }

    // ─── 예외 케이스 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 조회 - 존재하지 않는 채팅방은 404 반환")
    void getMessages_roomNotFound_returns404() {
        // given
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, null, 30);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("메시지 조회 - 채팅방 소유자가 아니면 403 반환")
    void getMessages_otherUser_returns403() {
        // given
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, "user_otherClerkId");
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, null, 30);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == FORBIDDEN)
                .verify();

        verify(chatMessageRepository, never()).findByRoomIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("메시지 조회 - limit이 1 미만이면 400 반환")
    void getMessages_limitTooLow_returns400() {
        // given
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID);
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, null, 0);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("메시지 조회 - limit이 100 초과이면 400 반환")
    void getMessages_limitTooHigh_returns400() {
        // given
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID);
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        // when
        var result = chatMessageService.getMessages(CLERK_ID, ROOM_ID, null, 101);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

    private ChatRoom mockChatRoom(UUID id, String clerkId) {
        ChatRoom chatRoom = ChatRoom.of(clerkId, "테스트 채팅방");
        try {
            var idField = ChatRoom.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(chatRoom, id);

            var createdAtField = ChatRoom.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(chatRoom, OffsetDateTime.now());

            var updatedAtField = ChatRoom.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(chatRoom, OffsetDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return chatRoom;
    }

    private ChatMessage mockChatMessage(UUID id, UUID roomId, String role, String content, OffsetDateTime createdAt) {
        ChatMessage msg = ChatMessage.of(roomId, role, content);
        try {
            var idField = ChatMessage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(msg, id);

            var createdAtField = ChatMessage.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(msg, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return msg;
    }
}
