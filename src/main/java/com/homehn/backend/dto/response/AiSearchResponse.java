package com.homehn.backend.dto.response;

import com.homehn.backend.entity.RoomEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchResponse {
    private String keyword;
    private String district;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minArea;
    private BigDecimal maxArea;
    private RoomEntity.RoomType roomType;
    private Boolean isFurnished;
    private RoomEntity.GenderRequirement genderRequirement;
    private String sortBy;
    private String note;
}
