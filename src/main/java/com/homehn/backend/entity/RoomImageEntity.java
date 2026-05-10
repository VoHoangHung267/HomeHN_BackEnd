package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "room_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomImageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id") private RoomEntity room;
    @Column(name = "image_url", columnDefinition = "TEXT") private String imageUrl;
    @Column(name = "public_id") private String publicId;
    @Column(name = "is_primary") @Builder.Default private boolean isPrimary = false;
    @Column(name = "sort_order") @Builder.Default private int sortOrder = 0;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
