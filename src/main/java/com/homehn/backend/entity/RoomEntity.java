package com.homehn.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private UserEntity landlord;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal area;

    @Column(name = "electric_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal electricPrice = BigDecimal.ZERO;

    @Column(name = "water_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal waterPrice = BigDecimal.ZERO;

    @Column(name = "other_fees", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal otherFees = BigDecimal.ZERO;

    @Column(nullable = false)
    private String address;

    private String ward;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String city;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    @Builder.Default
    private RoomType roomType = RoomType.PHONG_TRO;

    @JsonProperty("isFurnished")
    @Column(name = "is_furnished")
    @Builder.Default
    private boolean isFurnished = false;

    @Column(name = "max_people")
    @Builder.Default
    private int maxPeople = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_requirement")
    @Builder.Default
    private GenderRequirement genderRequirement = GenderRequirement.ALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.PENDING;

    @Column(name = "view_count")
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<RoomImageEntity> images = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomAmenityEntity> amenities = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Column(name = "avg_rating", columnDefinition = "DECIMAL(3,2)")
    @Builder.Default
    private double avgRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private int reviewCount = 0;

    public enum RoomType     { PHONG_TRO, CHUNG_CU_MINI, STUDIO, NGAN_PHONG, NHA_NGUYEN_CAN }
    public enum GenderRequirement { ALL, MALE, FEMALE }
    public enum RoomStatus   { ACTIVE, PENDING, REJECTED, HIDDEN, EXPIRED, RENTED, AVAILABLE_SOON, HIDDEN_REVIEW }
}
