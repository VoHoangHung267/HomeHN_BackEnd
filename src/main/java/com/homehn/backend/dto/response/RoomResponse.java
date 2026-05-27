package com.homehn.backend.dto.response;

import com.homehn.backend.entity.RoomAmenityEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.RoomImageEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RoomResponse {
    private Long id;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private String landlordAvatar;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal area;
    private BigDecimal electricPrice;
    private BigDecimal waterPrice;
    private BigDecimal otherFees;
    private String address;
    private String ward;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private RoomEntity.RoomType roomType;
    private Boolean isFurnished;
    private Integer maxPeople;
    private RoomEntity.GenderRequirement genderRequirement;
    private RoomEntity.RoomStatus status;
    private int viewCount;
    private List<String> imageUrls;
    private String primaryImageUrl;
    private List<String> amenities;
    private LocalDateTime createdAt;
    private Boolean favorited;
    private double avgRating;
    private int    reviewCount;
    private LocalDate availableFrom;

    public static RoomResponse from(RoomEntity r) {
        return RoomResponse.builder()
                .id(r.getId())
                .landlordId(r.getLandlord().getId())
                .landlordName(r.getLandlord().getFullName())
                .landlordPhone(r.getLandlord().getPhone())
                .landlordAvatar(r.getLandlord().getAvatarUrl())
                .title(r.getTitle())
                .description(r.getDescription())
                .price(r.getPrice())
                .area(r.getArea())
                .electricPrice(r.getElectricPrice())
                .waterPrice(r.getWaterPrice())
                .otherFees(r.getOtherFees())
                .address(r.getAddress())
                .ward(r.getWard())
                .district(r.getDistrict())
                .city(r.getCity())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .roomType(r.getRoomType())
                .isFurnished(r.isFurnished())
                .maxPeople(r.getMaxPeople())
                .genderRequirement(r.getGenderRequirement())
                .status(r.getStatus())
                .viewCount(r.getViewCount())
                .amenities(r.getAmenities().stream()
                        .map(RoomAmenityEntity::getAmenityName).toList())
                .imageUrls(r.getImages().stream()
                        .map(RoomImageEntity::getImageUrl).toList())
                .primaryImageUrl(r.getImages().stream()
                        .filter(RoomImageEntity::isPrimary)
                        .map(RoomImageEntity::getImageUrl)
                        .findFirst().orElse(null))
                .createdAt(r.getCreatedAt())
                .avgRating(r.getAvgRating())
                .reviewCount(r.getReviewCount())
                .build();
    }
}
