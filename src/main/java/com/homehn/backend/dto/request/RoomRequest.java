package com.homehn.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.homehn.backend.entity.RoomEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0.01", message = "Giá thuê phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá thuê không hợp lệ, tối đa 10 chữ số phần nguyên và 2 chữ số thập phân")
    @DecimalMax(value = "9999999999.99", message = "Giá thuê không được vượt quá 9,999,999,999.99")
    private BigDecimal price;

    @NotNull(message = "Diện tích không được để trống")
    @DecimalMin(value = "5", message = "Diện tích tối thiểu 5m²")
    @Digits(integer = 6, fraction = 2, message = "Diện tích không hợp lệ, tối đa 6 chữ số phần nguyên và 2 chữ số thập phân")
    @DecimalMax(value = "999999.99", message = "Diện tích không được vượt quá 999,999.99m²")
    private BigDecimal area;

    @DecimalMin(value = "0", message = "Giá điện không được nhỏ hơn 0")
    @Digits(integer = 8, fraction = 2, message = "Giá điện không hợp lệ, tối đa 8 chữ số phần nguyên và 2 chữ số thập phân")
    @DecimalMax(value = "99999999.99", message = "Giá điện không được vượt quá 99,999,999.99")
    private BigDecimal electricPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "Giá nước không được nhỏ hơn 0")
    @Digits(integer = 8, fraction = 2, message = "Giá nước không hợp lệ, tối đa 8 chữ số phần nguyên và 2 chữ số thập phân")
    @DecimalMax(value = "99999999.99", message = "Giá nước không được vượt quá 99,999,999.99")
    private BigDecimal waterPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "Phí khác không được nhỏ hơn 0")
    @Digits(integer = 8, fraction = 2, message = "Phí khác không hợp lệ, tối đa 8 chữ số phần nguyên và 2 chữ số thập phân")
    @DecimalMax(value = "99999999.99", message = "Phí khác không được vượt quá 99,999,999.99")
    private BigDecimal otherFees = BigDecimal.ZERO;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 255, message = "Phường/xã không được vượt quá 255 ký tự")
    private String ward;

    @NotBlank(message = "Quận/huyện không được để trống")
    @Size(max = 255, message = "Quận/huyện không được vượt quá 255 ký tự")
    private String district;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 255, message = "Thành phố không được vượt quá 255 ký tự")
    private String city;

    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    private Double longitude;

    private RoomEntity.RoomType roomType = RoomEntity.RoomType.PHONG_TRO;

    @JsonProperty("isFurnished")
    private boolean isFurnished = false;

    @Min(value = 1, message = "Tối thiểu 1 người")
    @Max(value = 10, message = "Tối đa 10 người")
    private int maxPeople = 2;

    private RoomEntity.GenderRequirement genderRequirement = RoomEntity.GenderRequirement.ALL;

    @Size(max = 20, message = "Tiện ích không được vượt quá 20 mục")
    private List<@NotBlank(message = "Tên tiện ích không được để trống")
            @Size(max = 100, message = "Tên tiện ích không được vượt quá 100 ký tự") String> amenities = new ArrayList<>();

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
