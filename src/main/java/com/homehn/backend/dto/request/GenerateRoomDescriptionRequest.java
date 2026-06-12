package com.homehn.backend.dto.request;

import com.homehn.backend.entity.RoomEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRoomDescriptionRequest {
    private String title;
    private String address;
    private String ward;
    private String city;
    private BigDecimal price;
    private BigDecimal area;
    private BigDecimal electricPrice;
    private BigDecimal waterPrice;
    private BigDecimal otherFees;
    private RoomEntity.RoomType roomType;
    private Boolean isFurnished;
    private Integer maxPeople;
    private RoomEntity.GenderRequirement genderRequirement;
    private List<String> amenities;
    private String currentDescription;
}
