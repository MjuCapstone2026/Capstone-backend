package com.mju.capstone_backend.domain.chatroom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "clerk_id", nullable = false)
    private String clerkId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "ai_summary")
    private String aiSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb")
    private String preferences;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public static ChatRoom of(String clerkId, String name) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.clerkId = clerkId;
        chatRoom.name = name;
        return chatRoom;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
