package com.mju.capstone_backend.domain.chatmessage.repository;

import com.mju.capstone_backend.domain.chatmessage.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    List<ChatMessage> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID roomId, OffsetDateTime cursor, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE chat_messages SET embedding = CAST(:embedding AS vector) WHERE id = :id",
            nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);
}
