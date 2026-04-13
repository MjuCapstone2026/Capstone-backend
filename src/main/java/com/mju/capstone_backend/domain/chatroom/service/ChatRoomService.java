package com.mju.capstone_backend.domain.chatroom.service;

import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomRequest;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomResponse;
import reactor.core.publisher.Mono;

public interface ChatRoomService {

    Mono<CreateChatRoomResponse> createChatRoom(String clerkId, CreateChatRoomRequest request);
}
