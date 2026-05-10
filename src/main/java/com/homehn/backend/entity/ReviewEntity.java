package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity @Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id","reviewer_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id") private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id") private UserEntity reviewer;

    @Column(nullable = false)
    private int rating; // Tổng 1-5

    // 4 tiêu chí phụ (nullable — không bắt buộc)
    @Column(name = "rating_location") private Integer ratingLocation;
    @Column(name = "rating_price")    private Integer ratingPrice;
    @Column(name = "rating_landlord") private Integer ratingLandlord;
    @Column(name = "rating_hygiene")  private Integer ratingHygiene;

    @Column(length = 200)
    private String comment;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    private List<ReviewMediaEntity> mediaFiles = new ArrayList<>();

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
