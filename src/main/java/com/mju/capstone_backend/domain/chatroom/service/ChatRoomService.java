package com.mju.capstone_backend.domain.chatroom.service;

import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomRequest;
import com.mju.capstone_backend.domain.chatroom.dto.CreateChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.DeleteChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomResponse;
import com.mju.capstone_backend.domain.chatroom.dto.GetChatRoomsResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ChatRoomService {

    Mono<CreateChatRoomResponse> createChatRoom(String clerkId, CreateChatRoomRequest request);

    Mono<GetChatRoomsResponse> getChatRooms(String clerkId);

    Mono<GetChatRoomResponse> getChatRoom(String clerkId, UUID roomId);

    Mono<DeleteChatRoomResponse> deleteChatRoom(String clerkId, UUID roomId);
}
