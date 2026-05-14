package com.mju.capstone_backend.domain.chatmessage.controller;

import com.mju.capstone_backend.domain.chatmessage.dto.GetChatRoomMessagesResponse;
import com.mju.capstone_backend.domain.chatmessage.service.ChatMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
@DisplayName("ChatMessageController 슬라이스 테스트")
class ChatMessageControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ChatMessageService chatMessageService;

    private static final String CLERK_ID = "user_testClerkId";
    private static final UUID ROOM_ID = UUID.randomUUID();

    // ─── 성공 케이스 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 히스토리 조회 - 유효한 JWT로 200 반환")
    void getMessages_withValidJwt_returns200() {
        // given
        UUID msgId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        GetChatRoomMessagesResponse response = new GetChatRoomMessagesResponse(
                ROOM_ID,
                List.of(new GetChatRoomMessagesResponse.MessageItem(msgId, "user", "안녕하세요", null, now)),
                null,
                false
        );
        when(chatMessageService.getMessages(eq(CLERK_ID), eq(ROOM_ID), isNull(), eq(30)))
                .thenReturn(Mono.just(response));

        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.roomId").isEqualTo(ROOM_ID.toString())
                .jsonPath("$.messages").isArray()
                .jsonPath("$.hasMore").isEqualTo(false);

        verify(chatMessageService).getMessages(eq(CLERK_ID), eq(ROOM_ID), isNull(), eq(30));
    }

    @Test
    @DisplayName("메시지 히스토리 조회 - cursor와 limit 파라미터로 서비스 올바르게 호출")
    void getMessages_withCursorAndLimit_callsServiceCorrectly() {
        // given
        GetChatRoomMessagesResponse response = new GetChatRoomMessagesResponse(
                ROOM_ID, List.of(), null, false
        );
        when(chatMessageService.getMessages(eq(CLERK_ID), eq(ROOM_ID), any(OffsetDateTime.class), eq(5)))
                .thenReturn(Mono.just(response));

        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/chat-messages/{roomId}?cursor=2026-04-03T12:09:30Z&limit=5", ROOM_ID)
                .exchange()
                .expectStatus().isOk();

        verify(chatMessageService).getMessages(eq(CLERK_ID), eq(ROOM_ID), any(OffsetDateTime.class), eq(5));
    }

    // ─── 인증 실패 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 히스토리 조회 - JWT 없이 요청 시 401 반환")
    void getMessages_withoutJwt_returns401() {
        // when & then
        webTestClient
                .get()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ─── 에러 전파 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 히스토리 조회 - 채팅방 없으면 404 반환")
    void getMessages_serviceReturns404_returns404() {
        // given
        when(chatMessageService.getMessages(eq(CLERK_ID), eq(ROOM_ID), any(), anyInt()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found.")));

        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("메시지 히스토리 조회 - 채팅방 소유자가 아니면 403 반환")
    void getMessages_serviceReturns403_returns403() {
        // given
        when(chatMessageService.getMessages(eq(CLERK_ID), eq(ROOM_ID), any(), anyInt()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this chat room.")));

        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .get()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .exchange()
                .expectStatus().isForbidden();
    }

    // ─── POST /messages 성공 케이스 ──────────────────────────────────────────────

    @Test
    @DisplayName("메시지 전송 - 유효한 JWT와 바디로 서비스 호출 확인")
    void sendMessage_withValidJwt_callsServiceCorrectly() {
        // given
        when(chatMessageService.sendMessage(eq(CLERK_ID), eq(ROOM_ID), eq("경복궁 대신 창덕궁으로 바꿔줘")))
                .thenReturn(Flux.empty());

        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"content": "경복궁 대신 창덕궁으로 바꿔줘"}
                        """)
                .exchange()
                .expectStatus().isOk();

        verify(chatMessageService).sendMessage(eq(CLERK_ID), eq(ROOM_ID), eq("경복궁 대신 창덕궁으로 바꿔줘"));
    }

    // ─── POST /messages 인증 실패 ─────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 전송 - JWT 없이 요청 시 401 반환")
    void sendMessage_withoutJwt_returns401() {
        // when & then
        webTestClient
                .post()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"content": "테스트"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ─── POST /messages 검증 실패 ─────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 전송 - content가 빈 문자열이면 400 반환")
    void sendMessage_withBlankContent_returns400() {
        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"content": ""}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ─── POST /messages 에러 전파 ─────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 전송 - 채팅방 없으면 404 반환")
    void sendMessage_serviceReturns404_returns404() {
        // given
        when(chatMessageService.sendMessage(eq(CLERK_ID), eq(ROOM_ID), anyString()))
                .thenReturn(Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found.")));

        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"content": "테스트"}
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("메시지 전송 - 채팅방 소유자가 아니면 403 반환")
    void sendMessage_serviceReturns403_returns403() {
        // given
        when(chatMessageService.sendMessage(eq(CLERK_ID), eq(ROOM_ID), anyString()))
                .thenReturn(Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this chat room.")));

        // when & then
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject(CLERK_ID)))
                .post()
                .uri("/api/v1/chat-messages/{roomId}", ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"content": "테스트"}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }
}
