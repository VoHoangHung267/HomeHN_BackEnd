package com.homehn.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.homehn.backend.entity.RoomEntity;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0", message = "Giá thuê không hợp lệ")
    private BigDecimal price;

    @NotNull(message = "Diện tích không được để trống")
    @DecimalMin(value = "5", message = "Diện tích tối thiểu 5m²")
    private BigDecimal area;

    private BigDecimal electricPrice = BigDecimal.ZERO;
    private BigDecimal waterPrice = BigDecimal.ZERO;
    private BigDecimal otherFees = BigDecimal.ZERO;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String ward;

    @NotBlank(message = "Quận/huyện không được để trống")
    private String district;

    @NotBlank(message = "Thành phố không được để trống")
    private String city;

    private Double latitude;
    private Double longitude;

    private RoomEntity.RoomType roomType = RoomEntity.RoomType.PHONG_TRO;

    @JsonProperty("isFurnished")
    private boolean isFurnished = false;

    @Min(value = 1, message = "Tối thiểu 1 người")
    @Max(value = 10, message = "Tối đa 10 người")
    private int maxPeople = 2;

    private RoomEntity.GenderRequirement genderRequirement = RoomEntity.GenderRequirement.ALL;

    private List<String> amenities = new ArrayList<>();

    // ── Getters với null-safe defaults ────────────────────
    public BigDecimal getElectricPrice() {
        return electricPrice == null ? BigDecimal.ZERO : electricPrice;
    }

    public BigDecimal getWaterPrice() {
        return waterPrice == null ? BigDecimal.ZERO : waterPrice;
    }

    public BigDecimal getOtherFees() {
        return otherFees == null ? BigDecimal.ZERO : otherFees;
    }

    public RoomEntity.RoomType getRoomType() {
        return roomType == null ? RoomEntity.RoomType.PHONG_TRO : roomType;
    }

    public RoomEntity.GenderRequirement getGenderRequirement() {
        return genderRequirement == null ? RoomEntity.GenderRequirement.ALL : genderRequirement;
    }

    public List<String> getAmenities() {
        return amenities == null ? new ArrayList<>() : amenities;
    }
}