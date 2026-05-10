package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id","seeker_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id")     private RoomEntity room;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seeker_id")   private UserEntity seeker;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "landlord_id") private UserEntity landlord;
    @Column(name = "last_message", columnDefinition = "TEXT") private String lastMessage;
    @Column(name = "last_message_at") private LocalDateTime lastMessageAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

