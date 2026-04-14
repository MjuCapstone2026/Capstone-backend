package com.mju.capstone_backend.domain.chatroom.controller;

import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomRequest;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomsResponse;
import com.mju.capstone_backend.domain.chatroom.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "ChatRoom API", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "채팅방 생성", description = "사용자의 새로운 여행 계획용 채팅방을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateChatRoomResponse> createChatRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return chatRoomService.createChatRoom(clerkId, request);
    }

    @Operation(summary = "내 채팅방 목록 조회", description = "현재 로그인한 사용자의 채팅방 목록을 최근 수정 순으로 반환합니다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<GetChatRoomsResponse> getChatRooms(JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return chatRoomService.getChatRooms(clerkId);
    }
}
