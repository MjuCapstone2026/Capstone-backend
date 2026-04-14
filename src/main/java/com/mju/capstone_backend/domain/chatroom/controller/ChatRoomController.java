package com.mju.capstone_backend.domain.chatroom.controller;

import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomRequest;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.DeleteChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomResponse;
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

import java.util.UUID;

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

    @Operation(summary = "채팅방 상세 조회", description = "현재 로그인한 사용자가 소유한 특정 채팅방의 메타 정보와 연결된 itineraryId를 반환합니다.")
    @GetMapping("/{roomId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<GetChatRoomResponse> getChatRoom(
            @PathVariable UUID roomId,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return chatRoomService.getChatRoom(clerkId, roomId);
    }

    @Operation(summary = "채팅방 삭제", description = "현재 로그인한 사용자가 소유한 채팅방을 삭제합니다. 연결된 메시지와 일정도 함께 삭제됩니다.")
    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<DeleteChatRoomResponse> deleteChatRoom(
            @PathVariable UUID roomId,
            JwtAuthenticationToken authentication) {
        String clerkId = authentication.getToken().getSubject();
        return chatRoomService.deleteChatRoom(clerkId, roomId);
    }
}
