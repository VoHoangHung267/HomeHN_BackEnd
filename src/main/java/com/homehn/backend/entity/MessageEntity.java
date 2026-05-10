package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chat_room_id") private ChatRoomEntity chatRoom;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id")    private UserEntity sender;
    @Column(columnDefinition = "TEXT", nullable = false) private String content;
    @Column(name = "is_read") @Builder.Default private boolean isRead = false;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @PrePersist void onCreate() { sentAt = LocalDateTime.now(); }
}
